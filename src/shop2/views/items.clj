(ns shop2.views.items
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
              [clojure.pprint :as pp]
              [clojure.set :as set]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn- get-list-items
    [a-list]
    (let [items (get-items)
          id-parents (get-parents a-list)
          active-items (->> a-list
                            :items
                            (remove #(some? (:finished %)))
                            (map :_id)
                            set)]
        (->> items
             (filter #(or (nil? (:parent %)) (contains? id-parents (:parent %))))
             (remove #(contains? active-items (:_id %))))))

(defn- mk-add-item
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]
       [:td.item-tags-td
        [:div.item-tags (some-> item :tag :entryname)]]]]])

(defn- mk-add-item-no-tag
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]]]])

(defn- items-by-tags
    [items]
    (for [[k v] items]
        [:tr
         [:td.items-block
          [:table
           [:tr
            [:td.tags-head {:style "width: 100%"} [:label.tags-head k]]]]
          (map mk-add-item-no-tag (sort-by :entrynamelc v))]]))

(defn- mk-letter
    [items]
    [:tr
     [:td.items-block
      [:p.no-margin (hf/submit-button {:class "isb"} "\u2713")]
      (map mk-add-item (sort-by :entrynamelc items))]])

(defn- items-by-name
    [alpha]
    (map #(mk-letter (get-in alpha [:items %])) (-> alpha :found seq sort)))

(defn- items->alpha
    [items*]
    (loop [items items*
           acc {:found #{} :items {}}]
        (if (empty? items)
            acc
            (let [item (first items)
                  c* (-> item :entrynamelc first)
                  c (if (<= (int \0) (int c*) (int \9)) \0 c*)]
                (recur (rest items) (-> acc (update :found conj c) (update-in [:items c] conj item)))))))

(defn- item-list
    [a-list sort-type]
    (if (= sort-type :tags)
        (items-by-tags (->> (get-list-items a-list)
                            (group-by #(-> % :tag :entryname))
                            (into (sorted-map))))
        (items-by-name (->> (get-list-items a-list) items->alpha))))

(defn- sort-button
    [st list-id]
    (if (= st :tags)
        [:a.link-flex {:href (str "/user/item/add/set-sort/" list-id "/name")} "N"]
        [:a.link-flex {:href (str "/user/item/add/set-sort/" list-id "/tags")} "T"]))

(defn add-items
    [request list-id]
    (let [a-list (get-list list-id)
          sort-type (or (some-> request udata :properties :items :sort-type keyword) :name)]
        (common request "Välj sak" [css-items css-tags-tbl]
                (hf/form-to
                    [:post "/user/item/add"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :list-id list-id)
                    [:div
                     (homeback-button (str "/user/list/get/" list-id))
                     (sort-button sort-type list-id)
                     [:a.link-flex {:href (str "/user/item/new/" list-id)} "+"]
                     (hf/submit-button {:class "button-s"} "\u2713")]
                    [:div
                     [:table (item-list a-list sort-type)]]))))

;;-----------------------------------------------------------------------------

(defn add-items!
    [{params :params}]
    (doseq [item-id (->> params keys (map name) (filter #(s/valid? :shop/_id %)))]
        (item->list (:list-id params) item-id))
    (ring/redirect (str "/user/item/add/" (:list-id params))))

;;-----------------------------------------------------------------------------

(defn info-part
    []
    (named-div "Information"
               [:table
                [:tr
                 [:td.new-item-td "Namn:"]
                 [:td (hf/text-field {:class "new-item-txt" :autofocus true} :new-item-name)]]
                [:tr
                 [:td.new-item-td "Pris:"]
                 [:td (hf/text-field {:class "new-item-txt"} :new-item-price)]]
                [:tr
                 [:td.new-item-td "URL:"]
                 [:td.url-td (hf/text-field {:class "new-item-txt"} :new-item-url)]]
                [:tr
                 [:td.new-item-td "Project:"]
                 [:td.url-td (mk-project-dd nil :project "new-item-txt")]]
                [:tr
                 [:td.new-item-td "One Shot:"]
                 [:td.url-td (hf/check-box {:class "new-cb"} :one-shot)]]
                ]))

(defn new-list-item
    [request list-id]
    (common request "Skapa ny sak" [css-items css-tags-tbl]
            (hf/form-to
                [:post "/user/item/new"]
                (ruaf/anti-forgery-field)
                (hf/hidden-field :list-id list-id)
                [:div
                 (homeback-button (str "/user/item/add/" list-id))
                 [:a.link-head (hf/submit-button {:class "button"} "Skapa")]]
                [:div
                 (info-part)
                 (named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
                 (tags-tbl list-id nil)])))

;;-----------------------------------------------------------------------------

(defn new-list-item!
    [{params :params}]
    (let [proj (when (and (s/valid? :shop/_id (name (:project params))) (not= (name (:project params)) no-id))
                   (get-project (:project params)))
          item (create-item-obj (:new-item-name params)
                                (:list-id params)
                                (extract-tag params)
                                proj
                                (:new-item-url params)
                                (str->num (:new-item-price params))
                                (= (:one-shot params) "true"))]
        (if (:oneshot item)
            (oneshot->list (:list-id params) item)
            (item->list (:list-id params) (:_id (add-item item))))
        (ring/redirect (str "/user/item/add/" (:list-id params)))))

;;-----------------------------------------------------------------------------

(defn set-item-sort
    [request listid sort-type]
    (set-user-property (uid request) :items {:sort-type sort-type})
    (ring/redirect (str "/user/item/add/" listid)))

