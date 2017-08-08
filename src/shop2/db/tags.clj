(ns shop2.db.tags
	(:require 	(clj-time		[core     		:as t]
            					[local    		:as l]
            					[coerce   		:as c]
            					[format   		:as f]
            					[periodic 		:as p])
            	(clojure 		[set      		:as set]
            					[pprint   		:as pp]
            					[string   		:as str])
            	(clojure.spec 	[alpha          :as s])
             	(cheshire 		[core     		:refer :all])
            	(taoensso 		[timbre   		:as log])
            	(monger 		[core     		:as mg]
            					[credentials 	:as mcr]
            					[collection 	:as mc]
            					[joda-time  	:as jt]
            					[operators 		:refer :all])
            	(shop2 			[utils     		:refer :all]
            					[spec       	:as spec]
            					[db 			:refer :all])
            	
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

(defn get-tag-names
	[]
	{:post [(q-valid? :shop/strings %)]}
	(mc-find-maps "get-tag-names" tags {} {:_id true :entryname true}))

(defn update-tag
	[tag-id tag-name*]
	{:pre [(q-valid? :shop/_id tag-id)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)]
		(if (some? db-tag)
			(if (= (:_id db-tag) tag-id)
				db-tag
				(throw (ex-info "duplicate name" {:cause :dup})))
			(mc-update-by-id "update-tag" tags (:_id tag-id)
				{$set {:entryname tag-name :entrynamelc tag-namelc}}))))

(defn add-tag
	[tag-name*]
	{:pre [(q-valid? :shop/string tag-name*)]
	 :post [(q-valid? :shop/tag %)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)
		  new-tag    (merge {:entryname tag-name
		  					 :entrynamelc tag-namelc} (mk-std-field))]
		(if (some? db-tag)
			db-tag
			(do
				(mc-insert "add-tag" tags new-tag)
				new-tag))))

(defn add-tags
	[tags*]
	{:pre [(q-valid? :shop/tags* tags*)]}
	(map #(add-tag (:entryname %)) tags*))

(defn add-tag-names
	[names]
	{:pre [(q-valid? :shop/strings names)]}
	(map add-tag names))

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

