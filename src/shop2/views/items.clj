(ns shop2.views.items
  	(:require 	[shop2.db                   :as db]
            	(shop2.views 	[layout     :as layout]
            				 	[common     :as common]
            					[home       :as home])
            	(clj-time       [core       :as t]
            	                [local      :as l]
            	                [format     :as f]
            	                [periodic   :as p])
            	(garden			[core       :as g]
            					[units      :as u]
            					[selectors  :as sel]
            					[stylesheet :as ss]
            					[color      :as color])
            	(hiccup			[core       :as h]
            					[def        :as hd]
            					[element    :as he]
            					[form       :as hf]
            					[page       :as hp]
            					[util       :as hu])
            	(ring.util 		[response   :as ring]
              					[anti-forgery :as ruaf])
            	[clojure.string           :as str]
            	[clojure.set              :as set]))

;;-----------------------------------------------------------------------------

(def css-items
	(g/css
		[:.group {
			:border-collapse :collapse
			:border        [[(u/px 1) :white :solid]]
			:border-radius (u/px 8)
			:margin        [[(u/px 50) 0 (u/px 20) 0]]
			:padding       (u/px 8)
		}]
		[:.head-td {:text-align :center}]
		[:.group-head-th {
			:padding   (u/px 10)
			:font-size (u/px 36)
		}]
		[:.item-tr:first-child {
		}]
		[:.item-tr {
			:border-color        :grey
			:border-top-style :solid
		}]
		[:.item-td {
			:padding (u/px 6)
			:width   layout/half
		}]
		[:.item-an {
			:display         :block
			:color           :white
			:text-decoration :none
		}]
		[:.item-at {
			:display         :block
			:color           :lightgrey
			:text-decoration :none
		}]
		[:.inner {
			:width (u/percent 95)
		}]
		[:.new-item-txt {
			:font-size (u/px 24)
			}]
		))

;;-----------------------------------------------------------------------------

