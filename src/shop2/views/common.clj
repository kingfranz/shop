(ns shop2.views.common
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
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

(defonce blank-tag {:_id no-id :entryname "NO TAG" :parent nil})
(defonce blank-proj {:_id no-id :entryname "NO PROJ"})

(defn- get-old-tag
	[params]
    (let [s (some-> params :tags str/trim)]
        (if (str/blank? s)
            nil
            (if (= s no-id)
                :blank
                (if (s/valid? :shop/_id s)
                    s
                    (throw+ (ex-info "Unknown tag" {:type :input :cause :unknown-tag})))))))

(defn- get-new-tag
	[params]
	(let [s (some-> params :new-tags str/trim)]
		(if (str/blank? s)
            nil
            (if (s/valid? :tags/entryname s)
                s
                (throw+ (ex-info "Invalid tag" {:type :input :cause :invalid-tag}))))))

(defn extract-tag
	[params]
    {:post [(utils/valid? (s/nilable :shop/tag) %)]}
	(let [old-tag-id   (get-old-tag params)
          new-tag-name (get-new-tag params)]
		(cond
            ; old has value, new has value
            (and (seq old-tag-id) (not= old-tag-id :blank) (seq new-tag-name))
                (throw+ (ex-info "Can't have both new and old tag" {:type :tags}))
            ; old has value, new is blank
            (and (seq old-tag-id) (not= old-tag-id :blank) (nil? new-tag-name))
                (get-tag old-tag-id)
            ; old is no-id, new has value
            (and (= old-tag-id :blank) (seq new-tag-name))
                (add-tag new-tag-name)
            ; old is no-id, new is blank
            (and (= old-tag-id :blank) (nil? new-tag-name))
                nil
            ; old is blank, new has value
            (and (nil? old-tag-id) (seq new-tag-name))
                (add-tag new-tag-name)
            ; old is blank, new is blank
            :else nil
            )))

(defn- mk-tag-entry
    [target tag]
    [:div.cb-div
     (labeled-radio (:entryname tag)
                    "tags"
                    (:_id tag)
                    (= target (:entryname tag)))])

(defn tags-tbl
    ([] (tags-tbl nil))
    ([tag]
     (named-div "Existerande kategorier:"
                    (->> (get-tag-names)
                         (sort-by :entrynamelc)
                         (concat [blank-tag])
                         (map #(mk-tag-entry (:entryname tag) %))))))

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
                     (sort-by :entrynamelc)
                     (concat [blank-proj])
                     (map #(mk-proj-entry proj-id %))))))

;;-----------------------------------------------------------------------------

(defn str->num
    [s]
    (try+
        (Double/valueOf (str/trim s))
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
