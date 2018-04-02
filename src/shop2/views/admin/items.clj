(ns shop2.views.admin.items
    (:require [shop2.db :refer :all]
              [shop2.extra :refer :all]
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
              [utils.core :as utils]
              [clojure.string :as str]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]))

;;-----------------------------------------------------------------------------

(defn-spec admin-info-part any?
    [item :shop/item]
    (named-div "Information"
               [:table.group
                [:tr
                 [:td.item-info-th [:label "Namn"]]
                 [:td (hf/text-field {:class "item-info"} :entryname (:entryname item))]]
                [:tr
                 [:td.item-info-th [:label "Pris"]]
                 [:td (hf/text-field {:class "item-info"} :price (:price item))]]
                [:tr
                 [:td.item-info-th [:label "URL"]]
                 [:td (hf/text-field {:class "item-info"} :url (:url item))]]
                [:tr
                 [:td.item-info-th [:label "Parent"]]
                 [:td (mk-list-dd (:parent item) :parent "item-info")]]
                [:tr
                 [:td.new-item-td "Project:"]
                 [:td.url-td (mk-project-dd nil :project "new-item-txt")]]
                [:tr
                 [:td.new-item-td "One Shot:"]
                 [:td.url-td (hf/check-box :one-shot "new-cb")]]
                ]))

;;-----------------------------------------------------------------------------

(defn-spec extract-id :shop/_id
    [params map?]
    (when-not (item-id-exists? (:_id params))
        (throw+ (ex-info "invalid id" {:type :input})))
    (:_id params))

(defn-spec extract-name :shop/entryname
    [params map?]
    (when (str/blank? (:entryname params))
        (throw+ (ex-info "invalid name" {:type :input})))
    (:entryname params))

(defn-spec extract-parent :shop/parent
    [params map?]
    (when-not (= (:parent params) no-id)
        (when-not (list-id-exists? (:parent params))
            (throw+ (ex-info "invalid parent" {:type :input})))
        (:parent params)))

(defn-spec extract-project (s/nilable :shop/project)
    [params map?]
    (when-not (= (:project params) no-id)
        (or (get-project (:project params))
            (throw+ (ex-info "invalid project" {:type :input})))))

(defn-spec extract-str (s/nilable :shop/string)
    [tag keyword?, params map?]
    (when-not (str/blank? (get params tag))
        (get params tag)))

(defn-spec extract-num (s/nilable double?)
    [tag keyword?, params map?]
    (when (extract-str tag params)
        (Double/valueOf (get params tag))))

;;-----------------------------------------------------------------------------

(defn edit-item
    [request item-id]
    (let [item (get-item item-id)]
        (common request "Edit item" [css-tags-tbl css-items]
                (hf/form-to
                    [:post "/admin/item/edited"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :_id (:_id item))
                    [:div
                     (homeback-button "/admin")
                     [:a.link-head {:href (str "/admin/item/delete/" item-id)} "Ta bort"]
                     (hf/submit-button {:class "button"} "Uppdatera")]
                    [:div
                     (admin-info-part item)
                     (named-div "Ny kategori:" (hf/text-field {:class "item-info"} :new-tag))
                     (tags-tbl nil (:tag item))]
                    ))))

(defn edit-item!
    [{params :params}]
    (update-item (-> (extract-id params)
                     (get-item)
                     (set-name (extract-name params))
                     (assoc :parent (extract-parent params)
                            :project (extract-project params)
                            :oneshot (or (:oneshot params) false)
                            :price (extract-num :price params)
                            :url (extract-str :url params)
                            :tag (extract-tag params))))
    (ring/redirect (str "/admin/item/edit/" (extract-id params))))

;;-----------------------------------------------------------------------------

