(ns shop2.db.items
	(:require 	[clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clojure.spec.alpha :as s]
                 [orchestra.core :refer [defn-spec]]
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

(defn-spec create-item-obj :shop/item
    "create a new item"
    [iname :shop/entryname
     parent :shop/parent
     tag :item/tag
     proj :item/project
     url :shop/url
     price :item/price
     oneshot :item/oneshot]
    (-> (create-entity iname)
        (assoc :parent  parent
               :tag     tag
               :project proj
               :url     url
               :price   price
               :oneshot oneshot)))

(defn-spec get-item-names (s/* (s/keys :req-un [:shop/_id :shop/entryname]))
	[]
	(mc-find-maps "get-item-names" "items" {} {:_id true :entryname true}))

(defn-spec get-items :shop/items
	[]
	(map conform-item (mc-find-maps "get-items" "items" {})))

(defn-spec get-item :shop/item
	[id :shop/_id]
	(conform-item (mc-find-one-as-map "get-item" "items" {:_id id})))

(defn-spec item-id-exists? boolean?
	[id :shop/_id]
	(= (get (mc-find-map-by-id "item-id-exists?" "items" id {:_id true}) :_id) id))

(defn-spec add-item :shop/item
	[entry :shop/item]
	(when (:tag entry)
        (add-tag (-> entry :tag :entryname)))
	(add-item-usage nil (:_id entry) :create 0)
	(mc-insert "add-item" "items" entry)
	entry)

(defn-spec update-item any?
	[entry :shop/item]
	(add-item-usage nil (:_id entry) :update 0)
    (mc-replace-by-id "update-item" "items" entry))

(defn-spec delete-item any?
	[item-id :shop/_id]
	(add-item-usage nil item-id :delete 0)
	(mc-remove-by-id "delete-item" "items" item-id))

