(ns shop2.core
    (:require [mongolib.core :as db]
              [shop2.db.user :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.items :refer :all]
              [shop2.views.css :refer :all]
              [shop2.controllers.routes :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.session-store :refer :all]
              [utils.core :as utils]
              [utils.logfile :as logfile]
              [compojure.core :refer [defroutes]]
              [compojure.route :as route]
              [taoensso.timbre :as log]
              [cemerick.friend :as friend]
              [cemerick.friend.workflows :as workflows]
              [cemerick.friend.credentials :as creds]
              [slingshot.slingshot :refer [throw+ try+]]
              [environ.core :refer [env]]
              [ring.logger.timbre :refer [wrap-with-logger wrap-with-body-logger]]
              [ring.middleware.session :refer [wrap-session]]
              [ring.middleware.keyword-params :refer [wrap-keyword-params]]
              [ring.middleware.nested-params :refer [wrap-nested-params]]
              [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
              [ring.middleware.params :refer [wrap-params]]
              [ring.middleware.cookies :refer [wrap-cookies]]
              [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
              [ring.middleware.content-type :refer [wrap-content-type]]
              [ring.util.http-response :as response]
              [clojure.pprint :as pp]
              [clojure.spec.test.alpha :as s]
              [clojure.string :as str])
    (:use [org.httpkit.server :only [run-server]])
    (:gen-class)
    (:import (clojure.lang ExceptionInfo)))

;;-----------------------------------------------------------------------------

(defroutes all-routes
	app-routes
  	(route/resources "/")
  	(route/not-found (four-oh-four)))

;;-----------------------------------------------------------------------------

(defonce dirty-fix (logfile/setup-log "shop" 1000000 3))

(defn- report-exception
    [request etype src cause context]
    (println "##" etype "Exception in" src cause (:message context))
    (log/fatal (:message context))
    (log/fatal (->> (:stack-trace context) seq (map StackTraceElement->vec) (str/join "\n")))
    (error-page request
                etype
                context
                cause
                src))

(defn- wrap-fallback-exception
	[handler]
	(fn [request]
		(try+
    		(handler request)
            (catch [:type :db] {:keys [src cause]}
                (report-exception request "DB" src cause &throw-context))
            (catch [:type :input] {:keys [src cause]}
                (report-exception request "Input" src cause &throw-context))
            (catch Exception e
                (report-exception request "Exception" nil (:cause &throw-context) &throw-context))
            (catch (some? (get % :clojure.spec.alpha/problems)) err
                (report-exception request "Spec Error" nil (:cause &throw-context) &throw-context))
            (catch Throwable e
                (report-exception request "Throwable" nil (:cause &throw-context) &throw-context))
            ;(catch Object e (println "\nCaught Object:" (type e) (str e) "\n") (throw+ e))
            )))

(defn unauth-handler
	[_]
	(Thread/sleep (* 3 1000))
    (response/bad-request "fail"))

(defn cred
    [load-credentials-fn {:keys [username password]}]
    ;(println "cred:" username password)
    (when-let [creds (load-credentials-fn username)]
        ;(println "cred2:" creds)
        (let [password-key (or (-> creds meta ::password-key) :password)]
            (when (= password (get creds password-key))
                (assoc creds password-key nil)))))

(defn ring-spy
    [handler]
    (fn [request]
        (spit "req-trace.txt"
              (str "\n\n----------------------------------\n"
                   (utils/now-str) "\n"
                   "----------------------------------\n"
                   (with-out-str (pp/pprint request)) "\n")
              :append true)
        (handler request)))

(defn custom-handler
    [f type]
    (fn [^Exception e data request]
        (f {:message (.getMessage e), :type type})))

(defn convert-db
    []
    (doseq [item (doall (get-raw-items))]
        (-> item
            (select-keys [:_id :created :entryname :entrynamelc :tag :url :price :project :parent :oneshot])
            (utils/spy)
            (update-item)))
    )

;; entry point, lein run will pick up and start from here
(defn -main
    [& args]
    (db/setup (env :database-db) (env :database-ip) (env :database-user) (env :database-pw))
	(-> all-routes
        (wrap-fallback-exception)
        (wrap-anti-forgery)
        (friend/authenticate {
                              :unauthorized-handler unauth-handler
                              :credential-fn        (partial cred get-user)
                              :workflows            [(workflows/interactive-form)]})
        ;(ring-spy)
        (wrap-session {:store (->ShopStore )})
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        ;(wrap-with-logger)
        ;(wrap-with-body-logger)
        (wrap-fallback-exception)
        (run-server {:port 3000})
        ))

