(ns shop2.views.admin.tags
    (:require [mongolib.core :as db]
              [shop2.extra :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.home :refer :all]
              [shop2.views.css :refer :all]
              [shopdb.misc :refer :all]
              [shopdb.tags :refer :all]
              [shopdb.items :refer :all]
              [shopdb.lists :refer :all]
              [shopdb.menus :refer :all]
              [shopdb.projects :refer :all]
              [shopdb.recipes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.string :as str]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn new-tag
    [request]
    (common request "Skapa ny tag" []                 ; css-lists-new
            (hf/form-to
                [:post "/admin/tag/new"]
                (ruaf/anti-forgery-field)
                [:div
                 (admin-home-button)
                 (hf/submit-button {:class "button"} "Skapa")]
                (named-div "Namn:"
                           (hf/text-field {:class "fnt-24px width-200px"} :entryname))
                (named-div "Parent:"
                           (mk-list-dd nil :parent "fnt-24px width-200px")))))

(defn new-tag!
    [{params :params}]
    (add-tag (:entryname params) (:parent params))
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

(defn edit-tag
    [request tag-id]
    (let [tag (get-tag tag-id)]
        (common request "Edit tag" [css-tags css-tags-tbl]
                (hf/form-to
                    [:post "/admin/tag/edited"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :_id (:_id tag))
                    [:table
                     [:tr
                      [:td {:colspan 2}
                       [:div
                        (admin-home-button)
                        [:a.link-flex {:href (str "/admin/tag/delete/" tag-id)} "Ta bort"]
                        [:a.link-flex {:href (str "/admin/tag/delete-all/" tag-id)} "Bort överallt"]
                        [:a.link-flex (hf/submit-button {:class "button"} "Uppdatera")]]]]
                     [:tr
                      [:td {:style "padding: 40px 25px; width: 50px"}
                       [:label "Namn"]]
                      [:td
                       (hf/text-field {:class "new-item-txt"}
                                      :entryname
                                      (:entryname tag))]]
                     [:tr
                      [:td {:style "padding: 40px 25px; width: 50px"}
                       [:label "Parent"]]
                      [:td
                       (mk-list-dd (:parent tag) :parent "fnt-24px width-200px")]]]))))

(defn edit-tag!
    [{params :params}]
    (update-tag (:_id params) (:entryname params) (:parent params))
    (ring/redirect (str "/admin/tag/edit/" (:_id params))))

;;-----------------------------------------------------------------------------

(defn delete-tag!
    [_ tag-id]
    (delete-tag tag-id)
    (ring/redirect "/admin/"))

(defn delete-tag-all!
    [_ tag-id]
    (delete-tag-all tag-id)
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

(defn bulk-edit-tags
    [request]
    (common request "Edit tags" [css-tags-tbl css-items]
            (hf/form-to
                [:post "/admin/tag/bulk-edit"]
                (ruaf/anti-forgery-field)
                [:div
                 (homeback-button "/admin")
                 [:a.link-head (hf/submit-button {:class "button"} "Uppdatera")]]
                [:table
                 [:tr
                  [:th [:label "X"]]
                  [:th.width-200px [:label.fnt-24px.width-100p "Name"]]
                  [:th.width-200px [:label.fnt-24px.width-100p "Parent"]]
                  ]
                 (for [tag (->> (get-tags) (sort-by :entrynamelc))]
                     [:tr
                      [:td
                       (hf/check-box {:class "new-cb"}
                                     (utils/mk-tag (:_id tag) "delete"))]
                      [:td.width-200px
                       (hf/text-field {:class "fnt-24px width-100p"}
                                      (utils/mk-tag (:_id tag) "name")
                                      (:entryname tag))]
                      [:td.width-200px
                       (mk-list-dd (:parent tag) (utils/mk-tag (:_id tag) "parent") "fnt-24px width-100p")]])])))

(defn-spec ^:private purge-no-id :shop/parent
    [v :shop/_id]
    (when (and (not= v no-id) (s/valid? :shop/_id v))
        v))

(defn bulk-edit-tags!
    [{params :params}]
    (doseq [tag (get-tags)
            :let [do-del (= (get params (utils/mk-tag (:_id tag) "delete")) "true")
                  iname (str/trim (get params (utils/mk-tag (:_id tag) "name")))
                  parent (purge-no-id (get params (utils/mk-tag (:_id tag) "parent")))]
            :when (or do-del
                      (and (not= iname (:entryname tag)) (s/valid? :shop/entryname iname))
                      (not= parent (:parent tag)))]
        (if do-del
            (delete-tag (:_id tag))
            (update-tag (:_id tag) iname parent)))
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

(st/instrument)
