(ns shop2.views.home
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items :refer :all]
              [shop2.db.menus :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.recipes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [hiccup.element :as he]
              [ring.util.response :as ring]
              [orchestra.spec.test :as st]
              [orchestra.core :refer [defn-spec]]
              [clojure.string :as str]))

;;-----------------------------------------------------------------------------

(defn-spec want-tree? boolean?
    [req map?]
    (= (->> req udata :properties :home :list-type) "tree"))

(defn-spec mk-list-name string?
    [slist :shop/lists-with-count]
    (str (:entryname slist) " - " (:count slist)))

(defn-spec sub-tree any?
    [lists :shop/lists, slist :shop/lists-with-count]
    (let [sub-lists (->> lists
                         (filter #(some->> % :parent :_id (= (:_id slist))))
                         (sort-by :entryname))]
        [:li
         [:a.link-thick {:href (str "/user/list/get/" (:_id slist))} (mk-list-name slist)]
         (when (seq sub-lists)
             [:ul
              (map #(sub-tree lists %) sub-lists)])]))

(defn-spec list-cmp* integer?
    [x1 integer?, x2 integer?]
    (if (> (:count x1) (:count x2))
        -1
        (if (< (:count x1) (:count x2))
            1
            (compare (:entryname x1) (:entryname x2)))))

(defn-spec list-cmp integer?
    [x1 integer?, x2 integer?]
    (cond
        (and (:last x1) (:last x2)) (list-cmp* x1 x2)
        (and (:last x1) (not (:last x2))) 1
        (and (not (:last x1)) (:last x2)) -1
        :else (list-cmp* x1 x2)))

(defn-spec list-row any?
    [a-list :shop/list]
    [:table.width-90p
     [:tr
      [:td.v-align [:a.link-thick {:href (str "/user/list/get/" (:_id a-list))}
                    (str (:entryname a-list) " " (:count a-list))]]
      [:td.r-align [:label (if (zero? (:total a-list)) "" (:total a-list))]]]])

(defn-spec list-tbl any?
    [lists :shop/list]
    [:table.width-100p
     (for [a-list (->> lists (filter #(nil? (:finished %))) (sort-by identity list-cmp))]
         [:tr [:td (list-row a-list)]])])

(defn-spec list-tree any?
    [request map?]
    (let [lists (get-lists-with-count)]
        (if (want-tree? request)
            [:ul.tree
             (map #(sub-tree lists %)
                  (->> lists
                       (filter #(nil? (:parent %)))
                       (sort-by :entryname)))]
            (list-tbl lists))))

(defn-spec mk-menu-row any?
    [menu :shop/menu]
    ; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
    [:tr
     [:td.menu-date-td [:label.menu-date (menu-date-short menu)]]
     [:td [:div.menu-txt [:label.home-margin (:entryname menu)]]]])

(defn-spec menu-list any?
    []
    [:table
     (map mk-menu-row (get-menus (today) (new-menu-end)))])

(defn-spec mk-proj-row any?
    [r :shop/project]
    [:tr
     [:td.proj-pri
      [:label.home-margin (str (:priority r))]]
     [:td
      [:label.home-margin.clip (:entryname r)]]
     [:td.r-align
      [:label.proj-tags (:tag r)]]
     ])

(defn- projekt-list
    []
    [:table {:style "width:100%"}
     (->> (get-active-projects)
          (map mk-proj-row))])

(defn- recipe-list
    []
    [:table
     (map (fn [r]
              [:tr
               [:td.home-margin
                [:a.link-thin {:href (str "/user/recipe/edit/" (:_id r))}
                 (:entryname r)]]])
          (sort-by :entryname
                   #(compare (str/lower-case %1) (str/lower-case %2))
                   (get-recipe-names)))])

(defn- lo-admin
    [req]
    (if (-> req udata :roles (contains? :admin))
        [:a.link-flex.admin-btn {:href "/admin/"} (he/image "/images/settingsw.png")]
        [:a.link-flex.admin-btn {:href "/logout"} (he/image "/images/logout.png")]))

(defn home-page
    [request]
    (common-refresh request "Shopping" [css-home-tree css-home css-menus]
                    [:div.column
                     (if (want-tree? request)
                         [:a.link-home {:href "/user/home/prio"} "Num"]
                         [:a.link-home {:href "/user/home/tree"} "Tree"])
                     (lo-admin request)
                     [:div.home-box (list-tree request)]]
                    [:div.column
                     [:p.header [:a.link-home {:href "/user/menu/edit"} "Veckomeny"]]
                     [:div.home-box (menu-list)]]
                    [:div.column
                     [:p.header [:a.link-home {:href "/user/project/edit"} "Projekt"]]
                     [:div.home-box (projekt-list)]]
                    [:div.column
                     [:p.header [:a.link-home {:href "/user/recipe/new"} "Recept"]]
                     [:div.home-box (recipe-list)]]
                    ))

(defn-spec set-home-type any?
    [request map?, list-type keyword?]
    (set-user-property (uid request) :home {:list-type list-type})
    (ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

(st/instrument)
