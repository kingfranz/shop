(ns shop2.views.home
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.views.projects :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items :refer :all]
              [shop2.db.menus :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.recipes :refer :all]
              [shop2.db.notes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [hiccup.element :as he]
              [ring.util.response :as ring]
              [orchestra.spec.test :as st]
              [orchestra.core :refer [defn-spec]]
              [clojure.string :as str]
              [clojure.spec.alpha :as s]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn-spec want-tree? boolean?
    [req map?]
    (= (->> req udata :properties :home :list-type) "tree"))

(defn-spec mk-list-name string?
    [slist :shop/list-with-count]
    (str (:entryname slist) " - " (:count slist)))

(defn-spec sub-tree any?
    [lists :shop/lists-with-count, this-list :shop/list-with-count]
    (let [sub-lists (->> lists
                         (filter #(some->> % :parent :_id (= (:_id this-list))))
                         (sort-by :entryname))]
        [:li
         [:a.link-thick {:href (str "/user/list/get/" (:_id this-list))} (mk-list-name this-list)]
         (when (seq sub-lists)
             [:ul
              (map #(sub-tree lists %) sub-lists)])]))

(defn-spec list-cmp integer?
    [l1 :shop/list-with-count, l2 :shop/list-with-count]
    (or (comp-nil (:last l1) (:last l2))
        (comp-nil (:count l2) (:count l1))
        (comp-nil (:entryname l1) (:entryname l2))
        0))

(defn-spec list-row any?
    [a-list :shop/list-with-count]
    [:table.width-90p
     [:tr
      [:td.v-align [:a.link-thick {:href (str "/user/list/get/" (:_id a-list))}
                    (str (:entryname a-list) " " (:count a-list))]]
      [:td.r-align [:label (if (zero? (:total a-list)) "" (:total a-list))]]]])

(defn-spec list-tbl any?
    [lists :shop/lists-with-count]
    [:table.width-100p
     (for [a-list (sort-by identity list-cmp lists)]
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
    [menu :shop/x-menu]
    ; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
    [:tr
     [:td.menu-date-td [:label.menu-date (menu-date-short menu)]]
     [:td [:div.menu-txt [:label.home-margin (:entryname menu)]]]])

(defn-spec menu-list any?
    []
    [:table
     (map mk-menu-row (get-menus (today) (new-menu-end)))])

(defn- recipe-list
    []
    [:table
     (map (fn [r]
              [:tr
               [:td.home-margin
                [:a.link-thin {:href (str "/user/recipe/edit/" (:_id r))}
                 (:entryname r)]]])
          (sort-by :entrynamelc (get-recipe-names)))])

(defn- note-list
    []
    [:table
     (map (fn [r]
              [:tr
               [:td.home-margin
                [:a.link-thin {:href (str "/user/note/edit/" (:_id r))}
                 (:entryname r)]]])
          (sort-by :entrynamelc (get-note-names)))])

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
                     [:div.home-box (display-projects)]]
                    [:div.column
                     [:div.column
                      [:p.header [:a.link-home {:href "/user/recipe/new"} "Recept"]]
                      [:div.home-box (recipe-list)]]
                     [:div.column
                      [:p.header [:a.link-home {:href "/user/note/new"} "Note"]]
                      [:div.home-box (note-list)]]]
                    ))

(defn-spec set-home-type any?
    [request map?, list-type keyword?]
    (set-user-property (uid request) :home {:list-type list-type})
    (ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

(st/instrument)
