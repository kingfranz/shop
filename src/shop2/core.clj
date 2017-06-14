(ns shop2.core
  	(:require 	(compojure 					[core       	:refer [defroutes]]
  											[route      	:as route]
            								[handler    	:as handler])
            	(shop2 						[db        		:as db])
            	(shop2.controllers 			[routes    		:as sr])
            	(shop2.views 				[layout    		:as layout])
            	(taoensso 					[timbre     	:as log])
            	(taoensso.timbre.appenders 	[core 			:as appenders])
            	(cemerick 					[friend     	:as friend])
            	(cemerick.friend 			[workflows 		:as workflows]
                             				[credentials 	:as creds])
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
		(assoc-in [:session :cookie-attrs :max-age] 36000)
		(assoc-in [:session :cookie-name] "secure-shop-session")
		))

(defn dirty-fix
	[x]
	(log/set-level! :trace)
    (log/merge-config! {:appenders {:println {:enabled? false}}})
    (log/merge-config! {:timestamp-opts {:pattern "MM-dd HH:mm:ss"
    					   				 :locale (java.util.Locale. "sv_SE")
    					   				 :timezone (java.util.TimeZone/getTimeZone "Europe/Stockholm")}
    					:output-fn (partial log/default-output-fn {:stacktrace-fonts {}})})
  	(log/merge-config!
  		{:appenders {:spit (appenders/spit-appender {:fname "shop.log"})}})
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

(defn aaaa
	[xx]
	(let [a (creds/bcrypt-credential-fn get-user xx)]
		(println "aaaa:" xx)
		(println "a:" a)
		a))

(def application
	(-> routes
		dirty-fix
		;logger/wrap-with-logger
		(friend/authenticate {
			:unauthorized-handler #(response/status (response/response "NO") 401)
			:credential-fn (partial creds/bcrypt-credential-fn get-user)
			;:credential-fn aaaa
            :workflows [(workflows/interactive-form)]})
		;(rmr/wrap-reload)
		;(rmst/wrap-stacktrace)
		;wrap-fallback-exception
		(rmd/wrap-defaults ring-default)))

(defn start
	[port]
  	(ring/run-jetty application {:port port
                                 :join? false}))

(defn -main
	[]
	;(println "fix recipes")
	;(println (db/fix-recipes))
	;(println "fix lists")
	;(println (db/fix-lists)))
  	(start 3000))
