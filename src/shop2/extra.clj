(ns shop2.extra
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [slingshot.slingshot :refer [throw+ try+]]
              ;[clojure.spec.alpha :as s]
              ;[orchestra.core :refer [defn-spec]]
              ))

;;-----------------------------------------------------------------------------

(defn today
	[]
	(let [now (l/local-now)]
		(t/date-time (t/year now) (t/month now) (t/day now))))

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
