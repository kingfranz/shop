(ns shop2.db.tags
    (:require [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.string :as str]
              [cheshire.core :refer :all]
              [taoensso.timbre :as log]
              [monger.operators :refer :all]
              [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn-spec get-tags :shop/tags
	[]
	(mc-find-maps "get-tags" "tags"))

(defn-spec get-tag :shop/tag
	[id :shop/_id]
	(mc-find-map-by-id "get-tag" "tags" id))

(defn-spec get-tag-names (s/* (s/keys :req-un [:shop/_id :shop/entryname]))
           []
           (mc-find-maps "get-tag-names" "tags" {} {:_id true :entryname true}))

(defn-spec get-tags-dd (s/coll-of (s/cat :str string? :id :shop/_id))
    []
    (->> (get-tag-names)
         (sort-by :entryname)
         (map (fn [l] [(:entryname l) (:_id l)]))
         (concat [["" no-id]])))

(defn-spec get-listid-by-name (s/nilable :shop/_id)
    [list-name :shop/string]
    (mc-find-one-as-map "get-list" "lists" {:entryname list-name} {:_id true}))

(defn-spec fix-list-ref (s/nilable :shop/_id)
    [lst (s/nilable string?)]
    (cond
        (str/blank? lst)         nil
        (s/valid? :shop/_id lst) lst
        :else                    (get-listid-by-name lst)))

(defn-spec update-tag any?
    ([tag :shop/tag]
     (mc-replace-by-id "update-tag" "tags" tag))
    ([tag-id :shop/_id, tag-name* :tags/entryname, parent* :shop/parent]
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc "tags" tag-namelc)
          parent     (fix-list-ref parent*)]
        (when (or (str/blank? tag-name) (str/includes? tag-name " "))
            (throw+ (ex-info "update-tag: invalid name" {:type :db})))
		(when (and (some? db-tag) (not= (:_id db-tag) tag-id))
			(throw+ (ex-info "duplicate name" {:type :db})))
        (mc-update-by-id "update-tag" "tags" tag-id
			{$set {:entryname tag-name :entrynamelc tag-namelc :parent parent}}))))

(defn-spec add-tag :shop/tag
    ([tag-name :shop/entryname]
        (add-tag tag-name nil))
    ([tag-name :shop/entryname, parent (s/nilable string?)]
    (let [new-tag    (assoc (create-entity (str/capitalize tag-name))
                            :parent (fix-list-ref parent))
          db-tag     (get-by-enlc "tags" (:entrynamelc new-tag))]
		(if (some? db-tag)
			db-tag
			(do
				(mc-insert "add-tag" "tags" new-tag)
				new-tag)))))

(defn-spec delete-tag any?
	[id :shop/_id]
	(mc-remove-by-id "delete-tag" "tags" id))

(defn-spec delete-tag-all any?
	[id :shop/_id]
	(delete-tag id)
	(mc-update "delete-tag-all" "lists"    {} {$pull {:tag {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" "recipes"  {} {$pull {:tag {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" "menus"    {} {$pull {:tag {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" "projects" {} {$pull {:tag {:_id id}}} {:multi true})
	(mc-update "delete-tag-all" "items"    {} {$pull {:tag {:_id id}}} {:multi true})
	)

(st/instrument)
