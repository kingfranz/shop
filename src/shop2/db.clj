(ns shop2.db
	(:require 	(clj-time			[core     		:as t]
            						[local    		:as l]
            						[coerce   		:as c]
            						[format   		:as f]
            						[periodic 		:as p])
            	(clojure 			[set      		:as set]
            						[pprint   		:as pp]
            						[spec     		:as s]
            						[string   		:as str])
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
            	(shop2 				[utils       	:as utils]
            						[spec       	:as spec])
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

(s/fdef mk-id :ret :shop/_id)

(defn mk-id
	[]
	(str (java.util.UUID/randomUUID)))

(s/fdef mk-std-field :ret :shop/std-keys)

(defn mk-std-field
	[]
	{:_id (mk-id) :created (utils/now)})

;;-----------------------------------------------------------------------------

(s/fdef fname
	:args (s/cat :s :shop/string)
	:ret :shop/string)

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
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

(s/fdef mk-enlc :args :shop/string :ret :shop/string)

(defn mk-enlc
	[en]
	(-> en str/trim str/lower-case (str/replace #"[ \t-]+" " ")))

(s/fdef get-by-enlc
	:args (s/cat :tbl :shop/string :en :shop/string)
	:ret (s/nilable map?))

(defn get-by-enlc
	[tbl en]
	(mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(s/fdef add-item-usage
	:args (s/cat :list-id :shop/_id
				 :item-id :shop/_id
				 :action  keyword?
				 :numof   number?)
	:ret map?)

(defn add-item-usage
	[list-id item-id action numof]
	(mc-insert "add-item-usage" item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------

(s/fdef get-user
	:args :shop/username
	:ret  (s/nilable :shop/user-db))

(defn get-user
	[uname]
	(let [udata (mc-find-one-as-map "get-user" users
					{:username {$regex (str "^" (str/trim uname) "$") $options "i"}})]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(s/fdef get-user-by-id
	:args :shop/_id
	:ret  (s/nilable :shop/user-db))

(defn get-user-by-id
	[uid]
	(let [udata (mc-find-map-by-id "get-user-by-id" users uid)]
		(when (seq udata)
			(-> udata
				(update :roles #(->> % (map keyword) set))
				(update :created str)))))

;;-----------------------------------------------------------------------------

(s/fdef get-users
	:ret  :shop/user-db)

(defn get-users
	[]
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

(s/fdef create-user
	:args (s/cat :username :shop/username
				 :passwd   :shop/password
				 :roles    :shop/roles)
	:ret :shop/user-db)

(defn create-user
	[username passwd roles]
	(when (some? (get-user username))
		(throw (ex-info "duplicate username" {:cause :username})))
	(let [user (merge {:username (str/trim username)
					   :password (creds/hash-bcrypt (verify-passwd passwd))
					   :roles    roles} (mk-std-field))]
		(mc-insert "create-user" users user)
		user))

;;-----------------------------------------------------------------------------

(s/fdef set-user-password
	:args (s/cat :uid    :shop/_id
				 :passwd :shop/password))

(defn set-user-password
	[uid passwd]
	(mc-update-by-id "set-user-password" users uid
		{$set {:password (creds/hash-bcrypt (verify-passwd passwd))}}))

;;-----------------------------------------------------------------------------

(s/fdef set-user-roles
	:args (s/cat :uid   :shop/_id
				 :roles :shop/roles))

(defn set-user-roles
	[uid roles]
	(mc-update-by-id "set-user-roles" users uid {$set {:roles roles}}))

;;-----------------------------------------------------------------------------

(s/fdef set-user-property
	:args (s/cat :uid      :shop/_id
				 :prop-key keyword?
				 :prop-val map?))

(defn set-user-property
	[uid prop-key prop-val]
	(mc-update-by-id "set-user-property" users uid
		{$set {:properties {prop-key prop-val}}}))

;;-----------------------------------------------------------------------------

;{:us u :pw p :properties {:home {:list-type :tree} :lists {}}}
