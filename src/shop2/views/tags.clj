(ns shop2.views.tags
  	(:require 	(shop2 			[db            	:as db])
            	(shop2.views 	[layout     	:as layout]
            				 	[common     	:as common]
            					[home       	:as home]
            					[css 			:refer :all])
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

(defn edit-tag-page
	[request tag-id]
	(let [tag (dbtags/get-tag tag-id)]
		(layout/common "Edit tag" [css-tags css-tags-tbl]
			(hf/form-to
	    		[:post "/update-tag"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :_id (:_id tag))
	        	[:table
	        		[:tr
	        			[:td {:colspan 2}
	        				[:div
				    			(common/home-button)
				    			[:a.link-flex {:href (str "/admin/delete-tag/" tag-id)} "Ta bort"]
				    			[:a.link-flex {:href (str "/admin/delete-tag-all/" tag-id)} "Rensa"]
				    			[:a.link-flex (hf/submit-button {:class "button button1"} "Uppdatera")]]]]
		        	[:tr
		        		[:td {:style "padding: 40px 25px; width: 50px"}
		        			(hf/label :xx "Namn")]
		        		[:td
	    					(hf/text-field {:class "new-item-txt"}
	    							:entryname (:entryname tag))]]]))))

(defn update-tag!
	[request {params :params}]
	(when (and (seq (:_id params)) (seq (:entryname params)))
		(dbtags/update-tag (:_id params) (:entryname params)))
	(ring/redirect (str "/admin/tag/" (:_id params))))

(defn delete-tag!
	[request tag-id]
	(dbtags/delete-tag tag-id)
	(ring/redirect "/user/home"))

(defn delete-tag-all!
	[request tag-id]
	(dbtags/delete-tag-all tag-id)
	(ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------
