(ns shop2.views.recipe
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[garden.core       :as g]
            	[garden.units      :as u]
            	[garden.selectors  :as sel]
            	[garden.stylesheet :as stylesheet]
            	[garden.color      :as color]
            	[hiccup.core              :as h]
            	[hiccup.def               :as hd]
            	[hiccup.element           :as he]
            	[hiccup.form              :as hf]
            	[hiccup.page              :as hp]
            	[hiccup.util              :as hu]
            	[ring.util.anti-forgery   :as ruaf]
            	[clojure.string           :as str]
            	[clojure.set              :as set]))

;;-----------------------------------------------------------------------------

(def css-recipe
	(g/css
		[:.rec-buttons-td {
			:width (u/px 267)
			:text-align :center
		}]
		[:.btn-spacer {
			:height (u/px 25)
		}]
		[:.rec-title-txt-td {
			:width (u/px 800)
			:text-align :center
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.rec-title-txt {
			:width layout/full
			:background-color layout/transparent
			:color :white
			:border 0
			:font-size (u/px 24)
		}]
		[:.recipe-item {
			:background-color layout/transparent
			:font-size (u/px 18)
			:color :white
			:border 0
		}]
		[:.rec-item-td {
			:border [[(u/px 1) :solid :grey]]
			:padding [[0 0 0 (u/px 10)]]
		}]
		[:.recipe-item-name {
			:width (u/px 590)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-item-unit {
			:width (u/px 70)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-item-amount {
			:width (u/px 110)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-url-table {
			:width layout/full
			:height (u/px 48)
		}]
		[:.recipe-url1 {
			:width (u/px 70)
			:font-size (u/px 24)
			:height (u/px 45)
		}]
		[:.recipe-url2 {
			:width (u/px 650)
			:height (u/px 45)
			:padding [[0 (u/px 10) (u/px 6) 0]]
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.recipe-url-field {
			:width layout/full
			:font-size (u/px 18)
			:vertical-align :center
		}]
		[:.rec-area-div {
			:width (u/px 800)
			:height (u/px 300)
		}]
		[:.recipe-area {
			:width layout/full
			:height layout/full
			:border [[(u/px 1) :solid :grey]]}]))

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
		 (range (+ (count (:items recipe)) 10))))

(defn show-recipe-page
    [recipe]
	(layout/common "Recept" [css-recipe]
        (hf/form-to {:enctype "multipart/form-data"}
    		[:post "/update-recipe"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :recipe-id (:_id recipe))
        	(hf/hidden-field :num-items (+ (count (:items recipe)) 10))
        	[:table.master-table
        		[:tr
        			[:td.rec-buttons-td [:a.button.button1 {:href "/"} "Home"]]
        			[:td.rec-buttons-td (hf/submit-button {:class "button button1"} "Updatera!")]
	        		[:td.rec-buttons-td (hf/reset-button {:class "button button1"} "Ta bort!")]]
	        	[:tr [:td.btn-spacer ""]]]
	        [:table
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
	        		[:td [:a.link-thin {:href (:url recipe)} "GO"]]]
	        	[:tr [:td.btn-spacer ""]]]
	        [:div.rec-area-div (hf/text-area {:class "recipe-item recipe-area"}
	        			 			   :recipe-area
	        			 			   (:text recipe))])))

;;-----------------------------------------------------------------------------

(defn edit-recipe
    [recipe-id]
	(show-recipe-page (db/get-recipe recipe-id)))

;;-----------------------------------------------------------------------------

(defn new-recipe
    []
	(show-recipe-page {
		:tags #{"Recept"},
   		:_id (db/mk-id),
   		:entryname "",
   		:added (common/now-str),
   		:url "",
   		:items [],
   		:text ""}))

;;-----------------------------------------------------------------------------

(defn get-r-item
	[params i]
	(when (some? (get params (mk-tag "recipe-item-name-" i)))
		{:text   (get params (mk-tag "recipe-item-name-" i))
		 :unit   (get params (mk-tag "recipe-item-unit-" i))
		 :amount (get params (mk-tag "recipe-item-amount-" i))}))

(defn get-r-items
	[params]
	(vec (remove nil? (map #(get-r-item params %) (range (Integer/valueOf (:num-items params)))))))

(defn update-recipe!
	[{params :params}]
	(db/update-recipe {:_id   (:recipe-id params)
					   :url   (:recipe-url params)
					   :entryname (:recipe-name params)
					   :text  (:recipe-area params)
					   :items (get-r-items params)})
	(:recipe-id params))

;;-----------------------------------------------------------------------------
