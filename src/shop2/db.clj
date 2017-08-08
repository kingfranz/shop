(ns shop2.db
	(:require 	(shop2				[utils			:refer :all])
   				(clj-time			[core     		:as t]
            						[local    		:as l]
            						[coerce   		:as c]
            						[format   		:as f]
            						[periodic 		:as p])
            	(clojure 			[set      		:as set]
            						[pprint   		:as pp]
            						[string   		:as str])
            	(clojure.spec 		[alpha          :as s])
             	(cheshire 			[core     		:refer :all])
				(cemerick 			[friend      	:as friend])
            	(cemerick.friend 	[workflows 	 	:as workflows]
                             		[credentials 	:as creds])
            	(taoensso 			[timbre   		:as log])
            	(monger 			[core     		:as mg]
            						[credentials 	:as mcr]
            						[collection 	:as mc]
            						[joda-time  	:as jt]
            						[operators 		:refer :all])
             	(environ 			[core 			:refer [env]])
            	(shop2 				[utils       	:as utils]
            						[spec       	:as spec])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

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

;;-----------------------------------------------------------------------------

(defn mk-id
	[]
 	{:post [(q-valid? :shop/_id %)]}
	(str (java.util.UUID/randomUUID)))

(defn mk-std-field
	[]
 	{:post [(q-valid? :shop/std-keys %)]}
	{:_id (mk-id) :created (utils/now)})

;;-----------------------------------------------------------------------------

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
 	{:post [(q-valid? :shop/string %)]}
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
 	{:pre [(q-valid? :shop/string en)]
     :post [(q-valid? :shop/string %)]}
	(-> en str/trim str/lower-case (str/replace #"[ \t-]+" " ")))

(defn get-by-enlc
	[tbl en]
 	{:pre [(q-valid? :shop/string tbl) (q-valid? :shop/string en)]
     :post [(q-valid? (s/nilable map?) %)]}
	(mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(defn add-item-usage
	[list-id item-id action numof]
 	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id) (q-valid? keyword? action) (q-valid? number? numof)]
     :post [(q-valid? map? %)]}
	(mc-insert "add-item-usage" item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

(defn get-user
	[uname]
 	{:pre [(q-valid? :shop/username uname)]
     :post [(q-valid? (s/nilable :shop/user-db) %)]}
	(let [udata (mc-find-one-as-map "get-user" users
					{:username {$regex (str "^" (str/trim uname) "$") $options "i"}})]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(defn get-user-by-id
	[uid]
 	{:pre [(q-valid? :shop/_id uid)]
     :post [(q-valid? (s/nilable :shop/user-db) %)]}
	(let [udata (mc-find-map-by-id "get-user-by-id" users uid)]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(defn get-users
	[]
 	{:post [(q-valid? (s/* :shop/user-db) %)]}
	(for [user (mc-find-maps "get-user" users {})]
		(-> user
			(update :roles #(->> % (map keyword) set))
			(update :created str))))

;;-----------------------------------------------------------------------------

(defn count-chars
	[pw c-class]
	(if (nil? (re-find c-class pw))
		(throw (ex-info (str "PW must contain at least one of " c-class) {:cause :password}))
		pw))

(defn verify-passwd
	[pw]
	(if (not= (str/trim pw) pw)
		(throw (ex-info "PW can't begin or end with space" {:cause :password}))
		(if (< (count pw) 8)
			(throw (ex-info "PW must be 8 chars or more" {:cause :password}))
			(-> pw
				(count-chars #"[a-zåäö]")
				(count-chars #"[A-ZÅÄÖ]")
				(count-chars #"[0-9]")
				(count-chars #"[.*!@#$%^&()=+-]")
				))))

(defn create-user
	[username passwd roles]
 	{:pre [(q-valid? :shop/username username) (q-valid? :shop/password passwd) (q-valid? :shop/roles roles)]
     :post [(q-valid? :shop/user-db %)]}
	(when (some? (get-user username))
		(throw (ex-info "duplicate username" {:cause :username})))
	(let [user (merge {:username (str/trim username)
					   :password (creds/hash-bcrypt (verify-passwd passwd))
					   :roles    roles} (mk-std-field))]
		(mc-insert "create-user" users user)
		user))

;;-----------------------------------------------------------------------------

(defn set-user-password
	[uid passwd]
 	{:pre [(q-valid? :shop/_id uid) (q-valid? :shop/password passwd)]}
	(mc-update-by-id "set-user-password" users uid
		{$set {:password (creds/hash-bcrypt (verify-passwd passwd))}}))

;;-----------------------------------------------------------------------------

(defn set-user-roles
	[uid roles]
 	{:pre [(q-valid? :shop/_id uid) (q-valid? :shop/roles roles)]}
	(mc-update-by-id "set-user-roles" users uid {$set {:roles roles}}))

;;-----------------------------------------------------------------------------

(defn set-user-property
	[uid prop-key prop-val]
 	{:pre [(q-valid? :shop/_id uid) (q-valid? keyword? prop-key) (q-valid? map? prop-val)]}
	(mc-update-by-id "set-user-property" users uid
		{$set {:properties {prop-key prop-val}}}))

;;-----------------------------------------------------------------------------

;{:us u :pw p :properties {:home {:list-type :tree} :lists {}}}
