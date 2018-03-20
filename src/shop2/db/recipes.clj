(ns shop2.db.recipes
	(:require 	[clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 [clojure.spec.alpha :as s]
                 [cheshire.core :refer :all]
                 [taoensso.timbre :as log]
                 [monger.core :as mg]
                 [monger.credentials :as mcr]
                 [monger.collection :as mc]
                 [monger.joda-time :as jt]
                 [monger.operators :refer :all]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.tags :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn get-recipe-names
	[]
	(mc-find-maps "get-recipe-names" recipes {} {:_id true :entryname true}))

(defn get-recipes
	[]
	{:post [(utils/valid? :shop/recipes %)]}
	(mc-find-maps "get-recipes" recipes))

(defn get-recipe
	[id]
	{:pre [(utils/valid? :shop/_id id)]
	 :post [(utils/valid? :shop/recipe %)]}
	(mc-find-one-as-map "get-recipe" recipes {:_id id}))

(defn add-recipe
	[entry]
	{:pre [(utils/valid? :shop/recipe* entry)]
	 :post [(utils/valid? :shop/recipe %)]}
	(add-tags (:tags entry))
	(let [entrynamelc (mk-enlc (:entryname entry))
		  db-entry (get-by-enlc recipes entrynamelc)
		  entry* (-> entry
		  			 (merge {:entrynamelc entrynamelc} (mk-std-field))
		  			 (update :entryname str/trim))]
		(if (some? db-entry)
			db-entry
			(do
				(mc-insert "add-recipe" recipes entry*)
				entry*))))

(defn update-recipe
	[recipe*]
	{:pre [(utils/valid? :shop/recipe* recipe*)]}
	(let [entrynamelc (mk-enlc (:entryname recipe*))
		  recipe (-> recipe*
		  			 (assoc :entrynamelc entrynamelc)
		  			 (update :entryname str/trim))
		  db-entry (get-by-enlc recipes entrynamelc)]
		(if (some? db-entry)
			(if (= (:_id db-entry) (:_id recipe))
				(mc-update-by-id "update-recipe" recipes (:_id recipe)
					{$set (select-keys recipe [:url :items :text])})
				(throw+ (ex-info "duplicate name" {:cause "dup"})))
			(do
				(mc-update-by-id "update-recipe" recipes (:_id recipe)
					{$set (select-keys recipe [:entryname :entrynamelc :url :items :text])})
				; now update the recipe in menus
				(mc-update "update-recipe" menus {:recipe._id (:_id recipe)}
					{$set {:recipe (select-keys recipe [:_id :entryname])}}
					{:multi true})))
		recipe))

