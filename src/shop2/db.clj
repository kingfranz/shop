(ns shop2.db
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
            				[spec       :as spec])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defonce db-conn  (mg/connect))
(defonce shopdb   (mg/get-db db-conn "shopdb"))
(defonce sessions "sessions")
(defonce item-usage "item-usage")
(defonce users      "users")
(defonce recipes  "recipes")

(defonce menus    "menus")

(defonce items    "items")

(defonce projects "projects")

(defonce lists    "lists")


;;-----------------------------------------------------------------------------

(defmacro q-valid? [sp v]
  `(q-valid* ~*file*
  	         ~(:line (meta &form))
  	         ~sp
  	         ~v))

(defn q-valid*
	[f l sp v]
	;(println "\nq-valid:" (str f ":" l) (pr-str sp) (pr-str v))
	(if-not (s/valid? sp v)
		(do
			(println "\n---------- " f l " ------------")
			(prn v)
			(println "---------------------------------------")
			(prn (s/explain-str sp v))
			(println "---------------------------------------"))
		true))

(defn p-trace
	[s v]
	(log/trace "\n" s "return:\n" (pr-str v) "\n")
	true)

;;-----------------------------------------------------------------------------

(defn spy
	([v]
	(spy "" v))
	([s v]
	(println "------------- SPY ---------------")
	(when-not (str/blank? s)
		(println s))
	(prn (type v))
	(pp/pprint v)
	(println "---------------------------------")
	v))

;;-----------------------------------------------------------------------------

(defn mk-id
	[]
	(str (java.util.UUID/randomUUID)))

(defn mk-std-field
	[]
	{:_id (mk-id) :created (utils/now)})

;;-----------------------------------------------------------------------------

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
	(second (re-matches #"^[^$]+\$(.+)@.+$" (str s))))

(defn- do-mc
	[mc-func caller tbl & args]
	(log/trace (apply str caller ": " (fname mc-func) " " tbl " " args))
	(let [ret (apply mc-func shopdb tbl (first args))]
		(log/trace caller "returned:" (pr-str ret))
		ret))

(defn mc-aggregate
	[func tbl & args]
	(do-mc mc/aggregate func tbl args))

(defn mc-find-maps
	[func tbl & args]
	(do-mc mc/find-maps func tbl args))

(defn mc-find-one-as-map
	[func tbl & args]
	(do-mc mc/find-one-as-map func tbl (vec args)))

(defn mc-find-map-by-id
	[func tbl & args]
	(do-mc mc/find-map-by-id func tbl args))

(defn mc-insert
	[func tbl & args]
	(do-mc mc/insert func tbl args))

(defn mc-insert-batch
	[func tbl & args]
	(do-mc mc/insert-batch func tbl args))

(defn mc-update
	[func tbl & args]
	(do-mc mc/update func tbl args))

(defn mc-update-by-id
	[func tbl & args]
	(do-mc mc/update-by-id func tbl args))

(defn mc-remove-by-id
	[func tbl & args]
	(do-mc mc/remove-by-id func tbl args))

;;-----------------------------------------------------------------------------

(defn add-item-usage
	[list-id item-id action numof]
	(mc-insert "add-item-usage" item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

(defn get-user-data
	[uname]
	{:pre [(q-valid? :shop/string uname)]
	 :post [(map? %)]}
	(let [udata (mc-find-one-as-map "get-user-data" users {:uname uname})]
		(if (empty? udata)
			{:uname uname}
			udata)))

(defn set-user-data
	[udata]
	;{:pre [(q-valid? :shop/string uname)]
	; :post [(p-trace "set-user-data" %) (map? %)]}
	(if (nil? (:_id udata))
		(mc-insert "set-user-data" users (merge udata (mk-std-field)))
		(mc-update-by-id "set-user-data" users (:_id udata) udata)))

;;-----------------------------------------------------------------------------

(defn save-session-data
	[key data]
	(mc-insert "save-session-data" sessions (assoc {:_id key} :data data)))

(defn read-session-data
	[key]
	(get (mc-find-one-as-map "read-session-data" sessions {:_id key}) :data))

(defn delete-session-data
	[key]
	(mc-remove-by-id "delete-session-data" sessions key))

