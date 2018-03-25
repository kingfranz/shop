(ns shop2.db.items
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
                 [shop2.conformer :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn get-item-names
	[]
	(mc-find-maps "get-item-names" items {} {:_id true :entryname true}))

(defn get-items
	[]
	{:post [(utils/valid? :shop/items %)]}
    (map conform-item (mc-find-maps "get-items" items {})))

(defn get-item
	[id]
	{:pre [(utils/valid? :shop/_id id)]
	 :post [(utils/valid? :shop/item %)]}
    (conform-item (mc-find-one-as-map "get-item" items {:_id id})))

(defn item-id-exists?
	[id]
	{:pre [(utils/valid? :shop/_id id)]}
	(= (get (mc-find-map-by-id "item-id-exists?" items id {:_id true}) :_id) id))

(defn add-item
	[entry]
	{:pre [(utils/valid? :shop/item entry)]
	 :post [(utils/valid? :shop/item %)]}
    (when (:tag entry)
        (add-tag (-> entry :tag :entryname)))
	(add-item-usage nil (:_id entry) :create 0)
	(mc-insert "add-item" items entry)
	entry)

(defn update-item
	[entry]
	{:pre [(utils/valid? :shop/item entry)]}
    (add-item-usage nil (:_id entry) :update 0)
    (mc-replace-by-id "update-item" items entry))

(defn delete-item
	[item-id]
	{:pre [(utils/valid? :shop/_id item-id)]}
	(add-item-usage nil item-id :delete 0)
	(mc-remove-by-id "delete-item" items item-id))

