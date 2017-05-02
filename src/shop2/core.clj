(ns shop2.core
  	(:require 	[compojure.core           :refer [defroutes]]
  				[compojure.route          :as route]
            	[compojure.handler        :as handler]
            	[shop2.db                 :as db]
            	[shop2.controllers.routes :as sr]
            	[shop2.views.layout       :as layout]
            	(taoensso 	[timbre   :as log])
            	[taoensso.timbre.appenders.core :as appenders]
            	[ring.middleware.defaults :as rmd]
            	[ring.middleware.reload   :as rmr]
            	[ring.middleware.stacktrace :as rmst]
            	[ring.adapter.jetty       :as ring])
  	(:gen-class))

;;-----------------------------------------------------------------------------

(defroutes routes
	sr/routes
  	(route/resources "/")
  	(route/not-found (layout/four-oh-four)))

(def application
	(-> routes
		(rmr/wrap-reload)
		(rmst/wrap-stacktrace)
		(rmd/wrap-defaults (assoc-in rmd/site-defaults [:security :anti-forgery] true))))

(defn start
	[port]
  	(log/set-level! :trace)
  	(log/merge-config!
  		{:appenders {:spit (appenders/spit-appender {:fname "shop.log"})}})
	(ring/run-jetty application {:port port
                                 :join? false}))

(defn -main []
  	(start 3000))
