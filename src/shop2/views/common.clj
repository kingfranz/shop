(ns shop2.views.common
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	(clj-time 	[core         :as t]
            				[local        :as l]
            				[coerce       :as c]
            				[format       :as f]
            				[periodic     :as p])
            	(taoensso 	[timbre       :as log])
				(garden 	[core         :as g]
            				[units        :as u]
            				[selectors    :as sel]
            				[stylesheet   :as ss]
            				[color        :as color])
            	(hiccup 	[core         :as h]
            				[def          :as hd]
            				[element      :as he]
            				[form         :as hf]
            				[page         :as hp]
            				[util         :as hu])
            	(clojure 	[string       :as str]
            				[set          :as set])))

;;-----------------------------------------------------------------------------

(def top-lvl-name "Ingen")

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
			(map #(db/add-tag %)
				(some-> params
				    (:new-tags params)
				    (str/replace "\"" "")
				    (str/split #"(,| )+")
					set))))

(def css-tags-tbl
	(g/css
		[:.cat-choice {
			:font-size (u/px 24)
			:margin    [[0 (u/px 10) 0 0]]
		}]
		[:.cat-choice-th {
			:text-align :left
			;:font-size (u/px 36)
			:padding [[(u/px 20) (u/px 20) 0 0]]
		}]
		[:.cb-div {
			:float :left
			:text-align :right
			:width (u/px 200)
			;:border [[(u/px 1) :solid :grey]]
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
		[:.new-tags {
			:width (u/px 600)
		}]
		[:.named-div {
			:margin [[(u/px 20) (u/px 20) (u/px 20) (u/px 20)]]
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.named-div-p {
			:margin [[0 0 (u/px 10) 0]]
		}]
		[:.named-div-l {
			:margin [[0 (u/px 10) 0 0]]
		}]
		))

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
	[:div.cb-div
		(hf/label {:class "new-cb-n"} :xxx tname)
		(hf/check-box {:id tname :class "new-cb"} (keyword tname))])

(defn old-tags-tbl
	[]
    (named-div "Existerande kategorier:"
	    (map mk-tag-entry (sort (map :entryname (db/get-tag-names))))))

(defn new-tags-tbl
	[]
	(named-div "Nya kategorier:"
	    (hf/text-field {:class "new-tags"} :new-tags)))

(defn home-button
	[]
	[:a.link-icon {:href "/"} (he/image "/images/home32w.png")])

(defn back-button
	[target]
	[:a.link-icon {:href target} (he/image "/images/back32w.png")])

(defn homeback-button
	[target]
	(list
		[:a.link-icon {:href "/"} (he/image "/images/home32w.png")]
		[:a.link-icon {:href target} (he/image "/images/back32w.png")]))


