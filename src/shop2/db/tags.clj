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
	{:post [(q-valid? :shop/tags %)]}
	(mc-find-maps "get-tags" tags))

(defn get-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(q-valid? :shop/tag %)]}
	(mc-find-map-by-id "get-tag" tags id))

(defn update-tag
	[tag-id tag-name]
	{:pre [(q-valid? :shop/_id tag-id)]}
	(mc-update-by-id "update-tag" tags (:_id tag-id)
		{$set {:entryname tag-name}}))

(defn get-tag-names
	[]
	{:post [(q-valid? :shop/strings %)]}
	(mc-find-maps "get-tag-names" tags {} {:_id true :entryname true}))

(defn add-tag
	[tag-name]
	{:pre [(q-valid? :shop/string tag-name)]
	 :post [(q-valid? :shop/tag %)]}
	(let [db-tags      (get-tags)
		  db-tag-names (->> db-tags (map :entryname) set)
		  clean-tag    (->> tag-name str/trim str/capitalize)
		  new-tag      (merge {:entryname clean-tag} (mk-std-field))]
		(if (some #{clean-tag} db-tag-names)
			(some #(when (= (:entryname %) clean-tag) %) db-tags)
			(do
				(mc-insert "add-tag" tags new-tag)
				new-tag))))

(defn add-tags
	[tags*]
	{:pre [(q-valid? :shop/tags* tags*)]}
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
				(mc-insert-batch "add-tags" tags new-tags)
				(throw (Exception. "Invalid tags"))))
		all-tags))

(defn add-tag-names
	[names]
	{:pre [(q-valid? :shop/strings names)]}
	(add-tags (map #(hash-map :entryname %) names)))

(defn delete-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(mc-remove-by-id "delete-tag" tags id))

(defn delete-tag-all
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(delete-tag id)
	(mc-update "delete-tag-all" lists {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" recipes {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" menus {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" projects {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" items {} {$pull {:tags {:_id id}}} {:multi true})
	)

