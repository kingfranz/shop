(ns shop2.views.admin.users
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
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]))

;;-----------------------------------------------------------------------------

(defn edit-user
    [request userid]
    (try
        (let [user (get-user-by-id userid)]
            (when (nil? user)
                (throw (Exception. "Unknown username")))
            (common request "Edit user" [] ; css-admin
                           (hf/form-to
                            [:post "/admin/edited-user"]
                            (ruaf/anti-forgery-field)
                            (hf/hidden-field :userid (:_id user))
                            [:table
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
                              [:td.new-cb
                               (hf/check-box {:class "new-cb"} :admin (contains? (:roles user) :admin))]]
                             [:tr [:td.vspace ""]]
                             [:tr
                              [:td
                               {:colspan 3}
                               (hf/submit-button {:class "login-txt"} "Uppdatera användare")]]])))
        (catch Throwable e
            (error-page request "/admin/edit-user" "" e))))

(defn edit-user!
    [{params :params :as request}]
    (try
        (when (not= (:password params) (:password2 params))
            (throw (ex-info "PW 1 and 2 doesn't match" {:cause :password})))
        (set-user-password (:userid params)
                              (:password params))
        (set-user-roles (:userid params)
                           (if (:admin params) #{:user :admin} #{:user}))
        (ring/redirect "/admin/")
        (catch Throwable e
            (error-page request "/admin/edit-user!" "fel" e))))

;;-----------------------------------------------------------------------------

(defn new-user
    [request]
    (try
        (common request "New user" [] ; css-admin
                       (hf/form-to
                        [:post "/admin/new-user"]
                        (ruaf/anti-forgery-field)
                        [:table
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
                          [:td {:colspan 2} (hf/submit-button {:class "login-txt"} "Skapa användare")]]]))
        (catch Throwable e
            (error-page request "/admin/new-user!" "fel" e))))

(defn new-user!
    [{params :params :as request}]
    (try
        (when (str/blank? (:username params))
            (throw (ex-info "username is blank" {:cause :username})))
        (when (not= (:password params) (:password2 params))
            (throw (ex-info "PW 1 and 2 doesn't match" {:cause :password})))
        (create-user (:username params)
                        (:password params)
                        (if (:admin params) #{:user :admin} #{:user}))
        (ring/redirect "/admin/")
        (catch Throwable e
            (error-page request "/admin/new-user!" "fel" e))))

(defn delete-user!
    [request userid]
    (try
        (delete-user userid)
        (ring/redirect "/admin/")
        (catch Throwable e
            (error-page request "/admin/delete-user!" "fel" e))))

