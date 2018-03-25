(ns shop2.views.admin.users
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [garden.core :as g]
              [garden.units :as u]
              [hiccup.core :as h]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]))

;;-----------------------------------------------------------------------------

(defn edit-user
    [request userid]
    (let [user (get-user-by-id userid)]
        (when (nil? user)
            (throw+ (ex-info "Unknown user" {:type :input})))
        (common request "Edit user" [css-tags-tbl css-items]
                (hf/form-to
                    [:post "/admin/user/edited"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :userid (:_id user))
                    [:table
                     [:tr
                      [:td {:colspan 2}
                       [:div
                        (admin-home-button)
                        [:a.link-flex {:href (str "/admin/" userid)} "Ta bort"]
                        [:a.link-flex (hf/submit-button {:class "button"} "Uppdatera")]]]]
                     [:tr
                      [:th.width-200px [:label.fz24.width-100p "Name"]]
                      [:td (hf/text-field {:class "new-item-txt"} :username (:username user))]]
                     [:tr
                      [:th.width-200px [:label.fz24.width-100p "Password"]]
                      [:td (hf/password-field {:class "new-item-txt"} :password)]]
                     [:tr
                      [:th.width-200px [:label.fz24.width-100p "PW again"]]
                      [:td (hf/password-field {:class "new-item-txt"} :password2)]]
                     [:tr
                      [:th.width-200px [:label.fz24.width-100p "Admin?"]]
                      [:td.new-cb
                       (hf/check-box {:class "new-cb"} :admin (contains? (:roles user) :admin))]]]))))

(defn edit-user!
    [{params :params}]
    (when (not= (:password params) (:password2 params))
        (throw+ (ex-info "PW 1 and 2 doesn't match" {:type :input})))
    (set-user-name (:userid params) (str/trim (:username params)))
    (set-user-password (:userid params) (:password params))
    (set-user-roles (:userid params) (if (:admin params) #{:user :admin} #{:user}))
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

(defn new-user
    [request]
    (common request "New user" [css-tags-tbl css-items]
            (hf/form-to
                [:post "/admin/user/new"]
                (ruaf/anti-forgery-field)
                [:table
                 [:tr
                  [:td {:colspan 2}
                   [:div
                    (admin-home-button)
                    [:a.link-flex (hf/submit-button {:class "button"} "sKAPA")]]]]
                 [:tr
                  [:th.width-200px [:label.fz24.width-100p "Name"]]
                  [:td (hf/text-field {:class "new-item-txt"} :username)]]
                 [:tr
                  [:th.width-200px [:label.fz24.width-100p "Password"]]
                  [:td (hf/password-field {:class "new-item-txt"} :password)]]
                 [:tr
                  [:th.width-200px [:label.fz24.width-100p "PW again"]]
                  [:td (hf/password-field {:class "new-item-txt"} :password2)]]
                 [:tr
                  [:th.width-200px [:label.fz24.width-100p "Admin?"]]
                  [:td.new-cb
                   (hf/check-box {:class "new-cb"} :admin)]]])))

(defn new-user!
    [{params :params}]
    (when (str/blank? (:username params))
        (throw+ (ex-info "username is blank" {:type :input})))
    (when (not= (:password params) (:password2 params))
        (throw+ (ex-info "PW 1 and 2 doesn't match" {:type :input})))
    (create-user (str/trim (:username params))
                 (:password params)
                 (if (:admin params) #{:user :admin} #{:user}))
    (ring/redirect "/admin/"))

(defn delete-user!
    [_ userid]
    (delete-user userid)
    (ring/redirect "/admin/"))

