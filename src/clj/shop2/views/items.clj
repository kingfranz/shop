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

(defn add-items-page
	[request list-id]
	(let [a-list (dblists/get-list list-id)]
		(layout/common request "Välj sak" [css-items css-tags-tbl]
	        [:div
    			(common/homeback-button (str "/user/list/" list-id))
	    		[:a.link-flex {:href (str "/user/mk-new-item/" list-id)} "Ny"]]
	        [:div#app
          		[:h1 "this is the app"]]
          	[:script {:src "js/client.js"}]
        	[:script {:src (str "window.onload = function () {shop2.core.run(\"" list-id "\");})"}])))

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

