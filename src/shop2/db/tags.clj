(ns shop2.db.tags
    (:require [clj-time.core :as t]
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

(defn get-tags-dd
    []
    (->> (get-tags)
         (sort-by :entryname)
         (map (fn [l] [(:entryname l) (:_id l)]))
         (concat [["" no-id]])))

(defn get-tag-names
	[]
	{:post [(utils/valid? :shop/strings %)]}
	(mc-find-maps "get-tag-names" tags {} {:_id true :entryname true}))

(defn- get-listid-by-name
    [list-name]
    {:pre [(utils/valid? :shop/string list-name)]
     :post [(utils/valid? :shop/_id %)]}
    (mc-find-one-as-map "get-list" lists {:entryname list-name} {:_id true}))

(defn- fix-list-ref
    [lst]
    {:pre [(utils/valid? (s/nilable string?) lst)]}
    (cond
        (str/blank? lst)         nil
        (s/valid? :shop/_id lst) lst
        :else                    (get-listid-by-name lst)))

(defn update-tag
	[tag-id tag-name* parent*]
	{:pre [(utils/valid? :shop/_id tag-id)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)
          parent     (fix-list-ref parent*)]
        (when (or (str/blank? tag-name) (str/includes? tag-name " "))
            (throw+ (ex-info "update-tag: invalid name" {:type :db})))
		(when (and (some? db-tag) (not= (:_id db-tag) tag-id))
			(throw+ (ex-info "duplicate name" {:type :db})))
        (mc-update-by-id "update-tag" tags tag-id
			{$set {:entryname tag-name :entrynamelc tag-namelc :parent parent}})))

(defn add-tag
    ([tag-name*] (add-tag tag-name* nil))
    ([tag-name* parent*]
    {:pre [(utils/valid? :shop/string tag-name*)]
	 :post [(utils/valid? :shop/tag %)]}
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc tags tag-namelc)
          parent     (fix-list-ref parent*)
		  new-tag    (merge {:entryname tag-name
		  					 :entrynamelc tag-namelc
                             :parent parent}
                            (mk-std-field))]
		(if (some? db-tag)
			db-tag
			(do
				(mc-insert "add-tag" tags new-tag)
				new-tag)))))

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
