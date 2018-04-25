(ns shop2.views.common
    (:require [shop2.extra :refer :all]
              [mongolib.core :as db]
              [shop2.db.user :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.views.css :refer :all]
              [shop2.spec :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [hiccup.element :as he]
              [hiccup.form :as hf]
              [environ.core :refer [env]]
              [clojure.string :as str]
              [utils.core :as utils]
              [clj-time.core :as t]
              [clj-time.format :as f]))

;;-----------------------------------------------------------------------------

(defn-spec get-parents (s/coll-of :shop/_id :kind set?)
    [a-list :shop/list]
    (loop [parent (:parent a-list)
           acc #{(:_id a-list)}]
        (if (nil? parent)
            acc
            (recur (:parent parent) (conj acc (:_id parent))))))

(defn-spec labeled-radio any?
    [label :shop/entryname, group string?, value :shop/_id, checked? boolean?]
    [:label.new-cb-n label (hf/radio-button group checked? value)])

(defn-spec named-div any?
    ([d-name string?, input any?]
     (named-div :break d-name input))
    ([line-type keyword?, d-name string?, input any?]
     [:div.named-div
      (if (= line-type :inline)
          [:label.named-div-l d-name]
          [:p.named-div-p d-name])
      input
      [:div {:style "clear:both;float:none;"}]]))

(defn named-block
    [header block]
    [:p
     [:div.items-block
      [:p.tags-head
       [:label.tags-head header]]
      block]])

;;-----------------------------------------------------------------------------

(def blank-tag {:_id no-id :entryname "NO TAG" :parent nil})
(def blank-proj {:_id no-id :entryname "NO PROJ"})

(defn-spec get-old-tag (s/nilable :shop/_id)
	[params map?]
    (when-not (str/blank? (:tags params))
        (when-not (s/valid? :shop/_id (:tags params))
            (throw+ {:type :input :src "get-old-tag" :cause "unknown tag"}))
        (:tags params)))

(defn-spec get-new-tag any?
	[params map?]
	(when-not (str/blank? (:new-tag params))
        (when-not (s/valid? :tags/entryname (:new-tag params))
            (throw+ {:type :input :src "get-new-tag" :cause "Invalid tag"}))
        (:new-tag params)))

(defn-spec extract-tag (s/nilable :shop/tag)
	[params map?]
    (let [old-tag-id   (get-old-tag params)
          new-tag-name (get-new-tag params)]
		(cond
            ; old has value, new has value
            (and (seq old-tag-id) (not= old-tag-id no-id) (seq new-tag-name))
                (throw+ {:type :input :src "extract-tag" :cause "Can't have both new and old tag"})
            ; old has value, new is blank
            (and (seq old-tag-id) (not= old-tag-id no-id) (nil? new-tag-name))
                (get-tag old-tag-id)
            ; old is no-id, new has value
            (and (= old-tag-id no-id) (seq new-tag-name))
                (add-tag new-tag-name)
            ; old is no-id, new is blank
            (and (= old-tag-id no-id) (nil? new-tag-name))
                nil
            ; old is blank, new has value
            (and (nil? old-tag-id) (seq new-tag-name))
                (add-tag new-tag-name)
            ; old is blank, new is blank
            :else nil
            )))

(defn-spec mk-tag-entry any?
    "create a labeled radio button for a tag"
    [target (s/nilable string?), tag (s/keys :req-un [:shop/_id :shop/entryname])]
    [:div.cb-div
     (labeled-radio (:entryname tag)
                    "tags"
                    (:_id tag)
                    (= target (:entryname tag)))])

(defn-spec filter-tag-parent (s/coll-of :shop/tag)
    [parent :shop/parent, tags (s/coll-of :shop/tag)]
    (if parent
        (let [parents (get-parents (get-list parent))]
            (filter #(or (nil? (:parent %)) (contains? parents (:parent %))) tags))
        tags))

(defn-spec tags-tbl any?
    [info any?, parent :shop/parent, tag :item/tag]
    [:div
     info
     (named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
     (named-div "Existerande kategorier:"
            (->> (get-tags)
                 (filter-tag-parent parent)
                 (sort-by :entrynamelc)
                 (concat [blank-tag])
                 (map #(mk-tag-entry (:entryname tag) %))))])

(defn-spec mk-proj-entry any?
    [target-id :shop/_id, proj :shop/project]
    [:div.cb-div
     (labeled-radio (:entryname proj)
                    "projects"
                    (:_id proj)
                    (= (:_id proj) target-id))])

(defn projs-tbl
    ([] (projs-tbl nil))
    ([proj-id]
     (named-div "Projekt:"
                (->> (get-active-projects)
                     (sort-by :entrynamelc)
                     (concat [blank-proj])
                     (map #(mk-proj-entry proj-id %))))))

;;-----------------------------------------------------------------------------

(defn-spec dt->str string?
    [dt (s/nilable :shop/date)]
    (if dt
       (utils/year-month-day dt)
       ""))

(defn-spec str->num (s/nilable double?)
    [s (s/nilable string?)]
    (try+
        (when s
            (Double/valueOf (str/trim s)))
        (catch Exception _ nil)))

;;-----------------------------------------------------------------------------

(defn home-button
    []
    [:a.link-flex {:href "/user/home"} (he/image "/images/home32w.png")])

(defn admin-home-button
    []
    [:a.link-flex {:href "/admin/"} (he/image "/images/home32w.png")])

(defn back-button
	[target]
	[:a.link-flex {:href target} (he/image "/images/back32w.png")])

(defn homeback-button
    [target]
    (list
        (home-button)
        (back-button target)))

(defn admin-homeback-button
    [target]
    (list
        (admin-home-button)
        (back-button target)))

;;-----------------------------------------------------------------------------

(defn-spec udata :shop/user
	[req map?]
	(if-let [current (get-in req [:session :cemerick.friend/identity :current])]
		(if-let [udata (get-in req [:session :cemerick.friend/identity :authentications current])]
			(if (s/valid? :shop/user udata)
				(get-user (:username udata))
				(throw+ {:type :input :src "udata" :cause (s/explain-str :shop/user udata)}))
			(throw+ {:type :input :src "udata" :cause "invalid session2"}))
		(throw+ {:type :input :src "udata" :cause "invalid request"})))

(defn-spec uid :shop/_id
	[req map?]
	(-> req udata :_id))

;;-----------------------------------------------------------------------------

(st/instrument)
