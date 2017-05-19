(ns shop2.db.projects
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
  								[items			:as dbitems]
  								[lists 			:as dblists])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-projects
	[]
	{:post [(p-trace "get-projects" %) (q-valid? :shop/projects %)]}
	(log/trace "get-projects: (mc/find-maps shopdb projects)")
	(mc/find-maps shopdb projects {:cleared nil}))

(defn get-active-projects
	[]
	{:post [(p-trace "get-active-projects" %)]}
	(log/trace "get-active-projects: (mc/find-maps shopdb projects {:finished nil})")
	(->> (mc/find-maps shopdb projects
			{:finished nil}
			{:_id true :entryname true :priority true})
		 (sort-by :priority)))

(defn get-project
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-project" %) (q-valid? :shop/project %)]}
	(log/trace "get-project: (mc/find-one-as-map shopdb projects {:_id " id "})")
	(mc/find-one-as-map shopdb projects {:_id id}))

(defn add-project
	[entry]
	{:pre [(q-valid? :shop/project* entry)]
	 :post [(p-trace "add-project" %) (q-valid? :shop/project %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-project: (mc/insert shopdb projects " entry* ")")
		(mc/insert shopdb projects entry*)
		entry*))

(defn finish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]
	 :post [(p-trace "finish-project" %)]}
	(log/trace "finish-project: (mc/update-by-id shopdb projects " project-id " {$set {:finished " (l/local-now) "}})")
	(mc/update-by-id shopdb projects project-id {$set {:finished (l/local-now)}}))

(defn unfinish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]
	 :post [(p-trace "unfinish-project" %)]}
	(log/trace "unfinish-project: (mc/update-by-id shopdb projects " project-id " {$unset :finished})")
	(mc/update-by-id shopdb projects project-id {$set {:finished nil}}))

(defn update-project
	[proj]
	{:pre [(q-valid? :shop/project* proj)]
	 :post [(p-trace "update-project" %)]}
	(log/trace "update-project: (mc/update-by-id shopdb projects " (:_id proj) " {$set " proj "})")
	(mc/update-by-id shopdb projects (:_id proj)
		{$set (select-keys proj [:entryname :priority :finished :tags])}))

(defn clear-projects
	[]
	(mc/update shopdb projects
		{:finished {$type "date"}}
		{$set {:cleared (utils/now)}}
		{:multi true}))

