(ns shop2.views.admin.users
  	(:require 	(shop2 		 	[db           	:as db]
  							 	[utils        	:as utils])
            	(shop2.views 	[layout       	:as layout]
            				 	[common       	:as common]
            				 	[css          	:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(clj-time 		[core           :as t]
            					[local          :as l]
            					[coerce         :as c]
            					[format         :as f]
            					[periodic       :as p])
            	(garden 		[core       	:as g]
            					[units      	:as u]
            					[selectors  	:as sel]
            					[stylesheet 	:as ss]
            					[color      	:as color]
            					[arithmetic 	:as ga])
            	(hiccup 		[core       	:as h]
            					[def        	:as hd]
            					[element    	:as he]
            					[form       	:as hf]
            					[page       	:as hp]
            					[util       	:as hu])
            	(ring.util 		[anti-forgery	:as ruaf]
            					[response     	:as ring])
            	(clojure 		[string         :as str]
            					[set            :as set])))

;;-----------------------------------------------------------------------------

(defn edit-user-page
	[request userid err-msg]
	(layout/common request "Edit user" [] ; css-admin
		(hf/form-to
	    	[:post "/admin/edit-user"]
	        (ruaf/anti-forgery-field)
	        (hf/hidden-field :userid userid)
	        (if-let [user (db/get-user-by-id userid)]
	        	[:table
		        	(when (some? err-msg)
		        		[:tr
		        			[:td {:colspan 3} err-msg]])
		        	[:tr [:td.vspace ""]]
					[:tr
						[:td.r-align "Namn:"]
						[:td.spacer ""]
						[:td (hf/label {:class "uname-txt"} :x (:username user))]]
					[:tr
						[:td.r-align "Password:"]
						[:td.spacer ""]
						[:td (hf/password-field {:class "login-txt"} :password)]]
					[:tr
						[:td.r-align "PW again:"]
						[:td.spacer ""]
						[:td (hf/password-field {:class "login-txt"} :password2)]]
					[:tr
						[:td.r-align "Admin?"]
						[:td.spacer ""]
						[:td.new-cb (hf/check-box {:class "new-cb"} :admin (contains? (:roles user) :admin))]]
					[:tr [:td.vspace ""]]
					[:tr
						[:td {:colspan 3} (hf/submit-button {:class "login-txt"}  "Uppdatera användare")]]]
				(hf/label :x (str "ERROR Unknown userid: " userid))))))

(defn edit-user!
	[{params :params}]
	(try
		(when (not= (:password params) (:password2 params))
			(throw (ex-info "PW 1 and 2 doesn't match" {:cause :password})))
		(db/set-user-password (:userid params)
			  				  (:password params))
		(db/set-user-roles (:userid params)
			  			   (if (:admin params) #{:user :admin} #{:user}))
		(ring/redirect "/admin/")
		(catch Exception e
			(edit-user-page nil (:userid params) (.getMessage e)))))

;;-----------------------------------------------------------------------------

(defn new-user-page
	[request err-msg]
	(layout/common request "New user" [] ; css-admin
		(hf/form-to
	    	[:post "/admin/new-user"]
	        (ruaf/anti-forgery-field)
	        [:table
	        	(when (some? err-msg)
	        		[:tr
	        			[:td {:colspan 2} err-msg]])
	        	[:tr [:td.vspace ""]]
				[:tr
					[:td.r-align "Namn:"]
					[:td.spacer ""]
					[:td (hf/text-field {:class "login-txt"} :username)]]
				[:tr
					[:td.r-align "Password:"]
					[:td.spacer ""]
					[:td (hf/password-field {:class "login-txt"} :password)]]
				[:tr
					[:td.r-align "PW again:"]
					[:td.spacer ""]
					[:td (hf/password-field {:class "login-txt"} :password2)]]
				[:tr
					[:td.r-align "Admin?"]
					[:td.spacer ""]
					[:td (hf/check-box {:class "new-cb"} :admin)]]
				[:tr [:td.vspace ""]]
				[:tr
					[:td {:colspan 2} (hf/submit-button {:class "login-txt"} "Skapa användare")]]])))

(defn new-user!
	[{params :params}]
	(try
		(when (str/blank? (:username params))
			(throw (ex-info "username is blank" {:cause :username})))
		(when (not= (:password params) (:password2 params))
			(throw (ex-info "PW 1 and 2 doesn't match" {:cause :password})))
		(db/create-user (:username params)
			  			(:password params)
			  			(if (:admin params) #{:user :admin} #{:user}))
		(ring/redirect "/admin/")
		(catch Exception e
			(new-user-page nil (.getMessage e)))))

