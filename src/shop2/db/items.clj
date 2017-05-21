(ns shop2.db.items
	(:require 	(clj-time	[core     :as t]
            				[local    :as l]
            				[coerce   :as c]
            				[format   :as f]
            				[periodic :as p])
            	(clojure 	[set      :as set]
            				[pprint   :as pp]
            				[spec     :as s]
            				[string   :as str])
            	(cheshire 	[core     :refer :all])
            	(taoensso 	[timbre   :as log])
            	(monger 	[core     :as mg]
            				[credentials :as mcr]
            				[collection :as mc]
            				[joda-time  :as jt]
            				[operators :refer :all])
            	(shop2 		[utils       :as utils]
            				[spec       :as spec]
            				[db 		:refer :all])
            	(shop2.db 		[tags 			:as dbtags])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-item-names
	[]
	(mc-find-maps "get-item-names" items {} {:_id true :entryname true}))

(defn get-items
	[]
	{:post [(q-valid? :shop/items %)]}
	(mc-find-maps "get-items" items {}))

(defn get-item
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(q-valid? :shop/item %)]}
	(mc-find-one-as-map "get-item" items {:_id id}))

(defn item-id-exists?
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(= (get (mc-find-map-by-id "item-id-exists?" items id {:_id true}) :_id) id))

(defn add-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]
	 :post [(q-valid? :shop/item %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(add-item-usage nil (:_id entry*) :create 0)
		(mc-insert "add-item" items entry*)
		entry*))

(defn update-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]}
	(add-item-usage nil (:_id entry) :update 0)
	(mc-update-by-id "update-item" items (:_id entry)
		{$set (select-keys entry [:entryname :unit :url :amount :price :tags :parent])}))

(defn delete-item
	[item-id]
	{:pre [(q-valid? :shop/_id item-id)]}
	(add-item-usage nil item-id :delete 0)
	(mc-remove-by-id "delete-item" items item-id))

