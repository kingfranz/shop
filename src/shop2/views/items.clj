(ns shop2.views.items
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
            	[shop2.views.home         :as home]
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[garden.core              :as g]
            	[garden.units             :as u]
            	[garden.selectors         :as sel]
            	[garden.stylesheet        :as stylesheet]
            	[garden.color             :as color]
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

(defn mk-add-item
	[a-list item]
	[:tr.item-tr
		[:td.item-td
			[:a.item-an
				{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
				(:entry-name item)]]
		[:td.item-td
			[:a.item-at
				{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
				(->> item :tags (common/filter-tags (:tags a-list)) common/frmt-tags)]]])

(defn add-items-page
	[a-list]
	(layout/common "New items" [css-items common/css-tags-tbl]
		(hf/form-to {:enctype "multipart/form-data"}
    		[:post "/new-item"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :list-id (:_id a-list))
	        [:table.master-table
	            [:tr
	            	[:td.head-td
    					[:a.link-head {:href "/"} "Home"]]
    				[:td.head-td
    					[:a.link-head {:href (str "/list/" (:_id a-list))}
    					(:entry-name a-list)]]]]
	        [:table.master-table.group
    			[:tr
    				[:th.group-head-th {:colspan 2}
    					(hf/label {} :xxx "Välj en existerande")]]
        		(map #(mk-add-item a-list %) (db/get-items))]
		    [:table.master-table.group
		        [:tr
		        	[:th.group-head-th "Skapa en ny"]]
		    	[:tr [:td.inner (common/old-tags-tbl)]]
		    	[:tr [:td.inner (common/new-tags-tbl)]]
		    	[:tr [:td.inner
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
		    	 			[:td {:colspan 2} (hf/submit-button {:class "button button1"} "Skapa")]]]]]])))

(defn add-item-page
	[list-id item-id]
	(db/item->list list-id item-id 1)
	(add-items-page (db/get-list list-id)))

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
		  tags        (common/extract-tags params)
		  item-id     (db/mk-id)]
		(if (and (some? target-list) (seq itemname) (seq tags))
			(do
				(db/add-item (-> {:_id item-id :entry-name itemname :tags tags}
							     (assoc-num-if :amount (:new-item-amount params))
							     (assoc-if :unit (:new-item-unit params))
							     (assoc-num-if :price (:new-item-price params))))
				(db/item->list (:list-id params) item-id 1)
				(home/home-page))
			(layout/common "That didn't work" []
				[:h3 "Failed to add item"]
				[:h3 (str "Target " (:list-id params) (if target-list " is OK" " is unknown"))]
				[:h3 (str "Itemname " (:new-item-name params) (if (seq itemname) " is OK" " is unknown"))]
				[:h3 (str "Tags " (prn-str tags) (if (seq tags) " is OK" " is unknown"))]
				[:h1 "WTF!"]))))

;;-----------------------------------------------------------------------------
