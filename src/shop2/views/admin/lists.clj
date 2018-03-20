(ns shop2.views.admin.lists
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
              [slingshot.slingshot :refer [throw+ try+]]
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

(defn new-list
    [request]
    (common request "Skapa ny lista" []                 ; css-lists-new
            (hf/form-to
                [:post "/admin/new-list"]
                (ruaf/anti-forgery-field)
                [:div
                 (home-button)
                 (hf/submit-button {:class "button"} "Skapa")]
                (named-div "Listans namn:"
                           (hf/text-field {:class "tags-head"} :entryname))
                (named-div "Överornad lista:"
                           (mk-list-dd nil :parent "tags-head"))
                (named-div "Lågprioriterad lista?"
                           (hf/check-box {:class "tags-head"} :low-prio)))))

(defn- mk-parent-map
    [params]
    (when-not (or (str/blank? (:parent params))
                  (= (:parent params) top-lvl-name))
        (let [p-list (find-list-by-name (:parent params))]
            (select-keys p-list [:_id :entryname :parent]))))

(defn new-list!
    [{params :params}]
    (if (seq (:entryname params))
        (add-list {:entryname (:entryname params)
                   :parent    (mk-parent-map params)
                   :last      (some? (:low-prio params))})
        (throw+ (Exception. "list name is blank")))
    (ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

(defn edit-list
    [request list-id]
    (let [a-list (get-list list-id)]
        (common request "Ändra lista" [css-tags-tbl css-items]
                (hf/form-to
                    [:post "/admin/edited-list"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :list-id list-id)
                    [:div
                     (homeback-button "/admin")
                     [:a.link-head {:href (str "/admin/delete-list/" list-id)} "Ta bort"]
                     (hf/submit-button {:class "button"} "Uppdatera")]
                    [:table
                     [:tr
                      [:td.item-info-th
                       [:label "Listans namn:"]]
                      [:td.item-info-th
                       (hf/text-field {:class "item-info"} :entryname (:entryname a-list))]]
                     [:tr
                      [:td.item-info-th
                       [:label "Överornad lista:"]]
                      [:td.item-info-th
                       (mk-list-dd (:parent a-list) :parent "tags-head")]]
                     [:tr
                      [:td.item-info-th
                       [:label "Lågprioriterad lista?"]]
                      [:td.item-info-th
                       (hf/check-box {:class "new-cb"} :low-prio (true? (:last a-list)))]]]))))

(defn edit-list!
    [{params :params}]
    (when (str/blank? (:entryname params))
        (throw+ (Exception. "list name is blank")))
    (update-list {:_id       (:list-id params)
                  :entryname (:entryname params)
                  :parent    (mk-parent-map params)
                  :last      (some? (:low-prio params))})
    (ring/redirect "/admin"))

(defn delete-list!
    [_ list-id]
    (delete-list list-id)
    (ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

