(ns shop2.views.items
  	(:require 	(shop2 			[db         :as db]
  								[utils      :as utils])
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
		[:.item-div {
			:float :left
		}]
		[:.item-txt-td {
			:width (u/px 250)
		}]
		[:.item-txt {
			:text-align :right
			:white-space :nowrap
		}]
		[:.item-tags-td {
			:width (u/px 200)
		}]
		[:.item-tags {
			:text-align :left
			:margin   [[(u/px 5) (u/px 5) (u/px 0) (u/px 5)]]
			:font-size (u/px 16)
			:white-space :nowrap
		}]
		[:.sort-div {
			:margin [[(u/px 10) 0 (u/px 10) 0]]
		}]
		[:a.r-space {
			:margin [[0 (u/px 5) 0 0]]
		}]
		[:a.lr-space {
			:margin [[0 (u/px 5) 0 (u/px 5)]]
		}]
		[:a.l-space {
			:margin [[0 0 0 (u/px 5)]]
		}]
		[:.url-td {
			:width (u/px 500)
		}]
		[:.new-item-txt {
			:font-size (u/px 24)
			:width layout/full
		}]
		[:.tags-head {
			:font-size (u/px 24)
			:background-color     (layout/grey% 30)
 			:color                :white
			:width layout/full
		}]
	))

;;-----------------------------------------------------------------------------

