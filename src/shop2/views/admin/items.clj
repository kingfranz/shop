(ns shop2.views.admin.items
  	(:require [shop2.db :refer :all]
              [shop2.extra        	:refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common       	:refer :all]
            				 	[shop2.views.css          	:refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items			:refer :all]
  								[shop2.db.lists 			:refer :all]
  								[shop2.db.menus 			:refer :all]
  								[shop2.db.projects 		:refer :all]
  								[shop2.db.recipes 		:refer :all]
              [utils.core :as utils]
                 [clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 [garden.core :as g]
                 [garden.units        	:as u]
            					[garden.selectors    	:as sel]
            					[garden.stylesheet   	:as ss]
            					[garden.color        	:as color]
            					[garden.arithmetic   	:as ga]
                 [hiccup.core :as h]
                 [hiccup.def          	:as hd]
            					[hiccup.element      	:as he]
            					[hiccup.form         	:as hf]
            					[hiccup.page         	:as hp]
            					[hiccup.util         	:as hu]
                 [ring.util.anti-forgery :as ruaf]
                 [ring.util.response     	:as ring]))

;;-----------------------------------------------------------------------------

(defn- mk-parent-dd
	[item]
	(let [lists      (get-list-names)
		  list-names (sort (map :entryname lists))
		  tl-name    (some #(when (= (:_id %) (:parent item)) (:entryname %)) lists)]
		(hf/drop-down {:class "item-info"} :parent list-names tl-name)))

(defn- info-part
	[item]
	(named-div "Information"
		[:table.group
			[:tr
				[:td.item-info-th [:label "Parent"]]
				[:td (mk-parent-dd item)]]
	    	[:tr
				[:td.item-info-th [:label "Namn"]]
				[:td (hf/text-field {:class "item-info"} :entryname (:entryname item))]]
			[:tr
				[:td.item-info-th [:label "Enhet"]]
				[:td (hf/text-field {:class "item-info"} :unit (:unit item))]]
			[:tr
				[:td.item-info-th [:label "Mängd"]]
				[:td (hf/text-field {:class "item-info"} :amount (:amount item))]]
			[:tr
				[:td.item-info-th [:label "Pris"]]
				[:td (hf/text-field {:class "item-info"} :price (:price item))]]
			[:tr
				[:td.item-info-th [:label "URL"]]
				[:td (hf/text-field {:class "item-info"} :url (:url item))]]
		]))

;;-----------------------------------------------------------------------------

(defn- extract-id
	[params]
	(if (or (str/blank? (:_id params))
			(not (item-id-exists? (:_id params))))
		(throw (ex-info "invalid id" {:cause :_id}))
		(:_id params)))

(defn- extract-name
	[params]
	(if (str/blank? (:entryname params))
		(throw (ex-info "invalid name" {:cause :entryname}))
		(:entryname params)))

(defn- extract-parent
	[params]
	(if (str/blank? (:parent params))
		(throw (ex-info "invalid name" {:cause :entryname}))
		(if (= (:parent params) top-lvl-name)
			nil
			(if-let [found (utils/find-first #(= (:entryname %) (:parent params))
											 (get-list-names))]
				(:_id found)
				(throw (ex-info "invalid parent" {:cause :parent}))))))

(defn- extract-str
	[tag params]
	(when-not (str/blank? (get params tag))
		(get params tag)))

(defn- extract-num
	[tag params]
	(when (extract-str tag params)
		(Double/valueOf (get params tag))))

;;-----------------------------------------------------------------------------

(defn edit-item
    [request item-id]
    (try
        (let [item (get-item item-id)]
            (common request "Edit item" [css-tags-tbl css-items]
                    (hf/form-to
                        [:post "/admin/edit-item"]
                        (ruaf/anti-forgery-field)
                        (hf/hidden-field :_id (:_id item))
                        [:div
                         (homeback-button "/admin")
                         [:a.link-head {:href (str "/admin/delete-item/" item-id)} "Ta bort"]
                         (hf/submit-button {:class "button"} "Uppdatera")]
                        [:div
                         (info-part item)
                         (named-div "Ny kategori:" (hf/text-field {:class "item-info"} :new-tag))
                         (old-tags-tbl (:tags item))]
                        )))
        (catch Throwable e (error-page request "/admin/edit-item" "" e))))

(defn edit-item!
    [{params :params :as request}]
    (try
        (update-item {
                             :_id       (extract-id 			params)
                             :entryname (extract-name 		params)
                             :parent    (extract-parent 		params)
                             :unit      (extract-str 		:unit params)
                             :amount    (extract-num 		:amount params)
                             :price     (extract-num 		:price params)
                             :url       (extract-str 		:url params)
                             :tags      (extract-tags params)})
        (ring/redirect (str "/admin/edit-item/" (extract-id params)))
        (catch Throwable e (error-page request "/admin/edit-item!" "" e))))

;;-----------------------------------------------------------------------------

(defn new-item
	[request]
    (try
        (common request "Skapa ny sak" [css-tags-tbl]
		(hf/form-to
    		[:post "/admin/new-item"]
        	(ruaf/anti-forgery-field)
        	[:div
    			(homeback-button "/admin")
    			[:a.link-head (hf/submit-button {:class "button"} "Skapa")]]
	        [:div
	        	(info-part nil)
             (named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
		    	(old-tags-tbl)
		    	]))
        (catch Throwable e (error-page request "/admin/new-item" "" e))))

(defn new-item!
    [request]
    (try
        (catch Throwable e (error-page request "/admin/new-item!" "" e))))

;;-----------------------------------------------------------------------------

(defn delete-item!
    [request item-id]
    (try
        (delete-item item-id)
        (ring/redirect "/admin/home")
        (catch Throwable e (error-page request "/admin/delete-item!" "" e))))

;;-----------------------------------------------------------------------------

