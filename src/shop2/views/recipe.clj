(ns shop2.views.recipe
  	(:require 	[shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.views.layout :refer :all]
                 [shop2.views.common       	:refer :all]
                 [shop2.views.css          	:refer :all]
                 [shop2.db.tags :refer :all]
                 [shop2.db.items			:refer :all]
                 [shop2.db.lists 			:refer :all]
                 [shop2.db.menus 			:refer :all]
                 [shop2.db.projects 		:refer :all]
                 [shop2.db.recipes 		:refer :all]
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
                 [garden.units        	:as u]
                 [garden.selectors    	:as sel]
                 [garden.stylesheet   	:as ss]
                 [garden.color        	:as color]
                 [garden.arithmetic   	:as ga]
                 [hiccup.core :as h]
                 [hiccup.def          	:as hd]
                 [hiccup.element      	:as he]
                 [hiccup.form         	:as hf]
                 [hiccup.page         	:as hp]
                 [hiccup.util         	:as hu]
                 [ring.util.anti-forgery :as ruaf]
                 [ring.util.response     	:as ring]))

;;-----------------------------------------------------------------------------

(defn mk-tag
	[s i]
	(keyword (str s i)))

(defn mk-recipe-item
	[idx item-map]
	[:tr
		[:td.rec-item-td
			(hf/text-field
				{:class "recipe-item recipe-item-name"}
				(mk-tag "recipe-item-name-" idx) (:text item-map))]
		[:td.rec-item-td
			(hf/text-field
				{:class "recipe-item recipe-item-unit"}
				(mk-tag "recipe-item-unit-" idx) (:unit item-map))]
		[:td.rec-item-td
			(hf/text-field
				{:class "recipe-item recipe-item-amount"}
				(mk-tag "recipe-item-amount-" idx) (:amount item-map))]])

(defn mk-recipe-item-list
	[recipe]
	(map #(mk-recipe-item %
						  (when (< % (count (:items recipe)))
						  	(nth (:items recipe) %)))
		 (range (+ (count (:items recipe)) 5))))

(defn show-recipe-page
    [request recipe]
	(common request "Recept" [css-recipe]
		(hf/form-to {:enctype "multipart/form-data"}
    		[:post (if (nil? recipe) "/user/create-recipe" "/user/update-recipe")]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :recipe-id (:_id recipe))
        	(hf/hidden-field :num-items (+ (count (:items recipe)) 5))
        	[:div
        		(home-button)
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
	        			(hf/text-field
	        				{:class "recipe-item recipe-url-field"}
	        				:recipe-url
	        				(:url recipe))]
	        		[:td [:a.link-thin {:href (:url recipe) :target "_blank"} "GO"]]]
	        	[:tr [:td.btn-spacer ""]]]
	        [:div.rec-area-div (hf/text-area {:class "recipe-item recipe-area"}
	        			 			   :recipe-area
	        			 			   (:text recipe))])))

;;-----------------------------------------------------------------------------

(defn edit-recipe
    [request recipe-id]
    {:pre [(utils/valid? :shop/_id recipe-id) (utils/valid? map? request)]}
	(show-recipe-page request (get-recipe recipe-id)))

(defn new-recipe
    [request]
	(show-recipe-page request nil))

;;-----------------------------------------------------------------------------

(defn assoc-if
	([k v]
    (assoc-if k v {}))
	([k v m]
    {:pre [(utils/valid? keyword? k) (utils/valid? map? m)]
     :post [(utils/valid? map? %)]}
	(if (or (nil? v) (and (string? v) (str/blank? v)))
		m
		(assoc m k v))))

(defn float-if
	[s]
	(when-not (str/blank? s)
		(Double/valueOf s)))

(defn get-r-item
	[params i]
	(let [item (->> (assoc-if :text   (get params (mk-tag "recipe-item-name-" i)))
		            (assoc-if :unit   (get params (mk-tag "recipe-item-unit-" i)))
		            (assoc-if :amount (some->> (mk-tag "recipe-item-amount-" i)
		            				  		   (get params)
		            				  		   (float-if))))]
		(when-not (empty? item)
			item)))

(defn get-r-items
	[params]
	(vec (remove nil? (map #(get-r-item params %) (range (Integer/valueOf (:num-items params)))))))

(defn update-recipe!
	[{params :params}]
	(update-recipe
   		(->> (assoc-if :_id       (:recipe-id params))
			 (assoc-if :url       (:recipe-url params))
			 (assoc-if :entryname (:recipe-name params))
			 (assoc-if :text      (:recipe-area params))
			 (assoc-if :items     (get-r-items params))))
	(ring/redirect (str "/user/recipe/" (:recipe-id params))))

(defn create-recipe!
	[{params :params}]
	(let [ret (add-recipe
             	(->> (assoc-if :url       (:recipe-url params))
					 (assoc-if :entryname (:recipe-name params))
					 (assoc-if :text      (:recipe-area params))
					 (assoc-if :items     (get-r-items params))))]
		(ring/redirect (str "/user/recipe/" (:_id ret)))))

;;-----------------------------------------------------------------------------