(defn get-parents
	[a-list]
	(loop [parent (:parent a-list)
		   acc    #{}]
		(if (nil? parent)
			acc
			(recur (:parent parent) (conj acc (:_id parent))))))

(defn get-items
	[a-list]
	(let [items      (db/get-items)
		  id-parents (get-parents a-list)]
		filter #(contains? (:parent %) id-parents) items))

(defn mk-add-item
	[a-list item]
	[:div.item-div
		[:table
			[:tr
				[:td.item-txt-td
					[:a.item-an
						{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
						(hf/label {:class "item-txt"} :x (:entryname item))]]
				[:td.item-tags-td
					(hf/label {:class "item-tags"}
						:x (some->> item
									:tags
									(common/filter-tags (:tags a-list))
									common/frmt-tags))]]]])

(defn mk-tags-item
	[a-list item]
	[:a.item-an
		{:href (str "/add-to-list/" (:_id a-list) "/" (:_id item))}
		(hf/label {:class "item-txt"} :x (:entryname item))])

(defn old-items-tags
	[a-list items-by-tag]
	(for [kv items-by-tag
		  :let [tag (key kv)
		  		items (sort-by #(str/lower-case (:entryname %)) (val kv))]]
		(list
			[:table {:style "width: 100%"}
				[:tr
					[:td.tags-head {:style "width: 100%"} (hf/label {:class "tags-head"} :x tag)]]
				[:tr
					[:td (map #(mk-add-item a-list %) items)]]])))

(defn old-items-name
	[a-list items]
	(map #(mk-add-item a-list %) items))

(defn stringify
	[tags]
	(str/join " " (sort (map :entryname tags))))

(defn old-items
	[a-list sort-type]
	(cond
		(= sort-type :tags-a-z)
			(old-items-tags a-list (->> (get-items a-list)
								 (map #(update-in % [:tags] stringify))
								 (group-by :tags)
								 (into (sorted-map))))
		(= sort-type :tags-z-a)
			(old-items-tags a-list (->> (get-items a-list)
								 (map #(update-in % [:tags] stringify))
								 (group-by :tags)
								 (into (sorted-map-by (fn [k1 k2] (compare k2 k1))))))
		(= sort-type :z-a)
			(old-items-name a-list (->> (get-items a-list)
								 (sort-by #(str/lower-case (:entryname %)))
								 reverse))
		:else
			(old-items-name a-list (->> (get-items a-list)
								 (sort-by #(str/lower-case (:entryname %)))))))

(defn add-items-page
	[list-id sort-type]
	(let [a-list (db/get-list list-id)]
		(layout/common "Välj sak" [css-items common/css-tags-tbl]
	        [:div
    			(common/homeback-button (str "/list/" list-id))
	    		[:a.link-head {:href (str "/mk-new-item/" list-id)} "Ny"]]
	        [:div.sort-div
	        	[:a.r-space.link-thin {:href (str "/add-items/" list-id "/a-z")} "A-Z"]
        		[:a.lr-space.link-thin {:href (str "/add-items/" list-id "/z-a")} "Z-A"]
        		[:a.lr-space.link-thin {:href (str "/add-items/" list-id "/tags-a-z")} "Tags A-Z"]
        		[:a.l-space.link-thin {:href (str "/add-items/" list-id "/tags-z-a")} "Tags Z-A"]]
        	[:div
        		(old-items a-list sort-type)])))

;;-----------------------------------------------------------------------------

(defn info-part
	[]
	(common/named-div "Information"
		[:table
			[:tr
				[:td.new-item-td "Namn:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-name")]]
			[:tr
				[:td.new-item-td "Enhet:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-unit")]]
			[:tr
				[:td.new-item-td "Mängd:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-amount")]]
			[:tr
				[:td.new-item-td "Pris:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-price")]]
			[:tr
				[:td.new-item-td "URL:"]
				[:td.url-td (hf/text-field {:class "new-item-txt"} "new-item-url")]]]))

(defn mk-new-item-page
	[list-id]
	(let [a-list (db/get-list list-id)]
		(layout/common "Skapa ny sak" [css-items common/css-tags-tbl]
			(hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/new-item"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :list-id list-id)
		        [:div
	    			(common/homeback-button (str "/add-items/" list-id))
	    			[:a.link-head {:href (str "/list/" list-id)} (:entryname a-list)]
	    			[:a.link-head (hf/submit-button {:class "button button1"} "Skapa")]]
		        [:div
		        	(info-part)
		        	(common/new-tags-tbl)
			    	(common/old-tags-tbl)
			    	]))))

;;-----------------------------------------------------------------------------

(defn edit-item-page
	[item-id]
	(let [item (db/get-item item-id)
		  lists (db/get-list-names)
		  list-names (sort (map :entryname lists))
		  tl-name (some #(when (= (:_id %) (:parent item)) (:entryname %)) lists)]
		(prn tl-name list-names)
		(layout/common "Edit item" [css-items common/css-tags-tbl]
			(hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/update-item"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :_id (:_id item))
	        	[:div
	    			(common/home-button)
	    			[:a.link-head {:href (str "/delete-item/" item-id)} "Ta bort"]
	    			(hf/submit-button {:class "button button1"} "Uppdatera")]
		        [:table.group
	    			[:tr
	    				[:td (hf/label :xx "Parent")]
	    				[:td (hf/drop-down {:class "new-item-txt"}
	    					:parent list-names tl-name)]]
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
	    				[:td (hf/label :xx "URL")]
	    				[:td (hf/text-field {:class "new-item-txt"} :url (:url item))]]
	        		[:tr
	    				[:td (hf/label :xx "Kategorier")]
	    				[:td (hf/text-field {:class "new-item-txt"} :tags
	    					(str/join ", " (map :entryname (:tags item))))]]]
			    ))))

(defn extract-id
	[params]
	(if (or (str/blank? (:_id params))
			(not (db/item-id-exists? (:_id params))))
		(throw (ex-info "invalid id" {:cause :_id}))
		(:_id params)))

(defn extract-name
	[params]
	(if (str/blank? (:entryname params))
		(throw (ex-info "invalid name" {:cause :entryname}))
		(:entryname params)))

(defn extract-parent
	[params]
	(if (str/blank? (:parent params))
		(throw (ex-info "invalid name" {:cause :entryname}))
		(if (= (:parent params) common/top-lvl-name)
			nil
			(if-let [found (utils/find-first #(= (:entryname %) (:parent params))
											 (db/get-list-names))]
				(:_id found)
				(throw (ex-info "invalid parent" {:cause :parent}))))))

(defn extract-str
	[tag params]
	(when-not (str/blank? (get params tag))
		(get params tag)))

(defn extract-num
	[tag params]
	(when (extract-str tag params)
		(Double/valueOf (get params tag))))

(defn extract-tags
	[params]
	(some-> (extract-str :tags params)
			(str/split #"(,| )+")
			(db/add-tag-names)
		))

(defn update-item
	[{params :params}]
	(db/update-item {:_id       (extract-id params)
					 :entryname (extract-name params)
					 :parent    (extract-parent params)
					 :unit      (extract-str :unit params)
					 :amount    (extract-num :amount params)
					 :price     (extract-num :price params)
					 :url       (extract-str :url params)
					 :tags      (extract-tags params)})
	(ring/redirect (str "/item/" (extract-id params))))

(defn delete-item
	[item-id]
	(db/delete-item item-id)
	(ring/redirect "/"))

;;-----------------------------------------------------------------------------

(defn add-item-page
	[list-id item-id]
	(db/item->list list-id item-id 1)
	(ring/redirect (str "/add-items/" list-id)))

;;-----------------------------------------------------------------------------

(defn assoc-if
	[m k txt]
	(if (seq txt)
		(assoc m k txt)
		m))

(defn assoc-num-if
	[m k txt]
	(if-let [n (some->> txt (re-matches #"\d+(\.\d+)?") first Double/valueOf)]
		(assoc m k n)
		m))

(defn new-item!
	[{params :params}]
	(let [target-list (db/get-list (:list-id params))
		  itemname    (:new-item-name params)
		  tags        (common/extract-tags params)]
		(if (and (some? target-list) (seq itemname) (seq tags))
			(let [new-item (db/add-item (-> {:entryname itemname
											 :tags tags
											 :parent (:_id target-list)}
							     			(assoc-num-if :amount (:new-item-amount params))
							     			(assoc-if     :unit   (:new-item-unit params))
							     			(assoc-if     :url    (:new-item-url params))
							     			(assoc-num-if :price  (:new-item-price params))))]
				(db/item->list (:list-id params) (:_id new-item) 1)
				(ring/redirect (str "/add-items/" (:list-id params))))
			(throw (Exception. (str
				"Failed to add item, "
				"Target " (:list-id params) (if target-list " is OK" " is unknown") ", "
				"Itemname " (:new-item-name params) (if (seq itemname) " is OK" " is unknown") ", "
				"Tags " (prn-str tags) (if (seq tags) " is OK" " is unknown")))))))

;;-----------------------------------------------------------------------------
