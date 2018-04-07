(ns shop2.views.admin
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.db.menus :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.recipes :refer :all]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [hiccup.form :as hf]
              [hiccup.page :as hp]
              [ring.util.anti-forgery :as ruaf]
              [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
              [environ.core :refer [env]]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn-spec admin-block any?
    [header string?, new-url string?, bulk (s/nilable string?), edit-url string?, data :shop/dd]
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

(def ^:private script (str "function post(params) {\n"
                           " var form = document.createElement(\"form\");\n"
                           "    form.setAttribute(\"method\", 'post');\n"
                           "    form.setAttribute(\"action\", '/login');\n"
                           "    for(var key in params) {\n"
                           "        if(params.hasOwnProperty(key)) {\n"
                           "            var hiddenField = document.createElement(\"input\");\n"
                           "            hiddenField.setAttribute(\"type\", \"hidden\");\n"
                           "            hiddenField.setAttribute(\"name\", key);\n"
                           "            hiddenField.setAttribute(\"value\", params[key]);\n"
                           "            form.appendChild(hiddenField);}}\n"
                           "    document.body.appendChild(form);\n"
                           "    form.submit();}\n\nfunction loadKey(aft) {\n"
                           "    var un = localStorage.getItem(\"shopuser\");\n"
                           "    var pk = localStorage.getItem(\"shoppass\");\n"
                           "    if(un && pk) { post({username: un, password: pk, '__anti-forgery-token': aft}); }}"))

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

(defn copy-password
    [request]
    (let [user-cmd (format "localStorage.setItem(\"shopuser\", \"%s\");" (:username (udata request)))
          pass-cmd (format "localStorage.setItem(\"shoppass\", \"%s\");" (:password (udata request)))]
        (hp/html5
            [:head
             [:title "Copy password"]]
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
        [:div.item-div
         [:a.button-s {:href "/admin/copy"} "Copy PW"]]
        [:p]
        [:label (str "Shopping " (env :app-version))]
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
                          (map (fn [item] [(str (:entryname item) " - " (some-> item :tag :entryname)) (:_id item)]))))
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
         [:script script]
         ;(hp/include-js "login.js")
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

(st/instrument)
