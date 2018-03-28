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
              [utils.core :as utils]
              [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]
              [clojure.pprint :as pp]
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
              [ring.util.response :as ring]))

;;-----------------------------------------------------------------------------

(defn- info-part
    [item]
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

(defn- extract-id
    [params]
    (when-not (item-id-exists? (:_id params))
        (throw+ (ex-info "invalid id" {:type :input})))
    (:_id params))

(defn- extract-name
    [params]
    (when (str/blank? (:entryname params))
        (throw+ (ex-info "invalid name" {:type :input})))
    (:entryname params))

(defn- extract-parent
    [params]
    (when-not (= (:parent params) no-id)
        (when-not (list-id-exists? (:parent params))
            (throw+ (ex-info "invalid parent" {:type :input})))
        (:parent params)))

(defn- extract-project
    [params]
    (when-not (= (:project params) no-id)
        (or (get-project (:project params))
            (throw+ (ex-info "invalid project" {:type :input})))))

(defn- extract-str
    [tag params]
    (when-not (str/blank? (get params tag))
        (get params tag)))

(defn- extract-num
    [tag params]
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
                     (info-part item)
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
                 (info-part nil)
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

(defn- item-tag
    [item]
    (some-> item :tag :_id))

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
                                     (get-tags-dd)
                                     (item-tag item))]
                      [:td.width-200px
                       (mk-list-dd (:parent item)
                                   (utils/mk-tag (:_id item) "parent")
                                   "fz24 width-100p")]
                      [:td.width-200px
                       (mk-project-dd (:project item)
                                      (utils/mk-tag (:_id item) "project")
                                      "fz24 width-100p")]
                      ])])))

(defn bulk-edit-items!
    [{params :params}]
    (doseq [item (get-items)
            :let [do-del (get params (utils/mk-tag (:_id item) "delete"))
                  iname (get params (utils/mk-tag (:_id item) "name"))
                  tag (get params (utils/mk-tag (:_id item) "tag"))
                  parent (get params (utils/mk-tag (:_id item) "parent"))
                  project (get params (utils/mk-tag (:_id item) "project"))
                  ]
            :when (or do-del
                      (not= iname (:entryname item))
                      (not= tag (item-tag item))
                      (not= parent (:parent item))
                      (not= project (:project item))
                      )]
        (if do-del
            (delete-item (:_id item))
            (update-item (-> item
                             (set-name iname)
                             (assoc :tag (when-not (= tag no-id) (get-tag tag))
                                    :parent (when-not (= parent no-id) parent)
                                    :project (when-not (= project no-id) (get-project project))
                                    )))))
    (ring/redirect "/admin/"))

;;-----------------------------------------------------------------------------

