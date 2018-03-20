(ns shop2.db
    (:require [shop2.extra :refer :all]
              [shop2.spec :refer :all]
              [utils.core :as utils]
              [slingshot.slingshot :refer [throw+ try+]]
              [taoensso.timbre :as log]
              [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]
              [clojure.pprint :as pp]
              [cemerick.friend :as friend]
              [cemerick.friend.workflows :as workflows]
              [cemerick.friend.credentials :as creds]
              [cheshire.core :refer :all]
              [monger.core :as mg]
              [monger.credentials :as mcr]
              [monger.collection :as mc]
              [monger.joda-time :as jt]
              [monger.operators :refer :all]
              [environ.core :refer [env]]
              )
    (:import [java.util UUID]))

;;-----------------------------------------------------------------------------

; mongo --port 27017 -u "mongoadmin" -p "Benq.fp731" --authenticationDatabase "admin"
; db.createUser({user:"shopper",pwd:"kAllE.kUlA399",roles:[{role:"readWrite",db:"shopdb"}]})

(defonce db-conn (mg/connect-with-credentials (env :database-ip)
							(mcr/create (env :database-user)
                   						(env :database-db)
                         				(env :database-pw)
                             )))
(defonce shopdb (mg/get-db db-conn (env :database-db)))

(defonce sessions   "sessions")
(defonce item-usage "item-usage")
(defonce users      "users")
(defonce recipes    "recipes")
(defonce menus      "menus")
(defonce items      "items")
(defonce projects   "projects")
(defonce lists      "lists")
(defonce tags       "tags")

(defonce no-id "00000000-0000-0000-0000-000000000000")

;;-----------------------------------------------------------------------------

(defn mk-id
	[]
 	{:post [(utils/valid? :shop/_id %)]}
	(str (UUID/randomUUID)))

(defn mk-std-field
	[]
 	{:post [(utils/valid? :shop/std-keys %)]}
	{:_id (mk-id) :created (l/local-now)})

;;-----------------------------------------------------------------------------

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
 	{:post [(utils/valid? :shop/string %)]}
	(second (re-matches #"^[^$]+\$(.+)@.+$" (str s))))

(defn- do-mc
	[mc-func caller tbl & args]
	(log/trace (apply str caller ": " (fname mc-func) " " tbl " " (first args)))
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

(defn mk-enlc
	[en]
 	{:pre [(utils/valid? :shop/string en)]
     :post [(utils/valid? :shop/string %)]}
	(-> en str/trim str/lower-case (str/replace #"[ \t-]+" "")))

(defn get-by-enlc
	[tbl en]
 	{:pre [(utils/valid? :shop/string tbl) (utils/valid? :shop/string en)]
     :post [(utils/valid? (s/nilable map?) %)]}
	(mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(defn add-item-usage
	[list-id item-id action numof]
 	{:pre [(utils/valid? (s/nilable :shop/_id) list-id)
           (utils/valid? :shop/_id item-id)
           (utils/valid? keyword? action)
           (utils/valid? number? numof)]}
	(mc-insert "add-item-usage" item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

(defn get-user
	[uname]
 	{:pre [(utils/valid? :shop/username uname)]
     :post [(utils/valid? (s/nilable :shop/user-db) %)]}
	(let [udata (mc-find-one-as-map "get-user" users
					{:username {$regex (str "^" (str/trim uname) "$") $options "i"}})]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(defn get-user-by-id
	[uid]
 	{:pre [(utils/valid? :shop/_id uid)]
     :post [(utils/valid? (s/nilable :shop/user-db) %)]}
	(let [udata (mc-find-map-by-id "get-user-by-id" users uid)]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(defn get-users
	[]
 	{:post [(utils/valid? (s/* :shop/user-db) %)]}
	(for [user (mc-find-maps "get-user" users {})]
		(-> user
			(update :roles #(->> % (map keyword) set))
			(update :created str))))

;;-----------------------------------------------------------------------------

(defn- count-chars
	[pw c-class]
	(if (nil? (re-find c-class pw))
		(throw+ (ex-info (str "PW must contain at least one of " c-class) {:cause :password :type :db}))
		pw))

(defn- verify-passwd
	[pw]
	(if (not= (str/trim pw) pw)
		(throw+ (ex-info "PW can't begin or end with space" {:cause :password :type :db}))
		(if (< (count pw) 8)
			(throw+ (ex-info "PW must be 8 chars or more" {:cause :password :type :db}))
			(-> pw
				(count-chars #"[a-zåäö]")
				(count-chars #"[A-ZÅÄÖ]")
				(count-chars #"[0-9]")
				(count-chars #"[.*!@#$%^&()=+-]")
				))))

(defn create-user
    [username passwd roles]
    {:pre [(utils/valid? :shop/username username)
           (utils/valid? :shop/password passwd)
           (utils/valid? :shop/roles roles)]
     :post [(utils/valid? :shop/user-db %)]}
    (when (some? (get-user username))
        (throw+ (ex-info "duplicate username" {:cause :username :type :db})))
    (let [user (merge {:username (str/trim username)
                       :password (creds/hash-bcrypt (verify-passwd passwd))
                       :roles    roles} (mk-std-field))]
        (mc-insert "create-user" users user)
        user))

(defn delete-user
    [userid]
    (mc-remove-by-id "delete-user" users userid))

;;-----------------------------------------------------------------------------

(defn set-user-name
    [uid name]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/string name)]}
    (mc-update-by-id "set-user-name" users uid {$set {:username name}}))

(defn set-user-password
    [uid passwd]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/password passwd)]}
    (mc-update-by-id "set-user-password" users uid
                     {$set {:password passwd}}))
                     ; {$set {:password (creds/hash-bcrypt (verify-passwd passwd))}}))

;;-----------------------------------------------------------------------------

(defn set-user-roles
	[uid roles]
 	{:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/roles roles)]}
	(mc-update-by-id "set-user-roles" users uid {$set {:roles roles}}))

;;-----------------------------------------------------------------------------

(defn set-user-property
	[uid prop-key prop-val]
 	{:pre [(utils/valid? :shop/_id uid)
           (utils/valid? keyword? prop-key)
           (utils/valid? map? prop-val)]}
    (let [props (:properties (get-user-by-id uid))]
        (mc-update-by-id "set-user-property" users uid
		{$set {:properties (assoc props prop-key prop-val)}})))

;;-----------------------------------------------------------------------------
