(ns shop2.views.recipe
    (:require [shop2.extra :refer :all]
              [mongolib.core :as db]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shopdb.misc :refer :all]
              [shopdb.tags :refer :all]
              [shopdb.items :refer :all]
              [shopdb.lists :refer :all]
              [shopdb.menus :refer :all]
              [shopdb.projects :refer :all]
              [shopdb.recipes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [utils.core :as utils]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [taoensso.timbre :as log]))

;;-----------------------------------------------------------------------------

(defn-spec ^:private mk-recipe-item any?
    [idx integer?, item-map :recipe/item]
    [:tr
     [:td.rec-item-td
      (hf/text-field
          {:class "recipe-item recipe-item-name"}
          (utils/mk-tag "recipe-item-name-" idx) (:text item-map))]
     [:td.rec-item-td
      (hf/text-field
          {:class "recipe-item recipe-item-unit"}
          (utils/mk-tag "recipe-item-unit-" idx) (:unit item-map))]
     [:td.rec-item-td
      (hf/text-field
          {:class "recipe-item recipe-item-amount"}
          (utils/mk-tag "recipe-item-amount-" idx) (:amount item-map))]])

(defn-spec ^:private mk-recipe-item-list any?
    [recipe (s/nilable :shop/recipe)]
    (map #(mk-recipe-item %
                          (when (< % (count (:items recipe)))
                              (nth (:items recipe) %)))
         (range (+ (count (:items recipe)) 5))))

(defn-spec show-recipe-page any?
    [request map?, recipe (s/nilable :shop/recipe)]
    (common request "Recept" [css-recipe]
            (hf/form-to
                [:post (if (nil? recipe) "/user/recipe/new" "/user/recipe/edit")]
                (ruaf/anti-forgery-field)
                (hf/hidden-field :recipe-id (:_id recipe))
                (hf/hidden-field :num-items (+ (count (:items recipe)) 5))
                [:div
                 (home-button)
                 (when recipe
                     [:a.link-flex {:href (str "/user/recipe/delete/" (:_id recipe))} "Ta bort!"])
                 (hf/submit-button {:class "button"}
                                   (if (nil? recipe) "Skapa" "Updatera!"))]
                [:table
                 [:tr [:th "Namn"]]
                 [:tr
                  [:td.rec-title-txt-td (hf/text-field {:class "rec-title-txt"}
                                                       :recipe-name (:entryname recipe))]]
                 [:tr [:td.btn-spacer ""]]]
                [:table.recipe-table
                 [:tr [:th "Ingrediens"] [:th "Enhet"] [:th "Mängd"]]
                 (mk-recipe-item-list recipe)
                 [:tr [:td.btn-spacer ""]]]
                [:table.recipe-url-table
                 [:tr
                  [:td.recipe-url1 "URL"]
                  [:td.recipe-url2
                   (hf/text-field {:class "recipe-item recipe-url-field"} :recipe-url (:url recipe))]
                  [:td [:a.link-thin {:href (:url recipe) :target "_blank"} "GO"]]]
                 [:tr [:td.btn-spacer ""]]]
                [:div.rec-area-div (hf/text-area {:class "recipe-item recipe-area"}
                                                 :recipe-area
                                                 (:text recipe))])))

;;-----------------------------------------------------------------------------

(defn-spec edit-recipe any?
    [request map?, recipe-id :shop/_id]
    (show-recipe-page request (get-recipe recipe-id)))

(defn-spec new-recipe any?
    [request map?]
    (show-recipe-page request nil))

;;-----------------------------------------------------------------------------

(defn-spec ^:private get-r-item (s/nilable :recipe/item)
    [params map?, i integer?]
    (let [item {:text   (get params (utils/mk-tag "recipe-item-name-" i))
                :unit   (or (get params (utils/mk-tag "recipe-item-unit-" i)) "")
                :amount (or (get params (utils/mk-tag "recipe-item-amount-" i)) "")}]
        (when (s/valid? :recipe/item item)
            item)))

(defn-spec ^:private get-r-items (s/coll-of :recipe/item)
    [params map?]
    (try+
       (->> params
           :num-items
           Integer/valueOf
           range
           (map #(get-r-item params %))
           (remove nil?)
           vec)
       (catch Exception e
           (log/error "get-r-items:" e)
           [])))

(defn edit-recipe!
    [{params :params}]
    (update-recipe (-> (get-recipe (:recipe-id params))
                       (set-name (:recipe-name params))
                       (assoc :url (or (:recipe-url params) "")
                              :text (or (:recipe-area params) "")
                              :items (get-r-items params))))
    (ring/redirect (str "/user/recipe/edit/" (:recipe-id params))))

(defn new-recipe!
    [{params :params}]
    (let [ret (add-recipe (-> (create-entity (:recipe-name params))
                              (assoc :url (:recipe-url params)
                                     :text (:recipe-area params)
                                     :items (get-r-items params))))]
        (ring/redirect (str "/user/recipe/edit/" (:_id ret)))))

(defn delete-recipe!
    [_ id]
    (delete-recipe id)
    (ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

(st/instrument)
