(ns shop2.db.projects
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
            	(shop2 			[utils       	:refer :all]
            					[spec       	:as spec]
            					[db 			:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-projects
	[]
	{:post [(q-valid? :shop/projects %)]}
	(mc-find-maps "get-projects" projects {:cleared nil}))

(defn get-active-projects
	[]
	(->> (mc-find-maps "get-active-projects" projects
			{:finished nil}
			{:_id true :entryname true :priority true})
		 (sort-by :priority)))

(defn get-project
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(q-valid? :shop/project %)]}
	(mc-find-one-as-map "get-project" projects {:_id id}))

(defn add-project
	[entry]
	{:pre [(q-valid? :shop/project* entry)]
	 :post [(q-valid? :shop/project %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-project" projects entry*)
		entry*))

(defn finish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]}
	(mc-update-by-id "finish-project" projects project-id {$set {:finished (l/local-now)}}))

(defn unfinish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]}
	(mc-update-by-id "unfinish-project" projects project-id {$set {:finished nil}}))

(defn update-project
	[proj]
	{:pre [(q-valid? :shop/project* proj)]}
	(mc-update-by-id "update-project" projects (:_id proj)
		{$set (select-keys proj [:entryname :priority :finished :tags])}))

(defn clear-projects
	[]
	(mc-update clear-projects projects
		{:finished {$type "date"}}
		{$set {:cleared (now)}}
		{:multi true}))

