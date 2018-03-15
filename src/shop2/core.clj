(ns shop2.core
    (:require [compojure.core :refer [defroutes]]
              [compojure.route :as route]
              [shop2.db :refer :all]
              [utils.logfile :as logfile]
              [shop2.controllers.routes :refer :all]
              [shop2.views.layout :refer :all]
              [taoensso.timbre :as log]
              [cemerick.friend :as friend]
              [cemerick.friend.workflows :as workflows]
              [cemerick.friend.credentials :as creds]
              [environ.core :refer [env]]
              [clj-time.core :as t]
              [clj-time.local :as l]
              [ring.middleware.defaults :as rmd]
              [ring.middleware.reload :as rmr]
              [ring.middleware.stacktrace :as rmst]
              [ring.middleware.session.store :as store]
              [ring.middleware.session.cookie :as cookie]
              [ring.middleware.session.memory :as mem]
              [ring.util.response :as response]
              [ring.adapter.jetty :as ring]
              [utils.core :as utils]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.pprint :as pp])
    (:gen-class))

;;-----------------------------------------------------------------------------

;(derive ::admin ::user)

;(def users {"root" {:username "root"
;                    :password (creds/hash-bcrypt "admin_password")
;                    :roles #{::admin}}
;            "jane" {:username "jane"
;                    :password (creds/hash-bcrypt "user_password")
;                    :roles #{::user}}})

(defroutes all-routes
	app-routes
  	(route/resources "/")
  	(route/not-found (four-oh-four)))

(defonce key-store (atom {}))

(defn- read-store-data
    [key]
    (println "read-store-data:" key)
    (get @key-store key))

(defn- save-store-data
    [key data]
    (println "save-store-data:" key data)
    (swap! key-store assoc key data))

(defn- delete-store-data
    [key]
    (println "delete-store-data:" key)
    (swap! key-store dissoc key))

(deftype CustomStore []
    store/SessionStore
    (store/read-session [_ key]
        (read-store-data key))
    (store/write-session [_ key data]
        (let [key (or key (mk-id))]
            (save-store-data key data)
            key))
    (store/delete-session [_ key]
        (delete-store-data key)
        nil))

(def ring-default
	(-> rmd/site-defaults
		(assoc-in [:session :store] (CustomStore.))
		;(assoc-in [:session :cookie-attrs :expires] (t/plus (l/local-now) (t/years 10)))
		;(assoc-in [:session :cookie-name] "secure-shop-session")
		))

(defonce dirty-fix (logfile/setup-log "shop" 1000000 3))

(defn wrap-fallback-exception
	[handler]
	(fn [request]
		(try+
			(handler request)
            (catch Throwable e
                (log/fatal e)
                (error-page request "" "" e)))))

(defn unauth-handler
	[request]
	(Thread/sleep (* 3 1000))
	(response/status (response/response "NO") 401))

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

(def application
	(-> all-routes
		(friend/authenticate {
                              :unauthorized-handler unauth-handler
                              :credential-fn        (partial creds/bcrypt-credential-fn get-user)
                              :workflows            [(workflows/interactive-form)]})
        wrap-fallback-exception
        ring-spy
		(rmd/wrap-defaults ring-default)
		))

