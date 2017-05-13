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
	(l/local-now))

(defn today
	[]
	;(->> (t/today) c/to-date c/from-date))
	(let [now (l/local-now)]
		(t/date-time (t/year now) (t/month now) (t/day now))))

(defn yesterday
	[]
	(let [now (today)]
		(t/date-time (t/year now) (t/month now) (- (t/day now) 1))))

(def menu-frmt (f/formatter "EEE MMM dd"))
(def menu-frmt-short (f/formatter "EEE dd"))

(defn menu-date-show
	[menu]
	(f/unparse (f/with-zone menu-frmt (t/default-time-zone)) (:date menu)))

(defn menu-date-short
	[menu]
	(f/unparse (f/with-zone menu-frmt-short (t/default-time-zone)) (:date menu)))

(defn menu-date-key
	[dt]
	(f/unparse (f/with-zone (f/formatter :date) (t/default-time-zone)) dt))

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
	;(println dt (today))
	(= dt (today)))

(def delta-days 10)

(defn old-menu-start
	[]
	(t/minus (today) (t/days delta-days)))

(defn new-menu-end
	[]
	(t/plus (today) (t/days delta-days)))

(defn menu-old-range
	[]
	(time-range (old-menu-start) (yesterday) (t/days 1)))

(defn menu-new-range
	[]
	(time-range (today) (new-menu-end) (t/days 1)))

(defn find-first
	[pred coll]
	(some (fn [x] (when (pred x) x)) coll))
