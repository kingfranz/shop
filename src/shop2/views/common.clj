(ns shop2.views.common
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	(clj-time 	[core         :as t]
            				[local        :as l]
            				[coerce       :as c]
            				[format       :as f]
            				[periodic     :as p])
            	(taoensso 	[timbre       :as log])
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

(def num-tags 4)

(defn mk-new-tk
	[idx]
	(keyword (str "new-tag-" idx)))

(defn extract-tags
	[params]
	(log/debug params)
	(concat (for [db-tag (db/get-tags)
		  		  :when (or (get params (keyword (:entryname db-tag)))
		  		 	        (get params (:entryname db-tag)))]
		  		db-tag)
		    (for [idx (range num-tags)
		  		  :let [tag-name (get params (mk-new-tk idx))]
		  		  :when (not (str/blank? tag-name))]
		  		(db/add-tag tag-name))))

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
		[:input.new-cb {
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
	(let [tags-left (set/difference (set (map :entryname item-tags))
									(set (map :entryname list-tags)))]
		(if (seq tags-left)
			tags-left
			#{"Allmänt"})))

(defn mk-tag-entry
	[tname]
	[:td.new-cb-td
		(hf/label {:class "new-cb-n"} :xxx tname)
		(hf/check-box {:id tname :class "new-cb"} (keyword tname))])

(defn old-tags-tbl
	[]
    [:table.master-table
    	[:tr
    		[:th.cat-choice-th {:colspan 4}
    			(hf/label {:class "cat-choice"} :xxx "Välj kategorier")]]
	    (map (fn [r] [:tr r])
        	(partition-all num-tags (map mk-tag-entry (sort (db/get-tag-names)))))])

(defn new-tags-tbl
	[]
	[:table.master-table
    	[:tr
    		[:th.cat-choice-th {:colspan 3}
    			(hf/label {:class "cat-choice"} :xxx "Gör nya kategorier")]]
	    [:tr
	    	(for [i (range num-tags)]
	    		[:td.new-tag-txt-td
	    			(hf/text-field {:class "new-name"} (mk-new-tk i))])]])
