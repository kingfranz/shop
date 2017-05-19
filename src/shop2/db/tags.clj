(ns shop2.db.tags
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
            	
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defonce tags     "tags")

(defn get-tags
	[]
	{:post [(p-trace "get-tags" %) (q-valid? :shop/tags %)]}
	(log/trace "get-tags: (mc/find-maps shopdb tags)")
	(mc/find-maps shopdb tags))

(defn get-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-tag" %) (q-valid? :shop/tag %)]}
	(log/trace "get-tag: (mc/find-map-by-id shopdb tags " id ")")
	(mc/find-map-by-id shopdb tags id))

(defn update-tag
	[tag-id tag-name]
	{:pre [(q-valid? :shop/_id tag-id)]
	 :post [(p-trace "update-tag" %)]}
	(log/trace "update-tag: (mc/update-by-id shopdb tags (:_id " tag-id ") {$set {:entryname " tag-name "}})")
	(mc/update-by-id shopdb tags (:_id tag-id)
		{$set {:entryname tag-name}}))

(defn get-tag-names
	[]
	{:post [(p-trace "get-tag-names" %) (q-valid? :shop/strings %)]}
	(mc/find-maps shopdb tags {} {:_id true :entryname true}))

(defn add-tag
	[tag-name]
	{:pre [(q-valid? :shop/string tag-name)]
	 :post [(p-trace "add-tag" %) (q-valid? :shop/tag %)]}
	(log/trace "add-tag: (get-tags)")
	(let [db-tags      (get-tags)
		  db-tag-names (->> db-tags (map :entryname) set)
		  clean-tag    (->> tag-name str/trim str/capitalize)
		  new-tag      (merge {:entryname clean-tag} (mk-std-field))]
		(if (some #{clean-tag} db-tag-names)
			(some #(when (= (:entryname %) clean-tag) %) db-tags)
			(do
				(log/trace "add-tag: (mc/insert shopdb tags " new-tag ")")
				(mc/insert shopdb tags new-tag)
				new-tag))))

(defn add-tags
	[tags*]
	{:pre [(q-valid? :shop/tags* tags*)]
	 :post [(p-trace "add-tags" %)]}
	(log/trace "add-tags: (get-tags)")
	(let [db-tags         (get-tags)
		  db-tag-names    (->> db-tags (map :entryname) set)
		  clean-tag-names (->> tags*
		  					   (map #(->> % :entryname str/trim str/capitalize))
		  					   set)
		  new-tag-names   (set/difference clean-tag-names db-tag-names)
		  new-tags        (mapv #(merge {:entryname %} (mk-std-field)) new-tag-names)
		  old-tag-names   (set/difference clean-tag-names new-tag-names)
		  all-tags        (concat new-tags
		  						  (map #(utils/find-first (fn [t] (= (:entryname t) %)) db-tags)
		  						  	   old-tag-names))]
		(when (seq new-tags)
			(if (q-valid? :shop/tags new-tags)
				(do
					(log/trace "add-tags: (mc/insert-batch shopdb tags " new-tags ")")
					(mc/insert-batch shopdb tags new-tags))
				(throw (Exception. "Invalid tags"))))
		all-tags))

(defn add-tag-names
	[names]
	{:pre [(q-valid? :shop/strings names)]}
	(log/trace "add-tag-names: " names)
	(add-tags (map #(hash-map :entryname %) names)))

(defn delete-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(log/trace "delete-tag: (mc/remove-by-id shopdb tags " id ")")
	(mc/remove-by-id shopdb tags id))

(defn delete-tag-all
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(log/trace "delete-tag: (mc/remove-by-id shopdb tags " id ")")
	(delete-tag id)
	(mc/update shopdb lists {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb recipes {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb menus {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb projects {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb items {} {$pull {:tags {:_id id}}} {:multi true})
	)

