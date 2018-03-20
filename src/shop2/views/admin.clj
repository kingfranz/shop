(ns shop2.views.admin
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.db.menus :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.recipes :refer :all]
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
              [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.string :as str]
              [clojure.set :as set]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn- admin-block
    [header new-url bulk edit-url data]
    (named-block header
                 (list
                     [:div.item-div
                      [:a.button-s {:href new-url} "Ny"]]
                     (when bulk
                         [:div.item-div
                          [:a.button-s {:href bulk} "Bulk"]])
                     [:div.item-div
                      (hf/form-to
                          [:post edit-url]
                          (ruaf/anti-forgery-field)
                          [:div.item-div
                           (hf/submit-button {:class "button-s"} "Ändra")]
                          [:div.item-div
                           (hf/drop-down {:class "ddown-col"} :target data)])])))

(defn- mk-pw
    [request]
    (let [uuid (mk-id)
          username (:username (udata request))
          user-cmd (format "localStorage.setItem(\"shopuser\", \"%s\");" username)
          pass-cmd (format "localStorage.setItem(\"shoppass\", \"%s\");" uuid)]
        (set-user-password (:_id (udata request)) uuid)
        (spit "user.txt" (str (utils/now-str) "\nusername: " username "\npassword: " uuid "\n\n") :append true)
        (str user-cmd pass-cmd)))

(defn renew-password
    [request]
    (let [uuid (mk-id)
          username (:username (udata request))
          user-cmd (format "localStorage.setItem(\"shopuser\", \"%s\");" username)
          pass-cmd (format "localStorage.setItem(\"shoppass\", \"%s\");" uuid)]
        (set-user-password (:_id (udata request)) uuid)
        (spit "user.txt" (str (utils/now-str) "\nusername: " username "\npassword: " uuid "\n\n") :append true)
        (hp/html5
            [:head
             [:title "Renew password"]]
            [:body
             [:script (str user-cmd pass-cmd)]
             [:h1 "Password for"]
             [:h2 {:id :shopu}]
             [:h2 {:id :shopp}]
             [:script "document.getElementById(\"shopu\").innerHTML = \"User: \" + localStorage.getItem(\"shopuser\");document.getElementById(\"shopp\").innerHTML = \"Is set to: \" + localStorage.getItem(\"shoppass\");"]
             [:p]
             [:a {:href "/admin/"} "Back"]
             ])))

(defn admin-page
    [request]
    (common
        request "Admin" [css-admin css-items css-tags-tbl css-misc]
        (home-button)
        [:div.item-div
         [:a.button-s {:href "/admin/renew"} "Renew PW"]]
        (admin-block "Listor"
                     "/admin/list/new"
                     nil
                     "/admin/list/edit"
                     (map (fn [{ename :entryname id :_id}] [ename id]) (sort-by :entryname (get-list-names))))
        (admin-block "Användare"
                     "/admin/user/new"
                     nil
                     "/admin/user/edit"
                     (map (fn [{uname :username id :_id}] [uname id]) (sort-by :entryname (get-users))))
        (admin-block "Items"
                     "/admin/item/new"
                     "/admin/item/bulk-edit"
                     "/admin/item/edit"
                     (->> (get-items)
                          (sort-by :entrynamelc)
                          (map (fn [item] [(str (:entryname item) " - " (frmt-tags (:tags item))) (:_id item)]))))
        (admin-block "Tags"
                     "/admin/tag/new"
                     "/admin/tag/bulk-edit"
                     "/admin/tag/edit"
                     (map (fn [{ename :entryname id :_id}] [ename id]) (sort-by :entrynamelc (get-tags))))))

;;-----------------------------------------------------------------------------

(defn auth-page
    []
    ;(create-user "soren" "password" #{:user :admin})
    (hp/html5
        [:head {:lang "sv"}
         [:meta {:charset "utf-8"}]
         [:meta {:http-equiv "X-UA-Compatible"
                 :content    "IE=edge,chrome=1"}]
         [:meta {:name    "viewport"
                 :content "width=device-width, initial-scale=1, maximum-scale=1"}]
         [:title "Shopping"]
         (hp/include-js "login.js")
         [:style css-auth]]
        [:body {:onload (str "loadKey(\"" *anti-forgery-token* \" ");")}
         (hf/form-to
             [:post "login"]
             (ruaf/anti-forgery-field)
             [:table
              [:tr
               [:td {:colspan 2} [:label (str "Shopping " (env :app-version))]]
               [:tr
                [:td "Username:"]
                [:td (hf/text-field {:class "login-txt"} :username)]]
               [:tr
                [:td "Password:"]
                [:td (hf/password-field {:class "login-txt"} :password)]]
               [:tr
                [:td {:colspan 2} (hf/submit-button {:class "login-txt"} "Logga in")]]]])]))

;;-----------------------------------------------------------------------------
