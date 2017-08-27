(ns shop2.views.items
  	(:require 	(shop2 			[db         	:as db]
  								[utils      	:as utils])
            	(shop2.views 	[layout     	:as layout]
            				 	[common     	:as common]
            					[home       	:as home]
            					[css        	:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(clj-time       [core       	:as t]
            	                [local      	:as l]
            	                [format     	:as f]
            	                [periodic   	:as p])
            	(garden			[core       	:as g]
            					[units      	:as u]
            					[selectors  	:as sel]
            					[stylesheet 	:as ss]
            					[color      	:as color])
            	(hiccup			[core       	:as h]
            					[def        	:as hd]
            					[element    	:as he]
            					[form       	:as hf]
            					[page       	:as hp]
            					[util       	:as hu])
            	(ring.util 		[response   	:as ring]
              					[anti-forgery 	:as ruaf])
            	(clojure.spec 	[alpha          :as s])
             	(clojure 		[string       	:as str]
            					[set          	:as set])))

;;-----------------------------------------------------------------------------

(defn get-parents
	[a-list]
	(loop [parent (:parent a-list)
		   acc    #{(:_id a-list)}]
		(if (nil? parent)
			acc
			(recur (:parent parent) (conj acc (:_id parent))))))

(defn get-items
	[a-list]
	(let [items        (dbitems/get-items)
		  id-parents   (get-parents a-list)
    	  active-items (->> a-list
                            :items
                            (remove #(some? (:finished %)))
                            (map :_id)
                            set)]
		(->> items
       		 (filter #(contains? id-parents (:parent %)))
          	 (remove #(contains? active-items (:_id %))))))

(defn mk-add-item
	[a-list item]
	[:div.item-div
		[:table.item-table
			[:tr
				[:td.item-txt-td
					[:a.item-txt-a
						{:href (str "/user/add-to-list/" (:_id a-list) "/" (:_id item))}
						[:div.item-txt (:entryname item)]]]
				[:td.item-tags-td
					(hf/label {:class "item-tags"}
						:x (some->> item
									:tags
									common/frmt-tags))]]]])

(defn mk-tags-item
	[a-list item]
	[:a.item-an
		{:href (str "/user/add-to-list/" (:_id a-list) "/" (:_id item))}
		(hf/label {:class "item-txt"} :x (:entryname item))])

(defn old-items-tags
	[a-list items-by-tag]
	(for [kv items-by-tag
		  :let [tag (key kv)
		  		items (sort-by #(str/lower-case (:entryname %)) (val kv))]]
		(list
			[:table {:style "width:100%;table-layout:fixed"}
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
			(old-items-tags a-list
                   			(->> (get-items a-list)
								 (map #(update-in % [:tags] stringify))
								 (group-by :tags)
								 (into (sorted-map))))
		(= sort-type :tags-z-a)
			(old-items-tags a-list
                   			(->> (get-items a-list)
								 (map #(update-in % [:tags] stringify))
								 (group-by :tags)
								 (into (sorted-map-by (fn [k1 k2] (compare k2 k1))))))
		(= sort-type :z-a)
			(old-items-name a-list
                   			(->> (get-items a-list)
								 (sort-by #(str/lower-case (:entryname %)))
								 reverse))
		:else
			(old-items-name a-list
                   			(->> (get-items a-list)
								 (sort-by #(str/lower-case (:entryname %)))))))

(defn add-items-page
	[request list-id sort-type]
	(let [a-list (dblists/get-list list-id)]
		(layout/common request "Välj sak" [css-items css-tags-tbl]
	        [:div
    			(common/homeback-button (str "/user/list/" list-id))
	    		[:a.link-head {:href (str "/user/mk-new-item/" list-id)} "Ny"]]
	        [:div.sort-div
	        	[:a.r-space.link-thin {:href (str "/user/add-items/" list-id "/a-z")} "A-Z"]
        		[:a.lr-space.link-thin {:href (str "/user/add-items/" list-id "/z-a")} "Z-A"]
        		[:a.lr-space.link-thin {:href (str "/user/add-items/" list-id "/tags-a-z")} "Tags A-Z"]
        		[:a.l-space.link-thin {:href (str "/user/add-items/" list-id "/tags-z-a")} "Tags Z-A"]]
        	[:div
        		(old-items a-list sort-type)])))

;;-----------------------------------------------------------------------------

(defn add-item-page
	[request list-id item-id]
	(dblists/item->list list-id item-id 1)
	(ring/redirect (str "/user/add-items/" list-id)))

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
	[request list-id]
	(layout/common request "Skapa ny sak" [css-items css-tags-tbl]
		(hf/form-to
    		[:post "/user/new-item"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :list-id list-id)
	        [:div
    			(common/homeback-button (str "/user/add-items/" list-id))
    			[:a.link-head {:href (str "/user/list/" list-id)}
    					(:entryname (dblists/get-list list-id))]
    			[:a.link-head (hf/submit-button {:class "button button1"} "Skapa")]]
	        [:div
	        	(info-part)
	        	(common/new-tags-tbl)
		    	(common/old-tags-tbl)
		    	])))

;;-----------------------------------------------------------------------------

(defn new-item!
	[{params :params :as request}]
	(try
		(let [tags (common/extract-tags params)
			  new-item (dbitems/add-item
			  			(-> {:entryname (s/assert :shop/string (:new-item-name params))
							 :parent (:list-id params)}
							(utils/assoc-str-if :tags   tags)
							(utils/assoc-num-if :amount (:new-item-amount params))
							(utils/assoc-str-if :unit   (:new-item-unit params))
							(utils/assoc-str-if :url    (:new-item-url params))
							(utils/assoc-num-if :price  (:new-item-price params))))]
			(dblists/item->list (:list-id params) (:_id new-item) 1)
			(ring/redirect (str "/user/add-items/" (:list-id params))))
		(catch AssertionError ae
			(mk-new-item-page (assoc request :err-msg (str "ASSERT: " (.getMessage ae)))
							  (:list-id params)))
		(catch Exception e
			(mk-new-item-page (assoc request :err-msg (str "Exception: " (.getMessage e)))
							  (:list-id params)))))

;;-----------------------------------------------------------------------------

