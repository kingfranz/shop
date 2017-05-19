(ns shop2.views.tags
  	(:require 	[shop2.db                   	:as db]
            	(shop2.views 	[layout     	:as layout]
            				 	[common     	:as common]
            					[home       	:as home])
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
            	(clojure 		[string       	:as str]
            					[set          	:as set])))

;;-----------------------------------------------------------------------------

(def css-tags
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
		[:.inner {
			:width (u/percent 95)
		}]
		[:.new-item-txt {
			:font-size (u/px 24)
			}]
		))

;;-----------------------------------------------------------------------------

(defn edit-tag-page
	[tag-id]
	(let [tag (dbtags/get-tag tag-id)]
		(layout/common "Edit tag" [css-tags common/css-tags-tbl]
			(hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/update-tag"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :_id (:_id tag))
	        	[:table
	        		[:tr
	        			[:td {:colspan 2}
	        				[:div
				    			(common/home-button)
				    			[:a.link-flex {:href (str "/delete-tag/" tag-id)} "Ta bort"]
				    			[:a.link-flex {:href (str "/delete-tag-all/" tag-id)} "Rensa"]
				    			[:a.link-flex (hf/submit-button {:class "button button1"} "Uppdatera")]]]]
		        	[:tr
		        		[:td {:style "padding: 40px 25px; width: 50px"}
		        			(hf/label :xx "Namn")]
		        		[:td
	    					(hf/text-field {:class "new-item-txt"}
	    							:entryname (:entryname tag))]]]))))

(defn update-tag!
	[{params :params}]
	(when (and (seq (:_id params)) (seq (:entryname params)))
		(dbtags/update-tag (:_id params) (:entryname params)))
	(ring/redirect (str "/tag/" (:_id params))))

(defn delete-tag!
	[tag-id]
	(dbtags/delete-tag tag-id)
	(ring/redirect "/"))

(defn delete-tag-all!
	[tag-id]
	(dbtags/delete-tag-all tag-id)
	(ring/redirect "/"))

;;-----------------------------------------------------------------------------
