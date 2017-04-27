(ns shop2.db
	(:require 	(clj-time	[core     :as t]
            				[local    :as l]
            				[format   :as f]
            				[periodic :as p])
            	(clojure 	[set      :as set]
            				[pprint   :as pp]
            				[string   :as str])
            	[cheshire.core :refer :all]
            	(monger 	[core     :as mg]
            				[credentials :as mcr]
            				[collection :as mc]
            				[joda-time  :as jt]
            				[operators :refer :all])
            	(shop2 		[utils       :as utils])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defonce db-conn  (mg/connect))
(defonce shopdb   (mg/get-db db-conn "shopdb"))
(defonce lists    "lists")
(defonce recipes  "recipes")
(defonce menus    "menus")
(defonce projects "projects")
(defonce items    "items")
(defonce tags     "tags")

;(let [admin-db   "admin"
;      u    "username"
;      p    (.toCharArray "password")
;      cred (mcr/create u admin-db p)
;      host "127.0.0.1"]
;  (mg/connect-with-credentials host cred))

;;-----------------------------------------------------------------------------

(def menu {
	:_id 123
	:text ""
	:recipe {:_id 123 :entryname ""}
	})

(defn mk-id
	[]
	(str (java.util.UUID/randomUUID)))

(defn now-str
	[]
	(f/unparse
		(f/with-zone
			(f/formatters :mysql)
			(t/default-time-zone))
		(l/local-now)))

(defn mk-std-field
	[]
	{:_id (mk-id) :created (utils/now)})

(defn get-tags
	[]
	(mc/find-maps shopdb tags))

(defn get-list
	[listid]
	(mc/find-one-as-map shopdb lists {:_id listid}))

(defn get-lists
	[]
	(mc/find-maps shopdb lists))

(defn get-list-names
	[]
	(mc/find-maps shopdb lists {} ["entryname"]))

(defn get-top-lists
	[]
	(mc/find-maps shopdb lists {:parent nil}))

(defn get-sub-lists
	[listid]
	(mc/find-maps shopdb lists {:parent listid}))

(defn get-recipes
	[]
	(mc/find-maps shopdb recipes))

(defn get-recipe
	[id]
	(mc/find-one-as-map shopdb recipes {:_id id}))

(defn get-projects
	[]
	(mc/find-maps shopdb projects))

(defn get-active-projects
	[]
	(->> (mc/find-maps shopdb projects {:finished nil})
		 (sort-by :priority)))

(defn get-project
	[id]
	(mc/find-one-as-map shopdb projects {:_id id}))

(defn get-menus
	[from to]
	(sort-by :date (mc/find-maps shopdb menus {:date {$gte from $lte to}})))

(defn get-items
	[]
	(mc/find-maps shopdb items))

(defn get-item
	[id]
	(mc/find-one-as-map shopdb items {:_id id}))

(defn add-tags
	[tags*]
	(let [db-tags    (->> (get-tags) (map :entryname) set)
		  clean-tags (->> tags* (map #(->> % str/trim str/capitalize)) set)
		  new-tags   (set/difference clean-tags db-tags)]
		(doseq [tag-name new-tags]
			(mc/insert shopdb tags (merge {:entryname tag-name} (mk-std-field))))))

(defn add-list
	[entry]
	(add-tags (:tags entry))
	(mc/insert shopdb lists (merge entry (mk-std-field))))

(defn add-item
	[entry]
	(add-tags (:tags entry))
	(mc/insert shopdb items (merge entry (mk-std-field))))

(defn add-project
	[entry]
	(add-tags (:tags entry))
	(mc/insert shopdb projects (merge entry (mk-std-field))))

(defn add-recipe
	[entry]
	(add-tags (:tags entry))
	(mc/insert shopdb recipes (merge entry (mk-std-field))))

(defn add-recipe-to-menu
	[menu-date recipe-id]
	(mc/update shopdb menus {:date menu-date}
		:recipe (mc/find-one-as-map shopdb recipes {:_id recipe-id} ["_id" "entryname"])))

;;-----------------------------------------------------------------------------

(defn item->list
	[list-id item-id num-of]
	(let [item* (get-item item-id)
		  item  (merge item* {:numof num-of :added (l/local-now)})]
		(mc/update-by-id shopdb lists list-id {$addToSet {:items item}})))

(defn update-recipe
	[recipe]
	(mc/update-by-id shopdb recipes (:_id recipe) recipe))

(defn finish-project
	[project-id]
	(mc/update-by-id shopdb projects project-id {:finished (l/local-now)}))

(defn unfinish-project
	[project-id]
	(mc/update-by-id shopdb projects project-id {:finished nil}))

(defn update-menu
	[menu-date mkey value]
	(mc/update shopdb menus {:date menu-date} {mkey value}))

(defn update-project
	[proj]
	(mc/update-by-id shopdb projects (:_id proj) proj))

(defn find-list-id
	[e-name]
	(get (mc/find-one-as-map shopdb lists {:entryname e-name}) :_id))

