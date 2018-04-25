(ns shop2.views.lists
    (:require [shop2.extra :refer :all]
              [mongolib.core :as db]
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
              [slingshot.slingshot :refer [throw+ try+]]
              [hiccup.form :as hf]
              [clojure.string :as str]
              [utils.core :as utils]
              [ring.util.response :as ring]))

;;-----------------------------------------------------------------------------

(defn-spec ^:private mk-tag-row any?
    [tag string?]
    [:tr [:td.tags-row {:colspan 3} [:label tag]]])

(defn-spec ^:private mk-name string?
    [item :list/item]
    (if (> (or (:numof item) 1) 1)
        (format "%s (%d)" (:entryname item) (:numof item))
        (:entryname item)))

(defn-spec ^:private mk-item-a any?
    [a-list :shop/list, item :list/item, active? boolean?, text string?]
    [:a.item-text
     {:href (str (if active? "/user/list/done/" "/user/list/undo/") (:_id a-list) "/" (:_id item))}
     text])

(defn-spec ^:private mk-item-row any?
           [a-list :shop/list, item :list/item, active? boolean?]
           [:tr {:class (if active? "bgrnd-white" "bgrnd-grey line-thru")}
            [:td.width-90p (mk-item-a a-list item active? (mk-name item))]
            (when active?
                (list [:td
                       [:a.arrow {:href (str "/user/list/up/" (:_id a-list) "/" (:_id item))} "▲"]]
                      [:td
                       [:a.arrow {:href (str "/user/list/down/" (:_id a-list) "/" (:_id item))} "▼"]]
                      (when-not (str/blank? (:url item))
                          [:td.width-30p
                           [:a.item-text {:href (:url item) :target "_blank"} "Link"]])))])

(defn-spec ^:private sort-by-key map?
    [target keyword?, item-list :list/items]
    (->> item-list
         (sort-by :entrynamelc)
         (group-by #(get-in % [target :entryname]))
         (into (sorted-map))))

(defn-spec ^:private mk-items any?
    [a-list :shop/list, active? boolean?]
    (let [item-list (if active?
                       (remove #(:finished %) (:items a-list))
                       (filter #(:finished %) (:items a-list)))
         projs (sort-by-key :project (filter #(some? (:project %)) item-list))
         tags (sort-by-key :tag (filter #(some? (:tag %)) item-list))
         no-p-or-t (remove #(or (some? (:tag %)) (some? (:project %))) item-list)]
       (list
           (for [[proj items] projs]
               (list
                   (mk-tag-row (str "Projekt: " proj))
                   (for [item items]
                       (mk-item-row a-list item active?))))
           (for [[tag items] tags]
               (list
                   (mk-tag-row (str "Kategori: " tag))
                   (for [item items]
                       (mk-item-row a-list item active?))))
           (when (seq no-p-or-t)
               (list
                   (mk-tag-row "* INGET *")
                   (for [item no-p-or-t]
                       (mk-item-row a-list item active?)))))
       ))

(defn-spec ^:private mk-list-header any?
           [a-list :shop/list]
           ; row with list name
           [:tr
            [:td
             [:table.width-100p
              [:tr
               [:th.l-align (home-button)]
               [:th.list-name-th
                [:label.list-name (:entryname a-list)]]
               [:th.r-align
                [:a.link-flex {:href (str "/user/item/add/" (:_id a-list))} "+"]]]]]])

(defn-spec ^:private mk-done-header any?
           [base-id :shop/_id, a-list :shop/list]
           ; row with list name
           [:tr
            [:td
             [:table.width-100p
              [:tr
               [:td.done-td "Avklarade"]
               [:td.done-td.r-align
                [:a.link-thin {:href (str "/user/list/clean/" base-id "/" (:_id a-list))} "Rensa"]]]]]])

(defn-spec ^:private mk-list-tbl any?
    [base-id :shop/_id, a-list :shop/list]
    [:table.list-tbl
     ; row with list name
     (mk-list-header a-list)
     ; rows with not-done items
     [:tr
      [:td
       [:table.width-100p
        (mk-items a-list true)]]]
     (mk-done-header base-id a-list)
     ; rows with done items
     (mk-items a-list false)])

(defn-spec show-list-page any?
    [request map?, list-id :shop/_id]
    (common-refresh request (:entryname (get-list list-id)) [css-lists css-items]
                    (loop [listid list-id
                           base-id list-id
                           acc []]
                        (if (some? listid)
                            (let [slist (get-list listid)]
                                (recur (-> slist :parent :_id) base-id (conj acc (mk-list-tbl base-id slist))))
                            (seq acc)))))

;;-----------------------------------------------------------------------

(defn item-done
    [_ list-id item-id]
    (finish-list-item list-id item-id)
    (ring/redirect (str "/user/list/get/" list-id)))

(defn item-undo
    [_ list-id item-id]
    (unfinish-list-item list-id item-id)
    (ring/redirect (str "/user/list/get/" list-id)))

(defn list-up
    [_ list-id item-id]
    (list-item+ list-id item-id)
    (ring/redirect (str "/user/list/get/" list-id)))

(defn list-down
    [_ list-id item-id]
    (list-item- list-id item-id)
    (ring/redirect (str "/user/list/get/" list-id)))

(defn clean-list
    [_ base-list list-id]
    (del-finished-list-items list-id)
    (ring/redirect (str "/user/list/get/" base-list)))

(st/instrument)
