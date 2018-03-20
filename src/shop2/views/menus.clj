(ns shop2.views.menus
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

(defn- mk-mtag
    [s dt]
    (keyword (str s "-" (menu-date-key dt))))

(defn- mk-recipe-link
    [menu r-link?]
    (when (:recipe menu)
        [:a.link-thin {:href (str "/user/recipe/" (:_id (:recipe menu)))}
         (:entryname (:recipe menu))]))

(defn- mk-recipe-add-del
    [menu r-link?]
    (when r-link?
        (if (:recipe menu)
            [:a.link-thin {:href (str "/user/menu/recipe-/" (menu-date-key (:date menu)))} "-"]
            [:a.link-thin {:href (str "/user/menu/choose-recipe/" (menu-date-key (:date menu)))} "+"]
            )))

(defn- mk-menu-row
    [menu r-link?]
    ; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
    (let [date-id (when (is-today? (:date menu)) "today")]
        [:tr
         [:td.menu-date-td
          (hf/label {:id date-id :class "menu-date"} :x
                    (menu-date-short menu))]
         (if r-link?
             [:td.menu-text-td
              (hf/hidden-field (mk-mtag "id" (:date menu)) (:_id menu))
              (hf/text-field {:class "menu-text"}
                             (mk-mtag "txt" (:date menu))
                             (:entryname menu))]
             [:td.menu-text-td
              (hf/label {:class "menu-text-old"} :x
                        (:entryname menu))])
         [:td.menu-ad-td (mk-recipe-add-del menu r-link?)]
         [:td.menu-link-td (mk-recipe-link menu r-link?)]
         ]))

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
                ;(throw+ (ex-info "test test" {:type ::db}))
                [:table.menu-table
                 (map #(mk-menu-row % false) (get-menus (old-menu-start) (today)))
                 (map #(mk-menu-row % true) (get-menus (today) (new-menu-end)))])))

;;-----------------------------------------------------------------------------

(defn edit-menu!
    [{params :params}]
    (let [db-menus (get-menus (today) (new-menu-end))]
        (doseq [dt (menu-new-range)
                :let [id (get params (mk-mtag "id" dt))
                      txt (get params (mk-mtag "txt" dt))
                      db-menu (some #(when (= (:date %) dt) %) db-menus)]
                :when (and (seq txt) (not= txt (:entryname db-menu)))]
            ;(println "update-menu!:" (mk-mtag "txt" dt) id txt db-menu)
            (if (seq id)
                (update-menu (merge db-menu {:entryname txt}))
                (add-menu {:date dt :entryname txt}))))
    (ring/redirect "/user/menu"))

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

