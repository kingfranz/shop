(ns shop2.db.lists
    (:require 	 [shop2.extra :refer :all]
                  [shop2.db :refer :all]
                  [shop2.db.items :refer :all]
                  [shop2.conformer :refer :all]
                  [clj-time.local :as l]
                  [slingshot.slingshot :refer [throw+ try+]]
                  [clojure.spec.alpha :as s]
                  [orchestra.core :refer [defn-spec]]
                  [orchestra.spec.test :as st]
                  [cheshire.core :refer :all]
                  [hiccup.form :as hf]
                  [taoensso.timbre :as log]
                  [monger.operators :refer :all]
                  [utils.core :as utils]
                  ))

;;-----------------------------------------------------------------------------

(defn-spec create-list-obj :shop/list
           [lst-name :shop/entryname, parent :shop/parent, last? boolean?]
           (-> (create-entity lst-name)
               (assoc :items  []
                      :parent parent
                      :last   last?)))

(defn-spec get-list :shop/list
           [listid :shop/_id]
           (mc-find-one-as-map "get-list" "lists" {:_id listid}))

(defn-spec get-list-by-name :shop/list
           [list-name :shop/entryname]
           (mc-find-one-as-map "get-list" "lists" {:entryname list-name}))

(defn-spec get-lists :shop/lists
           []
           (mc-find-maps "get-lists" "lists"))

