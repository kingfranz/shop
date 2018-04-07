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
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]))

;;-----------------------------------------------------------------------------

(defn new-list
    [request]
    (common request "Skapa ny lista" []                 ; css-lists-new
            (hf/form-to
                [:post "/admin/list/new"]
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

(defn-spec ^:private mk-parent-map (s/nilable (s/keys :req-un [:shop/_id :shop/entryname :shop/parent]))
    [params map?]
    (when-not (= (:parent params) no-id)
        (select-keys (get-list (:parent params)) [:_id :entryname :parent])))

(defn new-list!
    [{params :params}]
    (if (seq (:entryname params))
        (add-list (create-list-obj (:entryname params)
                                   (mk-parent-map params)
                                   (some? (:low-prio params))))
        (throw+ (Exception. "list name is blank")))
    (ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

(defn edit-list
    [request list-id]
    (let [a-list (get-list list-id)]
        (common request "Ändra lista" [css-tags-tbl css-items]
                (hf/form-to
                    [:post "/admin/list/edited"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :list-id list-id)
                    [:div
                     (homeback-button "/admin")
                     [:a.link-head {:href (str "/admin/list/delete/" list-id)} "Ta bort"]
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
                       (mk-list-dd (some-> a-list :parent :_id) :parent "tags-head")]]
                     [:tr
                      [:td.item-info-th
                       [:label "Lågprioriterad lista?"]]
                      [:td.item-info-th
                       (hf/check-box {:class "new-cb"} :low-prio (true? (:last a-list)))]]]))))

(defn edit-list!
    [{params :params}]
    (when (str/blank? (:entryname params))
        (throw+ (Exception. "list name is blank")))
    (update-list (-> (get-list (:list-id params))
                     (set-name (:entryname params))
                     (assoc :parent (mk-parent-map params)
                            :last   (some? (:low-prio params)))))
    (ring/redirect "/admin"))

(defn delete-list!
    [_ list-id]
    (delete-list list-id)
    (ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

