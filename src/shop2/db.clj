(ns shop2.db
  (:require [clojure.pprint :as pp]
  			[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[clojure.set :as set])
  (:import  [java.util UUID]))

(def ^:private database (atom {:lists    {}
    						   :recipes  {}
    						   :projects {}
    						   :menu     {}
    						   :items    {}}))
(def ^:private database-fname "./shop.data")


(defn now-str
	[]
	(f/unparse (f/with-zone (f/formatters :mysql) (t/default-time-zone)) (l/local-now)))


(defn save-db
	[]
	(clojure.pprint/pprint @database (clojure.java.io/writer database-fname)))

(defn load-db
	[]
	(reset! database (read-string (slurp database-fname))))

(defn get-tags
	[]
	(apply set/union (map :tags (vals (:lists @database)))))

(defn get-list
	[id]
	(get (:lists @database) id))

(defn get-lists
	[]
	(vals (:lists @database)))

(defn get-recipes
	[]
	(vals (:recipes @database)))

(defn get-recipe
	[id]
	(get (:recipes @database) id))

(defn get-projects
	[]
	(vals (:projects @database)))

(defn get-project
	[id]
	(get (:projects @database) id))

(defn get-menu
	[]
	(:menu @database))

(defn get-items
	[]
	(vals (:items @database)))

(defn get-item
	[id]
	(get (:items @database) id))

(defn add-list
	[entry]
	(reset! database (assoc-in @database [:lists (:_id entry)] entry))
	(save-db))

(defn add-item
	[entry]
	(reset! database (assoc-in @database [:items (:_id entry)] entry))
	(save-db))

(defn add-project
	[entry]
	(reset! database (assoc-in @database [:projects (:_id entry)] entry))
	(save-db))

(defn- add-or-inc
	[map-list pred upd-func]
	(loop [a-list map-list
		   acc    (empty map-list)]
		(if (empty? a-list)
			(conj acc (upd-func nil))
			(if (pred (first a-list))
				(concat (conj acc (upd-func (first a-list))) (rest a-list))
				(recur (rest a-list) (conj acc (first a-list)))))))

(defn- update-list
	[list-id item-id num]
	(let [the-list (get-list list-id)]
		(assoc the-list :items (add-or-inc (:items the-list)
										   #(= (:_id %) item-id)
										   #(if (nil? %)
										   		{:_id item-id :num-of num}
										   		(update-in % [:num-of] + num))))))

(defn item->list
	[list-id item-id num]
	(when (or (nil? (get-list list-id)) (nil? (get-item item-id)))
		(throw (Exception. "unknown list or item")))
	(reset! database (assoc-in @database [:lists list-id] (update-list list-id item-id num)))
	(save-db))

(defn update-recipe
	[recipe]
	(if-let [r-old (get-recipe (:_id recipe))]
		(do
			(reset! database (assoc-in @database [:recipes (:_id recipe)] (merge r-old recipe)))
			(save-db))
		(throw (Exception. "Unknown recipe"))))

(defn finish-project
	[id]
	(when-let [old-proj (get-project id)]
		(reset! database (assoc-in @database [:projects id] (assoc old-proj :finished (now-str))))
		(save-db)))

(defn unfinish-project
	[id]
	(when-let [old-proj (get-project id)]
		(reset! database (assoc-in @database [:projects id] (dissoc old-proj :finished)))
		(save-db)))

(defn update-menu
	[menu-date mkey value]
	(reset! database (assoc-in @database [:menu :items menu-date mkey] value))
	(save-db))

(defn update-project
	[proj]
	(if-let [r-old (get-project (:_id proj))]
		(do
			(reset! database (assoc-in @database [:projects (:_id proj)] (merge r-old proj)))
			(save-db))
		(throw (Exception. "Unknown project"))))

(defn mk-id
	[]
	(str (java.util.UUID/randomUUID)))

(defn- lookup
	[k m]
	(get m k))

(defn find-list-id
	[e-name]
	(some->> (get-lists)
			 (group-by :entry-name)
			 (lookup e-name)
			 first
			 (lookup :_id)))

