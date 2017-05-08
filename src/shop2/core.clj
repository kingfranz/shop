(ns shop2.core
  	(:require 	(compojure 					[core       :refer [defroutes]]
  											[route      :as route]
            								[handler    :as handler])
            	[shop2.db                       		:as db]
            	[shop2.controllers.routes       		:as sr]
            	[shop2.views.layout             		:as layout]
            	(taoensso 					[timbre     :as log])
            	(taoensso.timbre.appenders 	[core 		:as appenders])
            	(ring.middleware 			[defaults   :as rmd]
            								[reload     :as rmr]
            								[stacktrace :as rmst])
            	(ring.middleware.session 	[store  	:as store]
            								[cookie 	:as cookie]
            								[memory		:as mem])
            	[ring.adapter.jetty       				:as ring])
  	(:gen-class))

;;-----------------------------------------------------------------------------

(defroutes routes
	sr/routes
  	(route/resources "/")
  	(route/not-found (layout/four-oh-four)))

(deftype DBSessionStore []
	store/SessionStore
	(store/read-session [_ key]
		(db/read-session-data key))
	(store/write-session [_ key data]
		(let [key (or key (db/mk-id))]
			(db/save-session-data key data)
			key))
	(store/delete-session [_ key]
		(db/delete-session-data key)
		nil))

(defn db-store
  "Creates an DB session storage engine. Accepts an atom as an optional
  argument; if supplied, the atom is used to hold the session data."
  []
  (DBSessionStore.))

(def ring-default
	(-> rmd/site-defaults
		(assoc-in [:session :store] (mem/memory-store))
		(assoc-in [:session :cookie-attrs :max-age] 3600)
		(assoc-in [:session :cookie-name] "secure-shop-session")
		))

(def application
	(-> routes
		(rmr/wrap-reload)
		(rmst/wrap-stacktrace)
		(rmd/wrap-defaults rmd/site-defaults)))

(defn start
	[port]
  	(log/set-level! :trace)
  	(log/merge-config!
  		{:appenders {:spit (appenders/spit-appender {:fname "shop.log"})}})
	(ring/run-jetty application {:port port
                                 :join? false}))

(defn -main []
  	(start 3000))
