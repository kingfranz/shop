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
	{:post [(p-trace "get-item-names" %)]}
	(log/trace "get-item-names: (mc/find-maps shopdb items {})")
	(mc/find-maps shopdb items {} {:_id true :entryname true}))

(defn get-items
	[]
	{:post [(p-trace "get-items" %) (q-valid? :shop/items %)]}
	(log/trace "get-items: (mc/find-maps shopdb items {})")
	(mc/find-maps shopdb items {}))

(defn get-item
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-item" %) (q-valid? :shop/item %)]}
	(log/trace "get-item: (mc/find-maps shopdb items {:_id " id "})")
	(mc/find-one-as-map shopdb items {:_id id}))

(defn item-id-exists?
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(= (get (mc/find-map-by-id shopdb items id {:_id true}) :_id) id))

(defn add-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]
	 :post [(p-trace "add-item" %) (q-valid? :shop/item %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(add-item-usage nil (:_id entry*) :create 0)
		(log/trace "add-item: (mc/insert shopdb items " entry* ")")
		(mc/insert shopdb items entry*)
		entry*))

(defn update-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]
	 :post [(p-trace "update-item" %)]}
	(add-item-usage nil (:_id entry) :update 0)
	(log/trace "update-item: (mc/update-by-id shopdb items " (:_id entry) " " (select-keys entry [:entryname :unit :url :amount :price :tags]) ")")
	(mc/update-by-id shopdb items (:_id entry)
		{$set (select-keys entry [:entryname :unit :url :amount :price :tags :parent])}))

(defn delete-item
	[item-id]
	{:pre [(q-valid? :shop/_id item-id)]
	 :post [(p-trace "delete-item" %)]}
	(add-item-usage nil item-id :delete 0)
	(log/trace "delete-item: (mc/remove-by-id shopdb items " item-id ")")
	(mc/remove-by-id shopdb items item-id))

