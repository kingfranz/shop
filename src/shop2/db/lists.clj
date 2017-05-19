(ns shop2.db.lists
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
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-list
	[listid]
	{:pre [(q-valid? :shop/_id listid)]
	 :post [(p-trace "get-list" %) (q-valid? :shop/list %)]}
	(log/trace "get-list: (mc/find-one-as-map shopdb lists {:_id " listid "})")
	(mc/find-one-as-map shopdb lists {:_id listid}))

(defn get-lists
	[]
	{:post [(p-trace "get-lists" %) (q-valid? :shop/lists %)]}
	(log/trace "get-lists: (mc/find-maps shopdb lists)")
	(mc/find-maps shopdb lists))

(defn get-list-names
	[]
	{:post [(p-trace "get-list-names" %) (q-valid? :shop/strings %)]}
	(mc/find-maps shopdb lists {} {:_id true :entryname true}))

(defn get-top-list
	[a-list]
	{:pre [(q-valid? :shop/list a-list)]
	 :post [(p-trace "get-top-list" %) (q-valid? :shop/list %)]}
	(if (nil? (:parent a-list))
		a-list
		(get-top-list (get-list (:parent a-list)))))

(defn get-top-lists
	[]
	{:post [(p-trace "get-top-lists" %) (q-valid? :shop/lists %)]}
	(log/trace "get-top-lists: (mc/find-maps shopdb lists {:parent nil})")
	(mc/find-maps shopdb lists {:parent nil}))

(defn get-sub-lists
	[listid]
	{:pre [(q-valid? :shop/_id listid)]
	 :post [(p-trace "get-sub-lists" %) (q-valid? :shop/lists %)]}
	(log/trace "get-sub-lists: (mc/find-maps shopdb lists {:parent " listid "})")
	(mc/find-maps shopdb lists {:parent._id listid}))

(defn list-id-exists?
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(= (get (mc/find-map-by-id shopdb lists id {:_id true}) :_id) id))

(defn add-list
	[entry]
	{:pre [(q-valid? :shop/list* entry)]
	 :post [(p-trace "add-list" %) (q-valid? :shop/list %)]}
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-list: (mc/insert shopdb lists " entry* ")")
		(mc/insert shopdb lists entry*)
		entry*))

(defn update-list
	[entry]
	{:pre [(q-valid? :shop/list* entry)]
	 :post [(p-trace "update-list" %)]}
	(log/trace "update-list: (mc/update-by-id shopdb lists " (:_id entry) " " (select-keys entry [:entryname :parent]) ")")
	(mc/update-by-id shopdb lists (:_id entry)
		{$set (select-keys entry [:entryname :parent])}))

(defn delete-list
	[list-id]
	{:pre [(q-valid? :shop/_id list-id)]}
	(log/trace "delete-list: (mc/remove-by-id shopdb lists " list-id ")")
	(mc/remove-by-id shopdb lists list-id)
	(log/trace "delete-list: (mc/find-maps shopdb lists {} {:_id :parent})")
	(doseq [mlist (mc/find-maps shopdb lists {} {:_id true :parent true})
	  :let [np (some->> mlist :parent :parent)]
	  :when (= (some->> mlist (spy "delete-list") :parent :_id) list-id)]
		(log/trace "delete-list: (mc/update-by-id shopdb lists " (:_id mlist) " {$set {:parent " np "}})")
		(mc/update-by-id shopdb lists (:_id mlist) {$set {:parent np}})))

(defn get-lists-with-count
	[]
	{:post [(p-trace "get-lists-with-count" %)]}
	(log/trace "get-lists-with-count: ()")
	(mc/aggregate shopdb lists
		[{$project {:_id true
		 		    :entryname true
		 		    :parent true
		 		    :count {
		 		    	$cond [{$gt ["$items" nil]}
					           {$size {
					           		"$filter" {
					           			:input "$items"
					                    :as "item"
					                    :cond {$not [{$gt ["$$item.finished" nil]}]}}}}
					           0]}}}]))

(defn finish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]
	 :post [(p-trace "finish-list-item" %)]}
	(add-item-usage list-id item-id :finish 0)
	(log/trace "finish-list-item: (mc/update shopdb lists {:_id " list-id " :items._id " item-id "} {$set {:items.$.finished " (l/local-now) "}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished (l/local-now)}}))

(defn unfinish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]
	 :post [(p-trace "unfinish-list-item" %)]}
	(add-item-usage list-id item-id :unfinish 0)
	(log/trace "unfinish-list-item: (mc/update shopdb lists {:_id " list-id " :items {:_id " item-id" }} {$set {:finished nil}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished nil}}))

(defn del-finished-list-items
	[list-id]
	{:pre [(q-valid? :shop/_id list-id)]
	 :post [(p-trace "del-finished-list-items" %)]}
	(let [a-list (get-list list-id)
		  clean (remove #(some? (:finished %)) (:items a-list))]
		(log/trace "del-finished-list-items: (mc/update-by-id shopdb lists " list-id " {$set {:items clean}})")
		(mc/update-by-id shopdb lists list-id
			{$set {:items clean}})))

(defn- remove-item
	[list-id item-id]
	(add-item-usage list-id item-id :remove 0)
	(log/trace "remove-item: (mc/update shopdb lists {:_id " list-id "} {$pull {:items {:_id " item-id "}}})")
	(mc/update shopdb lists
		{:_id list-id}
		{$pull {:items {:_id item-id}}}))

(defn- mod-item
	[list-id item-id num-of]
	(add-item-usage list-id item-id :mod num-of)
	(log/trace "mod-item: (mc/update shopdb lists {:_id " list-id " :items._id " item-id "} {$inc {:items.$.numof " num-of "}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$inc {:items.$.numof num-of}}))

(defn find-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id)
		   (q-valid? :shop/_id item-id)]
	 :post [(p-trace "find-item" %)]}
	(log/trace "find-item: (mc/find-one-as-map shopdb lists {:_id " list-id " :items._id " item-id "} {:items.$ 1})")
	(some->> (mc/find-one-as-map shopdb lists {:_id list-id :items._id item-id} {:items.$ 1})
			 :items
			 first))

(defn item->list
	[list-id item-id num-of]
	{:pre [(q-valid? :shop/_id list-id)
		   (q-valid? :shop/_id item-id)
		   (q-valid? int? num-of)]
	 :post [(p-trace "item->list" %)]}
	(log/trace "item->list" list-id item-id num-of)
	; find the item if it's already in the list
	(if-let [item (find-item list-id item-id)]
		; yes it was
		(do
			(log/trace "item->list (zero? num-of)" (zero? num-of) "(:numof item)" (:numof item))
			(if (or (zero? num-of) (<= (+ (:numof item) num-of) 0))
				(finish-list-item list-id item-id)
				(mod-item list-id item-id num-of)))
		; no, we need to add it
		(when (pos? num-of)
			(add-item-usage list-id item-id :add-to num-of)
			(log/trace "item->list: (mc/update-by-id shopdb lists {$addToSet {:items (assoc (get-item " item-id ") :numof " num-of ")}})")
			(mc/update-by-id shopdb lists list-id
				{$addToSet {:items (assoc (dbitems/get-item item-id) :numof num-of)}}))))

(defn find-list-by-name
	[e-name]
	{:pre [(q-valid? :shop/string e-name)]
	 :post [(p-trace "find-list-by-name" %) (q-valid? :shop/list %)]}
	(log/trace "find-list-by-name: (mc/find-one-as-map shopdb lists {:entryname " e-name "})")
	(mc/find-one-as-map shopdb lists {:entryname e-name}))

