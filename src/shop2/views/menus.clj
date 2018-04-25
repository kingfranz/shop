(ns shop2.views.menus
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
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clj-time.format :as f]
              [hiccup.form :as hf]
              [utils.core :as utils]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [clojure.string :as str]
              [clj-time.core :as t]))

;;-----------------------------------------------------------------------------

(defn menu-new-range
    []
    (time-range (today) (new-menu-end) (t/days 1)))

(defn-spec ^:private mk-recipe-link any?
    [menu :shop/x-menu]
    (when (:recipe menu)
        [:a.link-thin {:href (str "/user/recipe/" (:_id (:recipe menu)))}
         (:entryname (:recipe menu))]))

(defn-spec ^:private mk-recipe-add-del any?
    [menu :shop/x-menu, r-link? boolean?]
    (when r-link?
        (if (:recipe menu)
            [:a.link-thin {:href (str "/user/menu/recipe-/" (menu-date-key (:date menu)))} "-"]
            [:a.link-thin {:href (str "/user/menu/choose-recipe/" (menu-date-key (:date menu)))} "+"]
            )))

(defn-spec ^:private mk-menu-row any?
    [menu :shop/x-menu, r-link? boolean?]
    [:tr
     [:td.menu-date-td
      [:label.menu-date (menu-date-short menu)]]
     (if r-link?
         [:td.menu-text-td
          (hf/text-field {:class "menu-text"}
                         (utils/mk-tag "txt" (dt->str (:date menu)))
                         (:entryname menu))]
         [:td.menu-text-td
          [:label.menu-text-old (:entryname menu)]])
     [:td.menu-ad-td (mk-recipe-add-del menu r-link?)]
     [:td.menu-link-td (mk-recipe-link menu)]])

(defn edit-menu
    [request]
    (common request "Veckomeny" [css-menus]
            (hf/form-to
                [:post "/user/menu/edit"]
                (ruaf/anti-forgery-field)
                [:table.menu-table
                 [:tr
                  [:td
                   (home-button)
                   (hf/submit-button {:class "button"} "Updatera!")]]]
                [:table.menu-table
                 (map #(mk-menu-row % false) (get-menus (old-menu-start) (today)))
                 (map #(mk-menu-row % true) (get-menus (today) (new-menu-end)))])))

;;-----------------------------------------------------------------------------

(defn edit-menu!
    [{params :params}]
    (doseq [dt (menu-new-range)
            :let [txt     (get-param params "txt" (dt->str dt))
                  db-menu (get-menu dt)]
            :when (and (not (str/blank? txt))
                       (or (nil? db-menu)
                           (not= txt (:entryname db-menu))))]
        (if (some? db-menu)
            (update-menu (set-name db-menu txt))
            (add-menu (assoc (create-entity txt) :date dt :recipe nil))))
    (ring/redirect "/user/menu/edit"))

;;-----------------------------------------------------------------------------

(defn recipe->menu
    [_ recipe-id menu-date]
    (add-recipe-to-menu (f/parse menu-date) recipe-id)
    (ring/redirect "/user/menu/edit"))

(defn recipe<-menu
    [_ menu-date]
    (remove-recipe-from-menu (f/parse menu-date))
    (ring/redirect "/user/menu/edit"))

;;-----------------------------------------------------------------------------

(defn choose-recipe
    [request menu-date]
    (common request "Välj recept" [css-menus]
            [:table.menu-table
             [:tr
              [:td.menu-head-td (home-button)]
              [:td.menu-head-td [:a.link-head {:href "/menu"} "Cancel"]]]]
            [:table.menu-table
             (map (fn [r]
                      [:tr
                       [:td
                        [:a.link-thin {:href (str "/user/menu/recipe+/" (:_id r) "/" menu-date)}
                         (:entryname r)]]])
                  (get-recipes))]))

;;-----------------------------------------------------------------------------


(st/instrument)
