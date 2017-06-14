(ns shop2.views.admin.lists
  	(:require 	(shop2 			[db                 :as db]
  								[utils    :as utils])
            	(shop2.views 	[layout       :as layout]
            					[common       :as common]
            					[home         :as home]
            				 	[css          :refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[hiccup.core              :as h]
            	[hiccup.def               :as hd]
            	[hiccup.element           :as he]
            	[hiccup.form              :as hf]
            	[hiccup.page              :as hp]
            	[hiccup.util              :as hu]
            	[garden.core              :as g]
            	[garden.units             :as u]
            	[garden.selectors         :as sel]
            	[garden.stylesheet        :as ss]
            	[garden.color             :as color]
            	(ring.util 		[anti-forgery :as ruaf]
            					[response     :as ring])
              	[clojure.string           :as str]
            	[clojure.set              :as set]))

;;-----------------------------------------------------------------------------

(defn new-list-page
    [request]
    (layout/common "Skapa ny lista" [] ; css-lists-new
        (hf/form-to
        	[:post "/admin/new-list"]
        	(ruaf/anti-forgery-field)
    		[:div
    			(common/home-button)
    			(hf/submit-button {:class "button button1"} "Skapa")]
            (common/named-div "Listans namn:"
            	(hf/text-field {:class "new-name"} :entryname))
        	(common/named-div "Överornad lista:"
        		(hf/drop-down {:class "new-parent"}
        					:list-parent
        					(conj (map :entryname (dblists/get-list-names)) common/top-lvl-name)))
            )))

(defn mk-parent-map
	[params]
	(when-not (or (str/blank? (:list-parent params))
				  (= (:list-parent params) common/top-lvl-name))
		(let [p-list (dblists/find-list-by-name (:list-parent params))]
			(select-keys p-list [:_id :entryname :parent]))))

(defn added-list!
	[{params :params}]
	(if (seq (:entryname params))
		(dblists/add-list {:entryname (:entryname params)
					  :parent (mk-parent-map params)})
		(throw (Exception. "list name is blank")))
	(ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

(defn edit-list-page
    [request list-id]
    (let [a-list (dblists/get-list list-id)]
    	(layout/common "Ändra lista" [] ; css-lists-new
	        (hf/form-to
	        	[:post "/admin/edit-list"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :list-id list-id)
	    		[:div
	    			(common/home-button)
	    			[:a.link-head {:href (str "/admin/delete-list/" list-id)} "Ta bort"]
	    			(hf/submit-button {:class "button button1"} "Uppdatera")]
	            (common/named-div "Listans namn:"
	            	(hf/text-field {:class "new-name"} :entryname (:entryname a-list)))
	        	(common/named-div "Överornad lista:"
	        		(hf/drop-down {:class "new-parent"}
	        					:list-parent
	        					(conj (map :entryname (dblists/get-list-names)) common/top-lvl-name)
	        					(some->> a-list :parent :entryname)))
            ))))

(defn edit-list!
	[{params :params}]
	(if (seq (:entryname params))
		(dblists/update-list {:_id (:list-id params)
						 :entryname (:entryname params)
						 :parent (mk-parent-map params)})
		(throw (Exception. "list name is blank")))
	(ring/redirect "/admin"))

(defn delete-list!
	[request list-id]
	(dblists/delete-list list-id)
	(ring/redirect "/admin"))

;;-----------------------------------------------------------------------------

