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

(defn add-item-usage
	[list-id item-id action numof]
	(log/trace "add-item-usage")
	(mc/insert shopdb item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

(defn get-user-data
	[uname]
	{:pre [(q-valid? :shop/string uname)]
	 :post [(p-trace "get-user-data" %) (map? %)]}
	(log/trace "get-user-data: (mc/find-maps shopdb users {:uname " uname "})")
	(let [udata (mc/find-one-as-map shopdb users {:uname uname})]
		(if (empty? udata)
			{:uname uname}
			udata)))

(defn set-user-data
	[udata]
	;{:pre [(q-valid? :shop/string uname)]
	; :post [(p-trace "set-user-data" %) (map? %)]}
	(if (nil? (:_id udata))
		(do
			(log/trace "set-user-data: (mc/insert shopdb users (merge " udata " (mk-std-field)))")
			(mc/insert shopdb users (merge udata (mk-std-field))))
		(do
			(log/trace "set-user-data: (mc/update-by-id shopdb users " (:_id udata) udata ")")
			(mc/update-by-id shopdb users (:_id udata) udata))))

;;-----------------------------------------------------------------------------

(defn save-session-data
	[key data]
	(mc/insert shopdb sessions (assoc {:_id key} :data data)))

(defn read-session-data
	[key]
	(get (mc/find-one-as-map shopdb sessions {:_id key}) :data))

(defn delete-session-data
	[key]
	(mc/remove-by-id shopdb sessions key))

