(ns shop2.views.recipe
  	(:require 	(shop2 			[db         :as db]
  								[utils      :as utils])
            	(shop2.views 	[layout     :as layout]
            					[common     :as common])
          		(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(garden 		[core       :as g]
            					[units      :as u]
            					[selectors  :as sel]
            					[stylesheet :as ss]
            					[color      :as color])
            	(clj-time 		[core       :as t]
            					[local      :as l]
            					[format     :as f]
            					[coerce 	:as c]
            					[periodic   :as p])
            	(hiccup 		[core       :as h]
            					[def        :as hd]
            					[element    :as he]
            					[form       :as hf]
            					[page       :as hp]
            					[util       :as hu])
            	(ring.util 		[anti-forgery :as ruaf]
            					[response     :as ring])
              	(clojure 		[string     :as str]
            					[set        :as set])))

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
			:width layout/full
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
		[:.recipe-table {
			:border-collapse :collapse
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
			:width layout/full
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
		 (range (+ (count (:items recipe)) 5))))

(defn show-recipe-page
    [recipe]
	(layout/common "Recept" [css-recipe]
        (hf/form-to {:enctype "multipart/form-data"}
    		[:post (if (nil? recipe) "/create-recipe" "/update-recipe")]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :recipe-id (:_id recipe))
        	(hf/hidden-field :num-items (+ (count (:items recipe)) 5))
        	[:div
        		(common/home-button)
        		(hf/submit-button {:class "button button1"}
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
    [recipe-id]
	(show-recipe-page (dbrecipes/get-recipe recipe-id)))

(defn new-recipe
    []
	(show-recipe-page nil))

;;-----------------------------------------------------------------------------

(defn assoc-if
	([k v]
	(if (or (nil? v) (and (string? v) (str/blank? v)))
		{}
		(hash-map k v)))
	([k v m]
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
	(dbrecipes/update-recipe (->> (assoc-if :_id (:recipe-id params))
						   (assoc-if :url (:recipe-url params))
						   (assoc-if :entryname (:recipe-name params))
						   (assoc-if :text  (:recipe-area params))
						   (assoc-if :items (get-r-items params))))
	(ring/redirect (str "/recipe/" (:recipe-id params))))

(defn create-recipe!
	[{params :params}]
	(let [ret (dbrecipes/add-recipe (->> (assoc-if :url (:recipe-url params))
								  (assoc-if :entryname (:recipe-name params))
								  (assoc-if :text  (:recipe-area params))
								  (assoc-if :items (get-r-items params))))]
		(ring/redirect (str "/recipe/" (:_id ret)))))

;;-----------------------------------------------------------------------------
