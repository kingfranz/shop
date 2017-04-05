(ns shop2.views.common
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[garden.core              :as g]
            	[garden.units             :as u]
            	[garden.selectors         :as sel]
            	[garden.stylesheet        :as stylesheet]
            	[garden.color             :as color]
            	[hiccup.core              :as h]
            	[hiccup.def               :as hd]
            	[hiccup.element           :as he]
            	[hiccup.form              :as hf]
            	[hiccup.page              :as hp]
            	[hiccup.util              :as hu]
            	[ring.util.anti-forgery   :as ruaf]
            	[clojure.string           :as str]
            	[clojure.set              :as set]))

;;-----------------------------------------------------------------------------

(defn get-tag
	[p t l]
	(if (seq (get p t))
		(conj l (get p t))
		l))

(defn extract-tags
	[params]
	(->> (db/get-tags)
		 (map (fn [tn] (when (get params (keyword tn)) tn)))
		 (get-tag params :new-tag-0)
	     (get-tag params :new-tag-1)
	     (get-tag params :new-tag-2)
	     (remove nil?)
	     set))

(def menu-frmt (f/formatter "EEE MMM dd"))
(def menu-frmt-short (f/formatter "EEE dd"))

(defn menu-date-show
	[dt]
	(f/unparse (f/with-zone menu-frmt (t/default-time-zone)) dt))

(defn menu-date-short
	[dt]
	(f/unparse (f/with-zone menu-frmt-short (t/default-time-zone)) dt))

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
	(let [now (l/local-now)]
		(and (= (t/month dt) (t/month now)) (= (t/day dt) (t/day now)))))

(def num-tags 4)

(def css-tags-tbl
	(g/css
		[:.cat-choice {
			:font-size (u/px 24)
		}]
		[:.cat-choice-th {
			:text-align :left
			;:font-size (u/px 36)
			:padding [[(u/px 20) (u/px 20) 0 0]]
		}]
		[:.new-cb-td {
			:text-align :right
			:width (u/px 200)
			:border [[(u/px 1) :solid :grey]]
			:padding [[0 (u/px 10) 0 0]]
		}]
		[:.new-cb-n {
			:text-align :right
			:margin [[(u/px 5) (u/px 10) (u/px 5) 0]]
		}]
		[:.new-cb {
			:margin [[0 (u/px 10) 0 0]]
			:transform "scale(2)"
		}]
		[:.new-tag-txt {
			:color :white
			:background-color layout/transparent
			:font-size (u/px 24)
			:width (u/px 182)
			:border [[(u/px 1) :solid :grey]]
			:margin [[0 (u/px 10) 0 0]]
		}]
		[:.new-name-td {
			:background-color layout/transparent
			:border 0
			;:padding [[(u/px 20) (u/px 20) 0 0]]
		}]
		))

(defn frmt-tags
	[tags]
	(->> tags
		 seq
		 sort
		 (str/join " ")))

(defn filter-tags
	[list-tags item-tags]
	(let [tags-left (set/difference item-tags list-tags)]
		(if (seq tags-left)
			tags-left
			#{"Allmänt"})))

(defn mk-tag-entry
	[tname]
	[:td.new-cb-td
		(hf/label {:class "new-cb-n"} :xxx tname)
		(hf/check-box {:id tname :class "new-cb"} tname)])

(defn old-tags-tbl
	[]
    [:table.master-table
    	[:tr
    		[:th.cat-choice-th {:colspan 4}
    			(hf/label {:class "cat-choice"} :xxx "Välj kategorier")]]
	    (map (fn [r] [:tr r])
        	(partition-all num-tags (map mk-tag-entry (db/get-tags))))])

(defn new-tags-tbl
	[]
	[:table.master-table
    	[:tr
    		[:th.cat-choice-th {:colspan 3}
    			(hf/label {:class "cat-choice"} :xxx "Gör nya kategorier")]]
	    [:tr
	    	(for [i (range num-tags)]
	    		[:td.new-tag-txt-td
	    			(hf/text-field {:class "new-tag-txt"} (str "new-tag-" i))])]])
