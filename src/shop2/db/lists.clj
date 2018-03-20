(ns shop2.db.lists
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
                 [hiccup.form :as hf]
                 [taoensso.timbre :as log]
                 [monger.core :as mg]
                 [monger.credentials :as mcr]
                 [monger.collection :as mc]
                 [monger.joda-time :as jt]
                 [monger.operators :refer :all]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.items :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn get-list
    [listid]
    {:pre [(utils/valid? :shop/_id listid)]
     :post [(utils/valid? :shop/list %)]}
    (mc-find-one-as-map "get-list" lists {:_id listid}))

(defn get-list-by-name
    [list-name]
    {:pre [(utils/valid? :shop/string list-name)]
     :post [(utils/valid? :shop/list %)]}
    (mc-find-one-as-map "get-list" lists {:entryname list-name}))

(defn get-lists
	[]
	{:post [(utils/valid? :shop/lists %)]}
	(mc-find-maps "get-lists" lists))

(defn get-list-names
	[]
	{:post [(utils/valid? :shop/strings %)]}
	(mc-find-maps "get-list-names" lists {} {:_id true :entryname true}))

(defn get-top-list
	[a-list]
	{:pre [(utils/valid? :shop/list a-list)]
	 :post [(utils/valid? :shop/list %)]}
	(if (nil? (:parent a-list))
		a-list
		(get-top-list (get-list (:parent a-list)))))

(defn get-top-lists
	[]
	{:post [(utils/valid? :shop/lists %)]}
	(mc-find-maps "get-top-lists" lists {:parent nil}))

(defn get-sub-lists
	[listid]
	{:pre [(utils/valid? :shop/_id listid)]
	 :post [(utils/valid? :shop/lists %)]}
	(mc-find-maps "get-sub-lists" lists {:parent._id listid}))

(defn get-lists-dd
    []
    (->> (get-lists)
         (sort-by :entryname)
         (map (fn [l] [(:entryname l) (:_id l)]))
         (concat [["" no-id]])))

(defn mk-list-dd
    [current-id dd-name dd-class]
    (hf/drop-down {:class dd-class} dd-name (get-lists-dd) current-id))

(defn list-id-exists?
	[id]
	{:pre [(utils/valid? :shop/_id id)]}
	(= (get (mc-find-map-by-id "list-id-exists?" lists id {:_id true}) :_id) id))

(defn add-list
	[entry]
	{:pre [(utils/valid? :shop/list* entry)]
	 :post [(utils/valid? :shop/list %)]}
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-list" lists entry*)
		entry*))

(defn update-list
	[entry]
	{:pre [(utils/valid? :shop/list* entry)]}
	(mc-update-by-id "update-list" lists (:_id entry)
		{$set (select-keys entry [:entryname :parent :last])}))

(defn delete-list
	[list-id]
	{:pre [(utils/valid? :shop/_id list-id)]}
	(mc-remove-by-id "delete-list" lists list-id)
	(doseq [mlist (mc-find-maps "delete-list" lists {} {:_id true :parent true})
	  :let [np (some->> mlist :parent :parent)]
	  :when (= (some->> mlist :parent :_id) list-id)]
		(mc-update-by-id "delete-list" lists (:_id mlist) {$set {:parent np}})))

(defn get-lists-with-count
	[]
	(mc-aggregate "get-lists-with-count" lists
		[{$project {:_id true
		 		    :entryname true
		 		    :parent true
         			:last true
		 		    :count {
		 		    	$cond [{$gt ["$items" nil]}
					           {$size {
					           		"$filter" {
					           			:input "$items"
					                    :as "item"
					                    :cond {$not [{$gt ["$$item.finished" nil]}]}}}}
					           0]}}}]))

(defn find-item
    [list-id item-id]
    {:pre [(utils/valid? :shop/_id list-id)
           (utils/valid? :shop/_id item-id)]}
    (some->> (mc-find-one-as-map "find-item" lists {:_id list-id :items._id item-id} {:items.$ 1})
             :items
             first))

(defn- item-finished?
    [list-id item-id]
    {:pre [(utils/valid? :shop/_id list-id) (utils/valid? :shop/_id item-id)]}
    (-> (find-item list-id item-id) :finished some?))

(defn finish-list-item
	[list-id item-id]
	{:pre [(utils/valid? :shop/_id list-id) (utils/valid? :shop/_id item-id)]}
	(add-item-usage list-id item-id :finish 0)
	(mc-update "finish-list-item" lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished (l/local-now)}}))

(defn unfinish-list-item
	[list-id item-id]
	{:pre [(utils/valid? :shop/_id list-id) (utils/valid? :shop/_id item-id)]}
	(add-item-usage list-id item-id :unfinish 0)
	(mc-update "unfinish-list-item" lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished nil :items.$.numof 1}}))

(defn del-finished-list-items
	[list-id]
	{:pre [(utils/valid? :shop/_id list-id)]}
	(let [a-list (get-list list-id)
		  clean (remove #(some? (:finished %)) (:items a-list))]
		(mc-update-by-id "del-finished-list-items" lists list-id
			{$set {:items clean}})))

(defn- remove-item
	[list-id item-id]
	(add-item-usage list-id item-id :remove 0)
	(mc-update "remove-item" lists
		{:_id list-id}
		{$pull {:items {:_id item-id}}}))

(defn- mod-item
	[list-id item-id num-of]
	(add-item-usage list-id item-id :mod num-of)
	(mc-update "mod-item" lists
		{:_id list-id :items._id item-id}
		{$inc {:items.$.numof num-of}}))

(defn find-list-by-name
	[e-name]
	{:pre [(utils/valid? :shop/string e-name)]
	 :post [(utils/valid? :shop/list %)]}
	(mc-find-one-as-map "find-list-by-name" lists {:entryname e-name}))

(defn list-item+
    [list-id item-id]
    {:pre [(utils/valid? :shop/_id list-id)
           (utils/valid? :shop/_id item-id)]}
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "list-item+"})))
    ; make sure the item is already in the list
    (if (find-item list-id item-id)
        ; yes it was
        (mod-item list-id item-id 1)
        ; no
        (throw+ (ex-info "unknown item" {:type :db :src "list-item+"}))))

(defn list-item-
    [list-id item-id]
    {:pre [(utils/valid? :shop/_id list-id)
           (utils/valid? :shop/_id item-id)]}
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "list-item-"})))
    ; make sure the item is already in the list
    (if-let [item (find-item list-id item-id)]
        ; yes it was
        (if (= (:numof item) 1)
            (finish-list-item list-id item-id)
            (mod-item list-id item-id -1))
        ; no
        (throw+ (ex-info "unknown item" {:type :db :src "list-item-"}))))

(defn item->list
    [list-id item-id]
	{:pre [(utils/valid? :shop/_id list-id)
		   (utils/valid? :shop/_id item-id)]}
	; make sure it's a valid list
	(when-not (list-id-exists? list-id)
		(throw+ (ex-info "unknown list" {:type :db :src "item->list"})))
	; find the item if it's already in the list
	(if (find-item list-id item-id)
		; yes it was
        (if (item-finished? list-id item-id)
            (unfinish-list-item list-id item-id)
            (throw+ (ex-info "item already in list" {:type :db :src "item->list"})))
		; no, we need to add it
		(do
			(add-item-usage list-id item-id :add-to 1)
			(mc-update-by-id "item->list" lists list-id
				{$addToSet {:items (assoc (get-item item-id) :numof 1)}}))))

