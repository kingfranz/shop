(ns shop2.core
    (:require [shop2.db :refer :all]
              [shop2.db.user :refer :all]
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
              [clojure.spec.test.alpha :as s])
    (:use [org.httpkit.server :only [run-server]])
    (:gen-class))

;;-----------------------------------------------------------------------------

(defroutes all-routes
	app-routes
  	(route/resources "/")
  	(route/not-found (four-oh-four)))

;;-----------------------------------------------------------------------------

;(def ring-default
;	(-> rmd/site-defaults
;		(assoc-in [:session :store] (ShopStore.))
;		;(assoc-in [:session :cookie-attrs :expires] (t/plus (l/local-now) (t/years 10)))
;		;(assoc-in [:session :cookie-name] "secure-shop-session")
;		))

(defonce dirty-fix (logfile/setup-log "shop" 1000000 3))

(defn wrap-fallback-exception
	[handler]
	(fn [request]
		(try+
            ;(println "wrap!")
			(handler request)
            (catch [:type :conform-error] {:keys [msg spec obj]}
                (println "## Conformer Exception:" spec "\n" obj)
                (log/fatal msg)
                ;(error-page request e)
                )
            (catch Throwable e
                (println "## Exception:" (.getMessage e) e)
                (log/fatal e)
                (error-page request e)
                ;{:status 400 :body "## EXCEPTION ##"}
                ))))

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

;; entry point, lein run will pick up and start from here
(defn -main
    [& args]
    (s/instrument)
	(-> all-routes
        (wrap-anti-forgery)
        (friend/authenticate {
                              :unauthorized-handler unauth-handler
                              :credential-fn        (partial cred get-user)
                              :workflows            [(workflows/interactive-form)]})
        (ring-spy)
        (wrap-session {:store (->ShopStore )})
        (wrap-keyword-params)
        (wrap-params)
        (wrap-cookies)
        ;(wrap-with-logger)
        ;(wrap-with-body-logger)
        (wrap-fallback-exception)
        (run-server {:port 3000})
        ))

