(ns shop2.views.admin.items
  	(:require 	(shop2 		 [db           :as db]
  							 [utils        :as utils])
            	(shop2.views [layout       :as layout]
            				 [common       :as common]
            				 [css          :refer :all])
            	(shop2.db 	[tags 			:as dbtags]
  							[items			:as dbitems]
  							[lists 			:as dblists]
  							[menus 			:as dbmenus]
  							[projects 		:as dbprojects]
  							[recipes 		:as dbrecipes])
            	(clj-time 	[core            :as t]
            				[local           :as l]
            				[coerce          :as c]
            				[format          :as f]
            				[periodic        :as p])
            	(garden 	[core       :as g]
            				[units      :as u]
            				[selectors  :as sel]
            				[stylesheet :as ss]
            				[color      :as color]
            				[arithmetic        :as ga])
            	(hiccup 	[core              :as h]
            				[def               :as hd]
            				[element           :as he]
            				[form              :as hf]
            				[page              :as hp]
            				[util              :as hu])
            	(ring.util 	[anti-forgery :as ruaf]
            				[response     :as ring])
            	(clojure 	[string           :as str]
            				[set              :as set])))

;;-----------------------------------------------------------------------------

(defn mk-parent-dd
	[item]
	(let [lists      (dblists/get-list-names)
		  list-names (sort (map :entryname lists))
		  tl-name    (some #(when (= (:_id %) (:parent item)) (:entryname %)) lists)]
		(hf/drop-down {:class "new-item-txt"}
	    					:parent list-names tl-name)))

(defn info-part
	[item]
	(common/named-div "Information"
		[:table.group
			[:tr
				[:td.info-head (hf/label :xx "Parent")]
				[:td (mk-parent-dd item)]]
	    	[:tr
				[:td.info-head (hf/label :xx "Namn")]
				[:td (hf/text-field {:class "new-item-txt"} :entryname (:entryname item))]]
			[:tr
				[:td.info-head (hf/label :xx "Enhet")]
				[:td (hf/text-field {:class "new-item-txt"} :unit (:unit item))]]
			[:tr
				[:td.info-head (hf/label :xx "Mängd")]
				[:td (hf/text-field {:class "new-item-txt"} :amount (:amount item))]]
			[:tr
				[:td.info-head (hf/label :xx "Pris")]
				[:td (hf/text-field {:class "new-item-txt"} :price (:price item))]]
			[:tr
				[:td.info-head (hf/label :xx "URL")]
				[:td.url-td (hf/text-field {:class "new-item-txt"} :url (:url item))]]
		]))

;;-----------------------------------------------------------------------------

(defn extract-id
	[params]
	(if (or (str/blank? (:_id params))
			(not (dbitems/item-id-exists? (:_id params))))
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
											 (dblists/get-list-names))]
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
			(dbtags/add-tag-names)
		))

(defn update-item
	[{params :params}]
	(dbitems/update-item {:_id       (extract-id params)
					 :entryname (extract-name params)
					 :parent    (extract-parent params)
					 :unit      (extract-str :unit params)
					 :amount    (extract-num :amount params)
					 :price     (extract-num :price params)
					 :url       (extract-str :url params)
					 :tags      (common/extract-tags params)})
	(ring/redirect (str "/admin/item/" (extract-id params))))

(defn delete-item
	[request item-id]
	(dbitems/delete-item item-id)
	(ring/redirect "/admin/home"))

;;-----------------------------------------------------------------------------

(defn edit-item
	[request item-id]
	(let [item (dbitems/get-item item-id)]
		(layout/common "Edit item" [css-tags-tbl]
			(hf/form-to
	    		[:post "/admin/edit-item"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :_id (:_id item))
	        	[:div
	    			(common/homeback-button "/admin")
	    			[:a.link-head {:href (str "/admin/delete-item/" item-id)} "Ta bort"]
	    			(hf/submit-button {:class "button button1"} "Uppdatera")]
		        [:div
		        	(info-part item)
		        	(common/new-tags-tbl)
			    	(common/old-tags-tbl)]
			    ))))

;;-----------------------------------------------------------------------------

(defn new-item
	[request]
	(layout/common "Skapa ny sak" [css-tags-tbl]
		(hf/form-to
    		[:post "/admin/new-item"]
        	(ruaf/anti-forgery-field)
        	[:div
    			(common/homeback-button "/admin")
    			[:a.link-head (hf/submit-button {:class "button button1"} "Skapa")]]
	        [:div
	        	(info-part nil)
	        	(common/new-tags-tbl)
		    	(common/old-tags-tbl)
		    	])))

;;-----------------------------------------------------------------------------
