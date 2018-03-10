(ns shop2.views.admin
    (:require [shop2.db :as db]
              [shop2.utils :as utils]
              [shop2.views.layout :as layout]
              [shop2.views.common :as common]
              [shop2.views.css :refer :all]
              [shop2.db.tags :as dbtags]
              [shop2.db.items :as dbitems]
              [shop2.db.lists :as dblists]
              [shop2.db.menus :as dbmenus]
              [shop2.db.projects :as dbprojects]
              [shop2.db.recipes :as dbrecipes]
              [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [garden.core :as g]
              [garden.units :as u]
              [garden.selectors :as sel]
              [garden.stylesheet :as ss]
              [garden.color :as color]
              [garden.arithmetic :as ga]
              [hiccup.core :as h]
              [hiccup.def :as hd]
              [hiccup.element :as he]
              [hiccup.form :as hf]
              [hiccup.page :as hp]
              [hiccup.util :as hu]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.string :as str]
              [clojure.set :as set]))

;;-----------------------------------------------------------------------------

(defn- admin-block
    [header new-url edit-url data]
    (common/named-block header
                        (list
                         [:div.item-div
                          [:a.button-s {:href new-url} "Ny"]]
                         [:div.item-div
                          (hf/form-to
                           [:post edit-url]
                           (ruaf/anti-forgery-field)
                           [:div.item-div
                            (hf/submit-button {:class "button-s"} "Ändra")]
                           [:div.item-div
                            (hf/drop-down {:class "ddown-col"} :list-list data)])])))

(defn admin-page
    [request]
    (layout/common
     request "Admin" [css-admin css-items css-tags-tbl css-misc]
     (common/home-button)
     (admin-block "Listor"
                  "/admin/new-list"
                  "/admin/edit-list"
                  (sort (map :entryname (dblists/get-list-names))))
     (admin-block "Användare"
                  "/admin/new-user"
                  "/admin/edit-user"
                  (sort (map :username (db/get-users))))
     (admin-block "Items"
                  "/admin/new-item"
                  "/admin/edit-item"
                  (map :entryname (sort-by :entrynamelc (dbitems/get-items))))
     (admin-block "Tags"
                  "/admin/new-tag"
                  "/admin/edit-tag"
                  (map :entryname (sort-by :entrynamelc (dbtags/get-tags))))))

;;-----------------------------------------------------------------------------

(defn auth-page
	[]
	;(db/create-user "soren" "password" #{:user :admin})
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
