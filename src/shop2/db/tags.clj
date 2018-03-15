(ns shop2.db.tags
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
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
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn get-tags
	[]
	{:post [(utils/valid? :shop/tags %)]}
	(mc-find-maps "get-tags" tags))

(defn get-tag
	[id]
	{:pre [(utils/valid? :shop/_id id)]
	 :post [(utils/valid? :shop/tag %)]}
	(mc-find-map-by-id "get-tag" tags id))

(defn get-tag-names
	[]
	{:post [(utils/valid? :shop/strings %)]}
	(mc-find-maps "get-tag-names" tags {} {:_id true :entryname true}))

(defn update-tag
	[tag-id tag-name*]
	{:pre [(utils/valid? :shop/_id tag-id)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)]
		(if (some? db-tag)
			(if (= (:_id db-tag) tag-id)
				db-tag
				(throw (ex-info "duplicate name" {:cause :dup})))
			(mc-update-by-id "update-tag" tags (:_id tag-id)
				{$set {:entryname tag-name :entrynamelc tag-namelc}}))))

(defn add-tag
	[tag-name*]
	{:pre [(utils/valid? :shop/string tag-name*)]
	 :post [(utils/valid? :shop/tag %)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)
		  new-tag    (merge {:entryname tag-name
		  					 :entrynamelc tag-namelc} (mk-std-field))]
		(if (some? db-tag)
			db-tag
			(do
				(mc-insert "add-tag" tags new-tag)
				new-tag))))

(defn add-tags
	[tags*]
	{:pre [(utils/valid? :shop/tags* tags*)]}
	(map #(add-tag (:entryname %)) tags*))

(defn delete-tag
	[id]
	{:pre [(utils/valid? :shop/_id id)]}
	(mc-remove-by-id "delete-tag" tags id))

(defn delete-tag-all
	[id]
	{:pre [(utils/valid? :shop/_id id)]}
	(delete-tag id)
	(mc-update "delete-tag-all" lists    {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" recipes  {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" menus    {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" projects {} {$pull {:tags {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" items    {} {$pull {:tags {:_id id}}} {:multi true})
	)
