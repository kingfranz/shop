(ns shop2.views.admin.tags
    (:require [shop2.db :refer :all]
              [shop2.extra :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.home :refer :all]
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
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]))

;;-----------------------------------------------------------------------------

(defn new-tag
    [request]
    (common request "Skapa ny tag" []                 ; css-lists-new
            (hf/form-to
                [:post "/admin/new-tag"]
                (ruaf/anti-forgery-field)
                [:div
                 (home-button)
                 (hf/submit-button {:class "button"} "Skapa")]
                (named-div "Namn:"
                           (hf/text-field {:class "tags-head"} :entryname)))))

(defn new-tag!
    [{params :params :as request}]
    )

(defn edit-tag
    [request tag-id]
    (let [tag (get-tag tag-id)]
        (common request "Edit tag" [css-tags css-tags-tbl]
                (hf/form-to
                    [:post "/update-tag"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :_id (:_id tag))
                    [:table
                     [:tr
                      [:td {:colspan 2}
                       [:div
                        (homeback-button "/admin")
                        [:a.link-flex {:href (str "/admin/delete-tag/" tag-id)} "Ta bort"]
                        [:a.link-flex {:href (str "/admin/delete-tag-all/" tag-id)} "Bort ;verallt"]
                        [:a.link-flex (hf/submit-button {:class "button"} "Uppdatera")]]]]
                     [:tr
                      [:td {:style "padding: 40px 25px; width: 50px"}
                       [:label "Namn"]]
                      [:td
                       (hf/text-field {:class "new-item-txt"}
                                      :entryname (:entryname tag))]]]))))

(defn edit-tag!
    [{params :params}]
    (when (and (seq (:_id params)) (seq (:entryname params)))
        (update-tag (:_id params) (:entryname params)))
    (ring/redirect (str "/admin/tag/" (:_id params))))

(defn delete-tag!
    [request tag-id]
    (delete-tag tag-id)
    (ring/redirect "/user/home"))

(defn delete-tag-all!
    [request tag-id]
    (delete-tag-all tag-id)
    (ring/redirect "/user/home"))
