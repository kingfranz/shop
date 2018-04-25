(ns shop2.views.home
    (:require [shop2.extra :refer :all]
              [mongolib.core :as db]
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
     (for [a-list (maplist-sort [#(:last %)
                                 #(utils/neg (:count %))
                                 #(:entryname %)
                                 (fn [_] 0)]
                                lists)]
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
        [:a.link-flex {:href "/admin/"} (he/image "/images/settingsw.png")]
        [:a.link-flex {:href "/logout"} (he/image "/images/logout.png")]))

(defn- box-head
    [lnk txt extra]
     [:table.width-100p
      [:tr.width-100p
       (when extra [:td extra])
       [:td.width-100p [:a.link-home.width-100p {:href lnk} txt]]]])

(defn home-page
    [request]
    (common-refresh request "Shopping" [css-home-tree css-home css-menus]
                    [:div.column
                     (if (want-tree? request)
                         (box-head "/user/home/prio" "Num" (lo-admin request))
                         (box-head "/user/home/tree" "Tree" (lo-admin request)))
                     [:div.home-box (list-tree request)]]
                    [:div.column
                     (box-head "/user/menu/edit" "Veckomeny" nil)
                     [:div.home-box (menu-list)]]
                    [:div.column
                     (box-head "/user/project/edit" "Projekt" (small-proj-sort-btn request))
                     [:div.home-box (display-projects request)]]
                    [:div.column
                     [:div.column
                      (box-head "/user/recipe/new" "Recept" nil)
                      [:div.home-box (recipe-list)]]
                     [:div.column
                      (box-head "/user/note/new" "Note" nil)
                      [:div.home-box (note-list)]]]
                    ))

(defn-spec set-home-type any?
    [request map?, list-type keyword?]
    (set-user-property (uid request) :home {:list-type list-type})
    (ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

(st/instrument)