(defn mk-add-item
	[a-list item]
	[:tr.item-tr
		[:td.item-td
			[:a.item-an
				{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
				(:entryname item)]]
		[:td.item-td
			[:a.item-at
				{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
				(some->> item :tags (common/filter-tags (:tags a-list)) common/frmt-tags)]]])

(defn info-part
	[]
	[:table.master-table
		[:tr
			[:th.cat-choice-th {:colspan 2}
				(hf/label {:class "cat-choice"} :xxx "Information om grejen")]]
			[:tr
			[:td (hf/label {:class "new-item-lbl"} :new-name "Namn:")]
			[:td (hf/text-field {:class "new-item-txt"} "new-item-name")]]
		[:tr
				[:td (hf/label {:class "new-item-lbl"} :new-name "Enhet:")]
			[:td (hf/text-field {:class "new-item-txt"} "new-item-unit")]]
		[:tr
				[:td (hf/label {:class "new-item-lbl"} :new-name "Mängd:")]
			[:td (hf/text-field {:class "new-item-txt"} "new-item-amount")]]
		[:tr
				[:td (hf/label {:class "new-item-lbl"} :new-name "Pris:")]
			[:td (hf/text-field {:class "new-item-txt"} "new-item-price")]]
		[:tr
				[:td {:colspan 2} (hf/submit-button {:class "button button1"} "Skapa")]]])

(defn add-items-page
	[list-id]
	(let [a-list (db/get-list list-id)]
		(layout/common "New items" [css-items common/css-tags-tbl]
			(hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/new-item"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :list-id list-id)
		        [:table.master-table
		            [:tr
		            	[:td.head-td
	    					[:a.link-head {:href "/"} "Home"]]
	    				[:td.head-td
	    					[:a.link-head {:href (str "/list/" list-id)}
	    					(:entryname a-list)]]]]
		        [:table.master-table.group
	    			[:tr
	    				[:th.group-head-th {:colspan 2}
	    					(hf/label :xxx "Välj en existerande")]]
	        		(map #(mk-add-item a-list %) (db/get-items))]
			    [:table.master-table.group
			        [:tr
			        	[:th.group-head-th "Skapa en ny"]]
			    	[:tr [:td.inner (common/old-tags-tbl)]]
			    	[:tr [:td.inner (common/new-tags-tbl)]]
			    	[:tr [:td.inner (info-part)]]]))))

;;-----------------------------------------------------------------------------

(defn edit-item-page
	[item-id]
	(let [item (db/get-item item-id)]
		(layout/common "Edit item" [css-items common/css-tags-tbl]
			(hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/update-item"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :_id (:_id item))
	        	[:table.master-table
		            [:tr
		            	[:td.head-td
	    					[:a.link-head {:href "/"} "Home"]]
	    				[:td.head-td
	    					[:a.link-head {:href (str "/delete-item/" item-id)} "Ta bort"]]
	    				[:td.head-td
	    					[:a.link-head (hf/submit-button {:class "button button1"} "Uppdatera")]]]]
		        [:table.master-table.group
	    			[:tr
	    				[:td (hf/label :xx "Namn")]
	    				[:td (hf/text-field {:class "new-item-txt"} :entryname (:entryname item))]]
	        		[:tr
	    				[:td (hf/label :xx "Enhet")]
	    				[:td (hf/text-field {:class "new-item-txt"} :unit (:unit item))]]
	        		[:tr
	    				[:td (hf/label :xx "Mängd")]
	    				[:td (hf/text-field {:class "new-item-txt"} :amount (:amount item))]]
	        		[:tr
	    				[:td (hf/label :xx "Pris")]
	    				[:td (hf/text-field {:class "new-item-txt"} :price (:price item))]]
	        		[:tr
	    				[:td (hf/label :xx "Kategorier")]
	    				[:td (hf/text-field {:class "new-item-txt"} :tags
	    					(str/join ", " (map :entryname (:tags item))))]]]
			    ))))

(defn mk-num
	[v t]
	(try
		(cond
			(= t :BigDecimal) (BigDecimal. v)
			(= t :Integer)    (Integer/valueOf v)
			(= t :Double)     (Double/valueOf v)
			)
		(catch Exception e nil)))

(defn isneg?
	[v]
	(and (some? v) (number? v) (neg? v)))

(defn parse-params
	[params config]
	(into {} (for [pkey (keys config)
				  :let [ptype  (first (get config pkey))
				  		pextra (second (get config pkey))
				  		pvalue (get params pkey)]
				  :when (not (str/blank? pvalue))]
		(do
			(cond
				(= ptype :string)    	(hash-map pkey pvalue)
				(= ptype :date-time) 	(if (and (nil? (f/parse pvalue)) (= pextra :must))
											(throw (Exception. (str pkey " can not be blank")))
											(hash-map pkey (f/parse pvalue)))
				(= ptype :pos-int)     	(if (isneg? (mk-num pvalue :Integer))
											(throw (Exception. (str pkey " is invalid")))
											(hash-map pkey (mk-num pvalue :Integer)))
				(= ptype :pos-decimal) 	(if (isneg? (mk-num pvalue :BigDecimal))
											(throw (Exception. (str pkey " is invalid")))
											(hash-map pkey (mk-num pvalue :BigDecimal)))
				(= ptype :tag-list)    	(hash-map pkey (map #(db/add-tag %)
															(some-> pvalue
																	(str/split #"(,| )+")
																	set vec)))
			)))))

(defn update-item
	[{params :params}]
	(let [input (parse-params params {
					:_id       [:string :must]
					:entryname [:string :must]
					:unit      [:string]
					:amount    [:pos-decimal]
					:price     [:pos-decimal]
					:tags      [:tag-list]})]
		(db/update-item input)
		(ring/redirect (str "/item/" (:_id input)))))

(defn delete-item
	[item-id]
	(db/delete-item item-id))

;;-----------------------------------------------------------------------------

(defn add-item-page
	[list-id item-id]
	(db/item->list list-id item-id 1)
	list-id)

;;-----------------------------------------------------------------------------

(defn assoc-if
	[m k txt]
	(if (seq txt)
		(assoc m k txt)
		m))

(defn assoc-num-if
	[m k txt]
	(if-let [n (some->> txt (re-matches #"\d+(\.\d+)?") first BigDecimal.)]
		(assoc m k n)
		m))

(defn new-item!
	[{params :params}]
	(let [target-list (db/get-list (:list-id params))
		  itemname    (:new-item-name params)
		  tags        (common/extract-tags params)]
		(if (and (some? target-list) (seq itemname) (seq tags))
			(let [new-item (db/add-item (-> {:entryname itemname :tags tags}
							     			(assoc-num-if :amount (:new-item-amount params))
							     			(assoc-if     :unit (:new-item-unit params))
							     			(assoc-num-if :price (:new-item-price params))))]
				(db/item->list (:list-id params) (:_id new-item) 1)
				(:list-id params))
			(throw (Exception. (str
				"Failed to add item"
				" Target " (:list-id params) (if target-list " is OK" " is unknown")
				" Itemname " (:new-item-name params) (if (seq itemname) " is OK" " is unknown")
				" Tags " (prn-str tags) (if (seq tags) " is OK" " is unknown")
				" WTF!"))))))

;;-----------------------------------------------------------------------------