(defn-spec get-list-names (s/* (s/keys :req-un [:shop/_id :shop/entryname]))
           []
           (mc-find-maps "get-list-names" "lists" {} {:_id true :entryname true}))

(defn-spec get-top-list :shop/list
           [a-list :shop/list]
           (if (nil? (:parent a-list))
               a-list
               (get-top-list (get-list (:parent a-list)))))

(defn-spec get-lists-dd (s/coll-of (s/cat :str string? :id :shop/_id))
           []
           (->> (get-lists)
                (sort-by :entryname)
                (map (fn [l] [(:entryname l) (:_id l)]))
                (concat [["" no-id]])))

(defn-spec mk-list-dd any?
           [current-id :shop/_id, dd-name keyword?, dd-class string?]
           (hf/drop-down {:class dd-class} dd-name (get-lists-dd) current-id))

(defn-spec list-id-exists? boolean?
           [id :shop/_id]
           (= (get (mc-find-map-by-id "list-id-exists?" "lists" id {:_id true}) :_id) id))

(defn-spec add-list :shop/list
           [entry :shop/list]
           (mc-insert "add-list" "lists" entry)
           entry)

(defn-spec update-list any?
           [entry :shop/list]
           (mc-replace-by-id "update-list" "lists" entry))

(defn-spec purge-parent :list/parent
           [lst-parent :list/parent, to-remove-id :shop/_id]
           (when-not (nil? lst-parent)
               (if (= (:_id lst-parent) to-remove-id)
                   (purge-parent (:parent lst-parent) to-remove-id)
                   (update lst-parent :parent #(purge-parent (:parent lst-parent) to-remove-id)))))

(defn-spec delete-list any?
           [list-id :shop/_id]
           (mc-remove-by-id "delete-list" "lists" list-id)
           (doseq [mlist (mc-find-maps "delete-list" "lists" {} {:_id true :parent true})
                   :let [np (purge-parent (:parent mlist) list-id)]
                   :when (not= (:parent mlist) np)]
               (mc-update-by-id "delete-list" "lists" (:_id mlist) {$set {:parent np}})))

(s/def :list/count integer?)
(s/def :list/total number?)
(s/def :shop/list-with-count (s/keys :req-un [:shop/_id :shop/entryname :list/parent :list/last :list/count :list/total]))
(s/def :shop/lists-with-count (s/* :shop/list-with-count))

(defn-spec get-lists-with-count :shop/lists-with-count
           []
           (mc-aggregate "get-lists-with-count" "lists"
                         [{$project {:entryname true
                                     :parent    true
                                     :last      true
                                     :items     {"$filter" {:input "$items" :cond {"$eq" ["$$this.finished" nil]}}}}}
                          {$project {:entryname true
                                     :parent    true
                                     :last      true
                                     :items     {"$map" {:input "$items" :in {"$multiply" ["$$this.price" "$$this.numof"]}}}}}
                          {$project {:entryname true
                                     :parent    true
                                     :last      true
                                     :count     {"$size" "$items"}
                                     :total     {"$sum" "$items"}}}]))

(defn-spec find-list-item-by-id (s/nilable :list/item)
           [list-id :shop/_id, item-id :shop/_id]
           (some->> (mc-find-one-as-map "find-item" "lists" {:_id list-id :items._id item-id} {:items.$ 1})
                    :items
                    first))

(defn-spec find-list-item-by-name (s/nilable :list/item)
           [list-id :shop/_id, item-name :shop/entryname]
           (some->> (mc-find-one-as-map "find-item" "lists" {:_id list-id :items.entryname item-name} {:items.$ 1})
                    :items
                    first))

(defn-spec item-finished? boolean?
           [list-id :shop/_id, item-id :shop/_id]
           (-> (find-list-item-by-id list-id item-id) :finished some?))

(defn-spec finish-list-item any?
    [list-id :shop/_id, item-id :shop/_id]
    (add-item-usage list-id item-id :finish 0)
    (mc-update "finish-list-item" "lists"
               {:_id list-id :items._id item-id}
               {$set {:items.$.finished (l/local-now)}}))

(defn-spec unfinish-list-item any?
    [list-id :shop/_id, item-id :shop/_id]
    (add-item-usage list-id item-id :unfinish 0)
    (mc-update "unfinish-list-item" "lists"
               {:_id list-id :items._id item-id}
               {$set {:items.$.finished nil :items.$.numof 1}}))

(defn-spec del-finished-list-items any?
    [list-id :shop/_id]
    (let [a-list (get-list list-id)
          clean (remove #(some? (:finished %)) (:items a-list))]
        (mc-update-by-id "del-finished-list-items" "lists" list-id
                         {$set {:items clean}})))

(defn-spec remove-item any?
    [list-id :shop/_id, item-id :shop/_id]
    (add-item-usage list-id item-id :remove 0)
    (mc-update "remove-item" "lists"
               {:_id list-id}
               {$pull {:items {:_id item-id}}}))

(defn-spec mod-item any?
    [list-id :shop/_id, item-id :shop/_id, num-of integer?]
    (add-item-usage list-id item-id :mod num-of)
    (mc-update "mod-item" "lists"
               {:_id list-id :items._id item-id}
               {$inc {:items.$.numof num-of}}))

(defn-spec find-list-by-name :shop/list
    [e-name :shop/entryname]
    (mc-find-one-as-map "find-list-by-name" "lists" {:entryname e-name}))

(defn-spec list-item+ any?
    [list-id :shop/_id, item-id :shop/_id]
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "list-item+"})))
    ; make sure the item is already in the list
    (if (find-list-item-by-id list-id item-id)
        ; yes it was
        (mod-item list-id item-id 1)
        ; no
        (throw+ (ex-info "unknown item" {:type :db :src "list-item+"}))))

(defn-spec list-item- any?
    [list-id :shop/_id, item-id :shop/_id]
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "list-item-"})))
    ; make sure the item is already in the list
    (if-let [item (find-list-item-by-id list-id item-id)]
        ; yes it was
        (if (= (:numof item) 1)
            (finish-list-item list-id item-id)
            (mod-item list-id item-id -1))
        ; no
        (throw+ (ex-info "unknown item" {:type :db :src "list-item-"}))))

(defn-spec item->list any?
    [list-id :shop/_id, item-id :shop/_id]
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "item->list"})))
    ; find the item if it's already in the list
    (if (find-list-item-by-id list-id item-id)
        ; yes it was
        (if (item-finished? list-id item-id)
            (unfinish-list-item list-id item-id)
            (throw+ (ex-info "item already in list" {:type :db :src "item->list"})))
        ; no, we need to add it
        (do
            (add-item-usage list-id item-id :add-to 1)
            (mc-update-by-id "item->list" "lists" list-id
                             {$addToSet {:items (assoc (get-item item-id) :numof 1 :finished nil)}}))))

(defn-spec oneshot->list any?
    [list-id :shop/_id, item :shop/_id]
    ; make sure it's a valid list
    (when-not (list-id-exists? list-id)
        (throw+ (ex-info "unknown list" {:type :db :src "item->list"})))
    ; find the item if it's already in the list
    (if-let [litem (find-list-item-by-name list-id (:entryname item))]
        ; yes it was
        (if (:finished litem)
            (unfinish-list-item list-id (:_id litem))
            (throw+ (ex-info "item already in list" {:type :db :src "item->list"})))
        ; no, we need to add it
        (mc-update-by-id "item->list" "lists" list-id
                         {$addToSet {:items (assoc item :numof 1 :finished nil)}})))

(st/instrument)
