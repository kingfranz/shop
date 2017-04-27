(ns shop2.utils
  	(:require 	(clj-time 	[core     :as t]
            				[local    :as l]
            				[coerce   :as c]
            				[format   :as f]
            				[periodic :as p])
            	(clojure 	[string   :as str]
            				[set      :as set])))

;;-----------------------------------------------------------------------------

(defn now
	[]
	(c/to-date (l/local-now)))

(defn today
	[]
	(let [now (l/local-now)]
		(c/to-date (t/date-time (t/year now) (t/month now) (t/day now)))))

(defn yesterday
	[]
	(let [now (l/local-now)]
		(c/to-date (t/date-time (t/year now) (t/month now) (- (t/day now) 1)))))

(def menu-frmt (f/formatter "EEE MMM dd"))
(def menu-frmt-short (f/formatter "EEE dd"))

(defn menu-date-show
	[menu]
	(f/unparse (f/with-zone menu-frmt (t/default-time-zone)) (c/from-date (:date menu))))

(defn menu-date-short
	[menu]
	(f/unparse (f/with-zone menu-frmt-short (t/default-time-zone)) (c/from-date (:date menu))))

(defn menu-date-key
	[menu]
	(f/unparse (f/with-zone (f/formatter :date) (t/default-time-zone)) (c/from-date (:date menu))))

(defn now-str
	[]
	(f/unparse (f/with-zone (f/formatter :mysql) (t/default-time-zone)) (l/local-now)))

(defn time-range
	"Return a lazy sequence of DateTime's from start to end, incremented
	by 'step' units of time."
	[start end step]
	(let [inf-range (p/periodic-seq start step)
		  below-end? (fn [t] (t/within? (t/interval start end) t))]
		(take-while below-end? inf-range)))

(defn is-today?
	[dt]
	(let [now (l/local-now)]
		(and (= (t/month dt) (t/month now)) (= (t/day dt) (t/day now)))))