(defn new-item
    [request]
    (common request "Skapa ny sak" [css-tags-tbl]
            (hf/form-to
                [:post "/admin/item/new"]
                (ruaf/anti-forgery-field)
                [:div
                 (homeback-button "/admin")
                 [:a.link-head (hf/submit-button {:class "button"} "Skapa")]]
                [:div
                 (hf/text-field :entryname)
                 (hf/drop-down :parent (->> (get-lists)
                                            (sort-by :entrynamelc)
                                            (map (fn [l] [(:entryname l) (:_id l)]))
                                            (concat [["" no-id]])))
                 (admin-info-part nil)
                 (named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
                 (tags-tbl nil nil)])))

(defn new-item!
    [{params :params}]
    (add-item (create-item-obj (extract-name params)
                               (extract-parent params)
                               (extract-tag params)
                               (extract-project params)
                               (extract-str :url params)
                               (extract-num :price params)
                               (or (:oneshot params) false))))

;;-----------------------------------------------------------------------------

(defn delete-item!
    [_ item-id]
    (delete-item item-id)
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

(defn bulk-edit-items
    [request]
    (common request "Edit items" [css-tags-tbl css-items]
            (hf/form-to
                [:post "/admin/item/bulk-edit"]
                (ruaf/anti-forgery-field)
                [:div
                 (homeback-button "/admin")
                 [:a.link-head (hf/submit-button {:class "button"} "Uppdatera")]]
                [:table
                 [:tr
                  [:th [:label "X"]]
                  [:th.width-400px [:label.fz24.width-100p "Name"]]
                  [:th.width-200px [:label.fz24.width-100p "Tags"]]
                  [:th.width-200px [:label.fz24.width-100p "Parent"]]
                  [:th.width-200px [:label.fz24.width-100p "Project"]]
                  ]
                 (let [tags-dd  (get-tags-dd)
                       lists-dd (get-lists-dd)
                       projs-dd  (get-projects-dd)]
                     (for [item (->> (get-items) (sort-by :entrynamelc))]
                      [:tr
                       [:td
                        (hf/check-box {:class "new-cb"}
                                      (utils/mk-tag (:_id item) "delete"))]
                       [:td.width-400px
                        (hf/text-field {:class "fz24 width-100p"}
                                       (utils/mk-tag (:_id item) "name")
                                       (:entryname item))]
                       [:td.width-200px
                        (hf/drop-down {:class "fz24 width-100p"}
                                      (utils/mk-tag (:_id item) "tag")
                                      tags-dd
                                      (some-> item :tag :_id))]
                       [:td.width-200px
                        (hf/drop-down {:class "fz24 width-100p"}
                                      (utils/mk-tag (:_id item) "parent")
                                      lists-dd
                                      (:parent item))]
                       [:td.width-200px
                        (hf/drop-down {:class "fz24 width-100p"}
                                       (utils/mk-tag (:_id item) "project")
                                      projs-dd
                                      (some-> item :project :_id))]
                       ]))])))

(defn- purge-no-id
    [v]
    (when (and (not= v no-id) (s/valid? :shop/_id v))
        v))

(defn bulk-edit-items!
    [{params :params}]
    (doseq [item (get-items)
            :let [do-del (= (get params (utils/mk-tag (:_id item) "delete")) "true")
                  iname (str/trim (get params (utils/mk-tag (:_id item) "name")))
                  tag (purge-no-id (get params (utils/mk-tag (:_id item) "tag")))
                  parent (purge-no-id (get params (utils/mk-tag (:_id item) "parent")))
                  project (purge-no-id (get params (utils/mk-tag (:_id item) "project")))
                  ]
            :when (or do-del
                      (not= iname (:entryname item))
                      (not= tag (some-> item :tag :_id))
                      (not= parent (:parent item))
                      (not= project (:project item))
                      )]
        (if do-del
            (delete-item (:_id item))
            (update-item (-> item
                             (set-name iname)
                             (assoc :tag (when tag (get-tag tag))
                                    :parent parent
                                    :project (when project (get-project project))
                                    )))))
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

