(ns shop2.views.lists
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
              [clojure.string :as str]
              [ring.util.response :as ring]))

;;-----------------------------------------------------------------------------

(defn mk-tag-row
    [tag]
    [:tr [:td.tags-row {:colspan 3} tag]])

(defn mk-name
    [item]
    (if (> (or (:numof item) 1) 1)
        (format "%s (%d)" (:entryname item) (:numof item))
        (:entryname item)))

(defn mk-item-a
    [a-list item active? text]
    [:a.item-text
     {:href (str (if active? "/user/list/done/" "/user/list/undo/") (:_id a-list) "/" (:_id item))}
     text])

(defn mk-item-row*
    [a-list item active?]
    (list
        [:td.item-text-td (mk-item-a a-list item active? (mk-name item))]
        (when active? (list
                          [:td
                           [:a.arrow {:href (str "/user/list/up/" (:_id a-list) "/" (:_id item))} "▲"]]
                          [:td
                           [:a.arrow {:href (str "/user/list/down/" (:_id a-list) "/" (:_id item))} "▼"]]
                          (when-not (str/blank? (:url item))
                              [:td.item-menu-td
                               [:a.item-text {:href (:url item) :target "_blank"} "Link"]])))))

(defn mk-item-row
    [a-list item active?]
    (if active?
        [:tr.item-text-tr (mk-item-row* a-list item active?)]
        [:tr.item-text-tr.done (mk-item-row* a-list item active?)]))

(defn- sort-items
    [item-list]
    (->> item-list
         (group-by #(get-in % [:tag :entryname]))
         (into (sorted-map))
         seq))

(defn mk-items
    [a-list row-type]
    (let [item-list (if (= row-type :active)
                        (remove #(:finished %) (:items a-list))
                        (filter #(:finished %) (:items a-list)))]
        (for [[tag items] (sort-items item-list)]
            (list
                (mk-tag-row tag)
                (for [item (sort-by :entrynamelc items)]
                    (mk-item-row a-list item (= row-type :active)))))))

(defn mk-list-tbl
    [base-id a-list]
    [:table.list-tbl
     ; row with list name
     [:tr
      [:td
       [:table.width-100p
        [:tr
         [:th.align-l (home-button)]
         [:th.list-name-th
          [:label.list-name (:entryname a-list)]]
         [:th.align-r
          [:a.link-flex {:href (str "/user/item/add/" (:_id a-list))} "+"]]]]]]
     ; rows with not-done items
     [:tr
      [:td
       [:table.width-100p
        (mk-items a-list :active)]]]
     [:tr
      [:td
       [:table.width-100p
        [:tr
         [:td.done-td {:colspan "1"} "Avklarade"]
         [:td.done-td.align-r {:colspan "1"}
          [:a.link-thin {:href (str "/user/list/clean/" base-id "/" (:_id a-list))} "Rensa"]]]]]]
     ; rows with done items
     (mk-items a-list :inactive)])

(defn show-list-page
    [request list-id]
    (common-refresh request (:entryname (get-list list-id)) [css-lists]
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
