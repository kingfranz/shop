(ns shop2.views.projects
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
 	          	(garden 	[core              :as g]
            				[units             :as u]
            				[selectors         :as sel]
            				[stylesheet        :as ss]
            				[color             :as color])
             	(clj-time 	[core            :as t]
            				[local           :as l]
            				[format          :as f]
            				[periodic        :as p])
            	(hiccup 	[core              :as h]
            				[def               :as hd]
            				[element           :as he]
            				[form              :as hf]
            				[page              :as hp]
            				[util              :as hu])
            	[ring.util.anti-forgery   :as ruaf]
            	[clojure.string           :as str]
            	[clojure.set              :as set]))

;;-----------------------------------------------------------------------------

(def css-projects
	(g/css
		[:.proj-tbl {
			:width layout/full
		}]
		[:.proj-head-td {
			:width layout/half
			:text-align :center
		}]
		[:.proj-head-th {
			:height (u/px 70)
			:width layout/full
			:border [[(u/px 1) :white :solid]]
		    :border-radius (u/px 8)
		}]
		[:.proj-head-val {
			:font-size (u/px 36)
		}]
		[:.proj-pri-td {
			:width (u/px 40)
		}]
		[:.proj-check-td {
			:width (u/px 20)
		}]
		[:.proj-txt-td {
			:width (u/px 500)
		}]
		[:.proj-tags-td {
			:width (u/px 200)
		}]
		[:.proj-pri-val {
			:width layout/full
			:font-size (u/px 18)
			:text-align :center
			:color :black
		}]
		[:.proj-check-val {
			:width layout/full
			:border :none
		    :color :white
		    :padding 0
		    :text-align :center
		    :text-decoration :none
		    :display :inline-block
		    :font-size (u/px 18)
		    :margin 0
		    :cursor :pointer
		}]
		[:.proj-txt-val {
			:width layout/full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color layout/transparent
		}]
		[:.proj-tags-val {
			:width layout/full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color layout/transparent
		}]
		[:.finished-proj {
			:text-decoration :line-through
			:width layout/full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color layout/transparent
		}]))

(def num-new-proj 5)
(def pri-name "proj-pri-")
(def txt-name "proj-txt-")
(def tags-name "proj-tags-")

(defn mk-tag
	[s i]
	(keyword (str s i)))

(defn mk-proj-row
	[proj]
	(let [id (if (map? proj) (:_id proj) (str proj))]
		[:tr
			[:td.proj-pri-td (hf/drop-down {:class "proj-pri-val"}
				(mk-tag pri-name id)
				(range 1 6) (:priority proj))]
			[:td.proj-check-td
				(when (map? proj)
					[:a {:class "proj-check-val"
						 :href (str "/finish-project/" (:_id proj))} "&#10004"])]
			[:td.proj-txt-td (hf/text-field {:class "proj-txt-val"}
				(mk-tag txt-name id)
				(:entryname proj))]
			[:td.proj-tags-td (hf/text-field {:class "proj-tags-val"}
				(mk-tag tags-name id)
				(str/join ", " (map :entryname (:tags proj))))]]))

(defn mk-finished-row
	[proj]
	[:tr
		[:td
			[:a.finished-proj
				{:href (str "/unfinish-project/" (:_id proj))}
				(str (:priority proj) " " 
					 (:entryname proj) " " 
					 "[" (str/join ", " (map :entryname (:tags proj))) "]" )]]])

(defn finished?
	[p]
	(some? (:finished p)))

(defn proj-comp
	([p] 0)
	([p1 p2]
	(if (and (finished? p1) (not (finished? p2)))
		1
		(if (and (not (finished? p1)) (finished? p2))
			-1
			(if (< (:priority p1) (:priority p2))
				-1
				(if (> (:priority p1) (:priority p2))
					1
					(compare (:entryname p1) (:entryname p2))))))))

(defn show-projects-page
    []
    (let [projects (db/get-projects)]
    	(layout/common "Projekt" [css-projects]
        (hf/form-to {:enctype "multipart/form-data"}
    		[:post "/update-projects"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :proj-keys (->> projects
        									 (remove finished?)
        									 (map :_id)
        									 (str/join "@")))
        	[:div.proj-div
	        	[:table.proj-tbl
	        		[:tr
	        			[:td.proj-head-td [:a.link-head {:href "/"} "Home"]]
	        			[:td.proj-head-td (hf/submit-button {:class "button button1"} "Updatera!")]]]
		        [:table.proj-tbl
		        	(list
		        		(let [by-pri (group-by :priority (remove finished? projects))]
		        			(for [pri-key (sort (keys by-pri))]
		        				(list [:tr [:th.proj-head-th {:colspan 4}
		        						(hf/label {:class "proj-head-val"} :xxx (str "Prioritet " pri-key))]]
		        					(map mk-proj-row (sort-by proj-comp (get by-pri pri-key))))))
		        		[:tr [:th.proj-head-th {:colspan 4} (hf/label {:class "proj-head-val"} :xxx "Nya projekt ")]]
		        		(for [x (range num-new-proj)]
		        			(mk-proj-row x)))]
		        [:table.proj-tbl
		        	[:tr [:th.proj-head-th (hf/label {:class "proj-head-val"} :xxx "Avklarade")]]
		        	(let [projs (sort-by proj-comp (filter finished? projects))]
		        		(map mk-finished-row projs))]]
    	))))

;;-----------------------------------------------------------------------------

(defn lspy
	[l]
	(prn "lspy:" (type l))
	(doseq [e l] (prn "lspy:" e))
	l)

(defn mk-proj-tags
	[params pkey]
	(map #(db/add-tag %)
		(some-> params
		    (get (mk-tag tags-name pkey))
		    (str/replace "\"" "")
		    (str/split #"(,| )+")
			set)))

(defn update-projects!
	[{params :params}]
	(doseq [pkey (str/split (:proj-keys params) #"@")
		    :let [f-name (get params (mk-tag txt-name pkey))
		          f-tags (mk-proj-tags params pkey)]
			:when (and (seq f-name) (seq f-tags))]
		(db/update-project {:_id        pkey
					   	    :entryname f-name
					   		:tags      f-tags
					   		:priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))}))
	(doseq [pkey (range num-new-proj)
		    :let [f-name (get params (mk-tag txt-name pkey))
		          f-tags (mk-proj-tags params pkey)]
			:when (and (seq f-name) (seq f-tags))]
		(db/add-project {:entryname f-name
					   	 :tags      f-tags
					   	 :priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))})))

(defn unfinish-project
	[id]
	(db/unfinish-project id))

(defn finish-project
	[id]
	(db/finish-project id))
