(ns shop2.views.common
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.views.css :refer :all]
              [shop2.spec :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [garden.core :as g]
              [garden.units :as u]
              [garden.selectors :as sel]
              [garden.stylesheet :as ss]
              [garden.color :as color]
              [garden.arithmetic :as ga]
              [hiccup.core :as h]
              [hiccup.def :as hd]
              [hiccup.element :as he]
              [hiccup.form :as hf]
              [hiccup.page :as hp]
              [hiccup.util :as hu]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [environ.core :refer [env]]
              [clojure.spec.alpha :as s]
              [clojure.string :as str]
              [clojure.set :as set]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn- labeled-radio
    [label group value checked?]
    [:label.new-cb-n label (hf/radio-button group checked? value)])

(defn named-div
    ([d-name input]
     (named-div :break d-name input))
    ([line-type d-name input]
     [:div.named-div
      (if (= line-type :inline)
          (hf/label {:class "named-div-l"} :x d-name)
          [:p.named-div-p d-name])
      input
      [:div {:style "clear:both;float:none;"}]]))

(defn named-block
    [header block]
    [:p
     [:div.items-block
      [:p.tags-head
       (hf/label {:class "tags-head"} :x header)]
      block]])

;;-----------------------------------------------------------------------------

(defonce top-lvl-name "Ingen")
(defonce tag-name-regex #"[a-zA-ZåäöÅÄÖ0-9_-]+")
(defonce blank-tag-id "@@--NO-TAG--@@")
(defonce blank-tag {:_id blank-tag-id :entryname "NO TAG" :parent nil})
(defonce blank-proj {:_id blank-tag-id :entryname "NO PROJ"})

(defn- get-old-tag
	[params]
    (let [s (some-> params :tags str/trim)]
        (if (str/blank? s)
            nil
            (if (= s blank-tag-id)
                :blank
                (if (re-matches uuid-regex s)
                    s
                    (throw+ (ex-info "Unknown tag" {:cause :unknown-tag})))))))

(defn- get-new-tag
	[params]
	(let [s (some-> params :new-tags str/trim)]
		(if (str/blank? s)
            nil
            (if (re-matches tag-name-regex s)
                s
                (throw+ (ex-info "Invalid tag" {:cause :invalid-tag}))))))

(defn extract-tags
	[params]
    {:post [(utils/valid? :shop/tags %)]}
	(let [old-tag-id (get-old-tag params)
          new-tag-name (get-new-tag params)]
		(cond
            (and (some? old-tag-id)
                 (some? new-tag-name)) (throw+ (ex-info "Can't have both new and old tag" {:type :tags}))
            (= old-tag-id :blank) []
            (some? old-tag-id) [(get-tag old-tag-id)]
            (some? new-tag-name) [(add-tag new-tag-name)]
            :else []
            )))

(defn frmt-tags
	[tags]
    {:pre [(utils/valid? :shop/tags tags)]
     :post [(utils/valid? string? %)]}
	(->> tags
		 (map :entryname)
		 sort
		 (str/join " ")))

(defn- mk-tag-entry
    [tag-strings tag]
    [:div.cb-div
     (labeled-radio (:entryname tag)
                    "tags"
                    (:_id tag)
                    (contains? tag-strings (:entryname tag)))])

(defn tags-tbl
    ([] (tags-tbl []))
    ([tags]
     (let [tag-strings (set (map :entryname tags))]
         (named-div "Existerande kategorier:"
                    (->> (get-tag-names)
                         (sort-by :entryname)
                         (concat [blank-tag])
                         (map #(mk-tag-entry tag-strings %)))))))

(defn- mk-proj-entry
    [target-id proj]
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
                     (sort-by :entryname)
                     (concat [blank-proj])
                     (map #(mk-proj-entry proj-id %))))))

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

(defn udata
	[req]
	(if-let [current (get-in req [:session :cemerick.friend/identity :current])]
		(if-let [udata (get-in req [:session :cemerick.friend/identity :authentications current])]
			(if (s/valid? :shop/user udata)
				(get-user (:username udata))
				(throw+ (ex-info (s/explain-str :shop/user udata) {:cause (str udata)})))
			(throw+ (ex-info "invalid session2" {:cause (str udata)})))
		(throw+ (ex-info "invalid request" {:cause (str req)}))))

(defn uid
	[req]
	(-> req udata :_id))

;;-----------------------------------------------------------------------------
