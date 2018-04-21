(ns shop2.extra
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [utils.core :as utils]))

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

(defn time-range
	"Return a lazy sequence of DateTime's from start to end, incremented
	by 'step' units of time."
	[start end step]
	(let [inf-range (p/periodic-seq start step)
		  below-end? (fn [t] (t/within? (t/interval start end) t))]
		(take-while below-end? inf-range)))

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

(defn menu-new-range
	[]
	(time-range (today) (new-menu-end) (t/days 1)))

(defn assoc-str-if
	[m k txt]
	(if (seq txt)
		(assoc m k txt)
		m))

(defn comp-nil
    [v1 v2]
    (let [result (compare v1 v2)]
        (when-not (zero? result)
            result)))

(defn comp-nil*
    [v1 v2]
    (let [result (compare v1 v2)]
        (printf "%30s %30s %30s %30s %d\n" (str v2) (str (type v2)) (str v1) (str (type v1)) result)
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
    ;(sort-by identity (fn [v1 v2] (utils/find-first some? (map (fn [r] (comp-nil* (r v1) (r v2))) rules))) coll))

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

