(ns shop2.extra
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [utils.core :as utils]
              [hiccup.form :as hf]
              [shop2.spec :refer :all]
              [shop2.db.misc :as db-m]
              [shop2.db.tags :as dbt]
              [shop2.db.lists :as dbl]
              [shop2.db.projects :as dbp]
              [mongolib.core :as db]
              [clojure.string :as str]))

;;-----------------------------------------------------------------------------

(defn-spec get-tags-dd (s/coll-of (s/cat :str string? :id :shop/_id))
           []
           (->> (dbt/get-tag-names)
                (sort-by :entryname)
                (map (fn [l] [(:entryname l) (:_id l)]))
                (concat [["" db-m/no-id]])))

(defn-spec get-lists-dd (s/coll-of (s/cat :str string? :id :shop/_id))
           []
           (->> (dbl/get-list-names)
                (sort-by :entryname)
                (map (fn [l] [(:entryname l) (:_id l)]))
                (concat [["" db-m/no-id]])))

(defn-spec get-projects-dd :shop/dd
           []
           (->> (dbp/get-project-names)
                (map (fn [l] [(:entryname l) (:_id l)]))
                (concat [["" db-m/no-id]])))

(defn-spec mk-project-dd any?
           [current-id (s/nilable :shop/_id), dd-name keyword?, dd-class string?]
           (hf/drop-down {:class dd-class} dd-name (get-projects-dd) current-id))

(defn-spec mk-list-dd any?
           [current-id (s/nilable :shop/_id), dd-name keyword?, dd-class string?]
           (hf/drop-down {:class dd-class} dd-name (get-lists-dd) current-id))

;;-----------------------------------------------------------------------------

(defn today
    []
    (let [now (l/local-now)]
        (t/date-time (t/year now) (t/month now) (t/day now))))

(defn end-of-today
    []
    (let [now (l/local-now)]
        (t/date-time (t/year now) (t/month now) (t/day now) 23 59 59)))

(def menu-frmt-short (f/formatter "EEE dd"))

(defn menu-date-short
	[menu]
	(f/unparse (f/with-zone menu-frmt-short (t/default-time-zone)) (:date menu)))

(defn menu-date-key
	[dt]
	(f/unparse (f/with-zone (f/formatter :date) (t/default-time-zone)) dt))

(defn is-today?
	[dt]
	(= dt (today)))

(def delta-days 10)

(defn old-menu-start
	[]
	(t/minus (today) (t/days delta-days)))

(defn new-menu-end
	[]
	(t/plus (today) (t/days delta-days)))

;;-----------------------------------------------------------------------------

(defn assoc-str-if
	[m k txt]
	(if (seq txt)
		(assoc m k txt)
		m))

;;-----------------------------------------------------------------------------

(defn comp-nil
    [v1 v2]
    (let [result (compare v1 v2)]
        (when-not (zero? result)
            result)))

(defn- xxx
    [rules x1 x2]
    (if (empty? rules)
        nil
        (if-let [res (comp-nil ((first rules) x1) ((first rules) x2))]
            res
            (xxx (rest rules) x1 x2))))

(defn maplist-sort
    [rules coll]
    (sort-by identity (fn [v1 v2] (xxx rules v1 v2)) coll))

;;-----------------------------------------------------------------------------

(defn get-param
    ([params par-name]
    (when-let [val (get params par-name)]
        (cond
            (= val "true") true
            (= val "false") false
            (re-matches #"\d+\.\d+" val) (Double/valueOf val)
            (re-matches #"\d+" val) (Integer/valueOf val)
            (f/parse val) (f/parse val)
            :else val)))
    ([params par-name idx]
    (get-param params (utils/mk-tag par-name idx))))

;;-----------------------------------------------------------------------------

