(ns shop2.views.admin
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
            					[arithmetic     :as ga])
            	(hiccup 		[core           :as h]
            					[def            :as hd]
            					[element        :as he]
            					[form           :as hf]
            					[page           :as hp]
            					[util           :as hu])
            	(ring.util 		[anti-forgery 	:as ruaf]
            					[response     	:as ring])
            	(environ 		[core   		:refer [env]])
          		(clojure 		[string         :as str]
            					[set           	:as set])))

;;-----------------------------------------------------------------------------

(defn list-list
	[]
	[:table
		(for [a-list (sort-by :entryname (dblists/get-lists-with-count))]
			[:tr
				[:td
					[:a.link-thick {:href (str "/admin/edit-list/" (:_id a-list))}
						(:entryname a-list)]]])])

(defn user-list
	[]
	[:table
		(for [user (sort-by :username (db/get-users))]
			[:tr
				[:td
					[:a.link-thick {:href (str "/admin/edit-user/" (:_id user))}
						(:username user)]]])])

(defn item-list
	[]
	[:table
		(for [item (sort-by #(str/lower-case (:entryname %)) (dbitems/get-item-names))]
			[:tr
				[:td
					[:a.link-thick {:href (str "/admin/edit-item/" (:_id item))}
						(:entryname item)]]])])

(defn tags-list
	[]
	[:table
		(for [tag (sort-by #(str/lower-case (:entryname %)) (dbtags/get-tag-names))]
			[:tr
				[:td
					[:a.link-thick {:href (str "/admin/edit-tag/" (:_id tag))}
						(:entryname tag)]]])])

(defn fix-names
  	[]
    (doseq [tag (dbtags/get-tags)
            :when (not (contains? tag :entrynamelc))]
      	(dbtags/update-tag (:_id tag) (:entryname tag)))
    (doseq [recipe (dbrecipes/get-recipes)
            :when (not (contains? recipe :entrynamelc))]
      	(dbrecipes/update-recipe recipe))
    (doseq [item (dbitems/get-items)
            :when (or (not (contains? item :entrynamelc))
                      (some #(not (contains? % :entrynamelc)) (:tags item)))]
      	(dbitems/update-item
         	(update item :tags #(map (fn [t] (assoc t :entrynamelc (db/mk-enlc (:entryname t)))) %))))
    )

(defn admin-page
	[request]
 	;(fix-names)
	(layout/common request "Admin" [css-admin]
        [:p (env :database-ip)]
	  	[:div.column
			[:p.header [:a.link-home {:href "/admin/new-list"} "Ny lista"]]
			[:div.home-box (list-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/admin/new-user"} "Users"]]
			[:div.home-box (user-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/mk-new-item"} "Items"]]
			[:div.home-box (item-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/new-tag"} "Tags"]]
			[:div.home-box (tags-list)]]
	  	))

;;-----------------------------------------------------------------------------

(defn auth-page
	[]
	;(db/create-user "soren" "Benq.fp731" #{:user :admin})
	(hp/html5
	  	[:head {:lang "sv"}
			[:meta {:charset "utf-8"}]
			[:meta {:http-equiv "X-UA-Compatible"
					:content "IE=edge,chrome=1"}]
			[:meta {:name "viewport"
					:content "width=device-width, initial-scale=1, maximum-scale=1"}]
			[:title "Shopping"]
			[:style css-auth]]
		(hf/form-to
	    	[:post "login"]
	        (ruaf/anti-forgery-field)
        	[:table
	        	[:tr
	        		[:td {:colspan 2} (hf/label :x (str "Shopping " (env :project/version)))]
	        	[:tr
	        		[:td "Username:"]
	        		[:td (hf/text-field {:class "login-txt"} :username)]]
	        	[:tr
	        		[:td "Password:"]
	        		[:td (hf/password-field {:class "login-txt"} :password)]]
	        	[:tr
	        		[:td {:colspan 2} (hf/submit-button {:class "login-txt"} "Logga in")]]]])))

;;-----------------------------------------------------------------------------
