(ns shop2.core
  	(:require 	(compojure 					[core       	:refer [defroutes]]
  											[route      	:as route]
            								[handler    	:as handler])
            	(shop2 						[db        		:as db]
                    						[logfile        :refer :all])
            	(shop2.controllers 			[routes    		:as sr])
            	(shop2.views 				[layout    		:as layout])
            	(taoensso 					[timbre     	:as log])
            	(taoensso.timbre.appenders 	[core 			:as appenders])
            	(cemerick 					[friend     	:as friend])
            	(cemerick.friend 			[workflows 		:as workflows]
                             				[credentials 	:as creds])
            	(environ 					[core 			:refer [env]])
            	(clj-time 	 				[core        	:as t]
            				 				[local       	:as l]
            				 				[coerce      	:as c]
            				 				[format      	:as f]
            				 				[periodic    	:as p])
            	(ring 						[logger			:as logger])
            	(ring.middleware 			[defaults   	:as rmd]
            								[reload     	:as rmr]
            								[stacktrace 	:as rmst])
            	(ring.middleware.session 	[store  		:as store]
            								[cookie 		:as cookie]
            								[memory			:as mem])
            	(ring.util 					[response   	:as response])
            	(ring.adapter 				[jetty			:as ring]))
  	(:gen-class))

;;-----------------------------------------------------------------------------

;(derive ::admin ::user)

;(def users {"root" {:username "root"
;                    :password (creds/hash-bcrypt "admin_password")
;                    :roles #{::admin}}
;            "jane" {:username "jane"
;                    :password (creds/hash-bcrypt "user_password")
;                    :roles #{::user}}})

(defn get-user
	[username]
	(let [u (db/get-user username)]
		(log/trace "get-user:" u)
		u))


(defroutes routes
	sr/routes
  	(route/resources "/")
  	(route/not-found (layout/four-oh-four)))

(def ring-default
	(-> rmd/site-defaults
		(assoc-in [:session :store] (cookie/cookie-store {:key (subs (db/mk-id) 0 16)}))
		(assoc-in [:session :cookie-attrs :expires] (t/plus (l/local-now) (t/years 10)))
		(assoc-in [:session :cookie-name] "secure-shop-session")
		))

(defn dirty-fix
	[x]
	(setup-log)
	x)

(defn wrap-fallback-exception
	[handler]
	(fn [request]
		(try
			(handler request)
			(catch Exception e
				(log/fatal e)
				{:status 500
				 :body (str "Something isn't quite right..." (.getMessage e) (ex-data e))}))))

(defn unauth-handler
	[request]
	(Thread/sleep (* 3 1000))
	(response/status (response/response "NO") 401))

(def application
	(-> routes
		dirty-fix
		;logger/wrap-with-logger
		(friend/authenticate {
			:unauthorized-handler unauth-handler
			:credential-fn        (partial creds/bcrypt-credential-fn get-user)
            :workflows            [(workflows/interactive-form)]})
		(rmd/wrap-defaults ring-default)
		))

