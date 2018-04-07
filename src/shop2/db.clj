(ns shop2.db
    (:require [shop2.extra :refer :all]
              [shop2.spec :refer :all]
              [utils.core :as utils]
              [slingshot.slingshot :refer [throw+ try+]]
              [taoensso.timbre :as log]
              [clj-time.local :as l]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
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

(defonce db-conn (mg/connect-with-credentials (env :database-ip)
							(mcr/create (env :database-user)
                   						(env :database-db)
                         				(env :database-pw)
                             )))
(defonce shopdb (mg/get-db db-conn (env :database-db)))

(defonce no-id "00000000-0000-0000-0000-000000000000")

;;-----------------------------------------------------------------------------

(defn-spec mk-id :shop/_id
	[]
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
          txt  (if (> (count txt*) 500) (str (doall (take 500 txt*)) " ## and much more") txt*)]
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

(defn-spec mk-enlc :shop/entrynamelc
	[en :shop/entryname]
 	(-> en str/trim str/lower-case (str/replace anti-lcname-regex "")))

(defn-spec update-enlc (s/keys :req-un [:shop/entryname :shop/entrynamelc])
    [entity (s/keys :req-un [:shop/entryname])]
    (assoc entity :entrynamelc (mk-enlc (:entryname entity))))

(defn-spec set-name (s/keys :req-un [:shop/entryname :shop/entrynamelc])
    [entity map?, ename :shop/entryname]
    (->> ename
         str/trim
         (assoc entity :entryname)
         (update-enlc)))

(defn-spec create-entity (s/keys :req-un [:shop/_id :shop/created :shop/entryname :shop/entrynamelc])
    [ename :shop/string]
    (-> (mk-std-field)
        (set-name ename)))

(defn-spec get-by-enlc (s/nilable map?)
	[tbl :shop/string, en :shop/string]
 	(mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(defn-spec add-item-usage any?
	[list-id (s/nilable :shop/_id), item-id :shop/_id, action keyword?, numof integer?]
 	(mc-insert "add-item-usage" "item-usage"
		(assoc (mk-std-field)
            :listid list-id
            :itemid item-id
            :action action
            :numof numof)))

;;-----------------------------------------------------------------------------

(st/instrument)
