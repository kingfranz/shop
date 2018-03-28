(ns shop2.db
    (:require [shop2.extra :refer :all]
              [shop2.spec :refer :all]
              [utils.core :as utils]
              [slingshot.slingshot :refer [throw+ try+]]
              [taoensso.timbre :as log]
              [clj-time.local :as l]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [cheshire.core :refer :all]
              [monger.core :as mg]
              [monger.credentials :as mcr]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [environ.core :refer [env]]
              )
    (:import [java.util UUID]))

;;-----------------------------------------------------------------------------

; mongo --port 27017 -u "mongoadmin" -p "Benq.fp731" --authenticationDatabase "admin"
; db.createUser({user:"shopper",pwd:"kAllE.kUlA399",roles:[{role:"readWrite",db:"shopdb"}]})

(def db-conn (mg/connect-with-credentials (env :database-ip)
							(mcr/create (env :database-user)
                   						(env :database-db)
                         				(env :database-pw)
                             )))
(def shopdb (mg/get-db db-conn (env :database-db)))

(def no-id "00000000-0000-0000-0000-000000000000")

;;-----------------------------------------------------------------------------

(defn mk-id
	[]
 	{:post [(utils/valid? :shop/_id %)]}
	(str (UUID/randomUUID)))

(defn mk-std-field
	[]
 	{:_id (mk-id) :created (l/local-now)})

;;-----------------------------------------------------------------------------

;monger.collection$find_one_as_map@5f2b4e24users
(defn fname
	[s]
 	{:post [(utils/valid? :shop/string %)]}
	(second (re-matches #"^[^$]+\$(.+)@.+$" (str s))))

(defn- do-mc
	[mc-func caller tbl & args]
	(log/trace (apply str caller ": " (fname mc-func) " " tbl " " (first args) "\n"))
	(let [ret (apply mc-func shopdb tbl (first args))
          txt* (pr-str ret)
          txt  (if (> (count txt*) 500) (str (take 500 txt*) " ## and much more") txt*)]
		(log/trace caller "returned:" txt "\n")
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

(defn mc-replace-by-id
    [func tbl & args]
    (do-mc mc/save func tbl args))

(defn mc-remove-by-id
	[func tbl & args]
	(do-mc mc/remove-by-id func tbl args))

;;-----------------------------------------------------------------------------

(defn mk-enlc
	[en]
 	{:pre [(utils/valid? :shop/string en)]
     :post [(utils/valid? :shop/entrynamelc %)]}
	(-> en str/trim str/lower-case (str/replace anti-lcname-regex "")))

(defn update-enlc
    [entity]
    (assoc entity :entrynamelc (mk-enlc (:entryname entity))))

(defn set-name
    [entity name]
    (->> name
         str/trim
         (s/assert :shop/string)
         (assoc entity :entryname)
         (update-enlc)))

(defn create-entity
    [ename]
    (-> (mk-std-field)
        (set-name ename)))

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
	(mc-insert "add-item-usage" "item-usage"
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

;;-----------------------------------------------------------------------------
