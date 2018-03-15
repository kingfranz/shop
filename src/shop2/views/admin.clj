(ns shop2.views.admin
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common       	:refer :all]
              [shop2.views.css          	:refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items			:refer :all]
              [shop2.db.lists 			:refer :all]
              [shop2.db.menus 			:refer :all]
              [shop2.db.projects 		:refer :all]
              [shop2.db.recipes 		:refer :all]
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
    (named-block header
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
                            (hf/drop-down {:class "ddown-col"} :target data)])])))

(defn admin-page
    [request]
    (common
        request "Admin" [css-admin css-items css-tags-tbl css-misc]
        (home-button)
        (admin-block "Listor"
                     "/admin/new-list"
                     "/admin/edit-list"
                     (map (fn [{ename :entryname id :_id}] [ename id]) (sort-by :entryname (get-list-names))))
        (admin-block "Användare"
                     "/admin/new-user"
                     "/admin/edit-user"
                     (map (fn [{uname :username id :_id}] [uname id]) (sort-by :entryname (get-users))))
        (admin-block "Items"
                     "/admin/new-item"
                     "/admin/edit-item"
                     (map (fn [{ename :entryname id :_id}] [ename id]) (sort-by :entrynamelc (get-items))))
        (admin-block "Tags"
                     "/admin/new-tag"
                     "/admin/edit-tag"
                     (map (fn [{ename :entryname id :_id}] [ename id]) (sort-by :entrynamelc (get-tags))))))

;;-----------------------------------------------------------------------------

;window.onload = function() {
;                            yourFunction(param1, param2);
;                            };

(defn auth-page
	[]
	;(create-user "soren" "password" #{:user :admin})
	(hp/html5
	  	[:head {:lang "sv"}
			[:meta {:charset "utf-8"}]
			[:meta {:http-equiv "X-UA-Compatible"
					:content "IE=edge,chrome=1"}]
			[:meta {:name "viewport"
					:content "width=device-width, initial-scale=1, maximum-scale=1"}]
			[:title "Shopping"]
            (hp/include-js "login.js")
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
