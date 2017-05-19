(ns shop2.views.projects
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
 	          	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
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
            	(ring.util 	[anti-forgery :as ruaf]
            				[response     :as ring])
              	(clojure 	[string           :as str]
            				[set              :as set])))

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

(defn tags-head
	[stags]
	(hf/label {:class "proj-head-val"} :xxx stags))

(defn t->s
	[tags]
	(if-let [s (some->> tags
						(map :entryname)
						sort
						(str/join " "))]
		s
		"Allmänt"))

(defn by-tags
	[projects]
	(let [by-tag (->> projects
		  			  (remove finished?)
		  			  (map #(assoc % :stags (t->s (:tags %))))
		  			  (group-by :stags))]
		[:table
			(for [tag-key (sort (keys by-tag))]
				(list
					[:tr
						[:th.proj-head-th {:colspan 4} (tags-head tag-key)]]
					(map mk-proj-row (->> tag-key
										  (get by-tag)
										  (sort-by proj-comp)))))]))

(defn prio-head
	[pri]
	(hf/label {:class "proj-head-val"} :xxx (str "Prioritet " pri)))

(defn by-prio
	[projects]
	(let [by-pri (group-by :priority (remove finished? projects))]
		[:table
			(for [pri-key (sort (keys by-pri))]
				(list
					[:tr
						[:th.proj-head-th {:colspan 4} (prio-head pri-key)]]
					(map mk-proj-row (->> pri-key
										  (get by-pri)
										  (sort-by proj-comp)))))]))

(defn show-projects-page
    [grouping]
    (let [projects (dbprojects/get-projects)]
    	(layout/common "Projekt" [css-projects]
	        (hf/form-to {:enctype "multipart/form-data"}
	    		[:post "/update-projects"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :proj-keys (->> projects
	        									 (remove finished?)
	        									 (map :_id)
	        									 (str/join "@")))
	        	[:table.proj-tbl
		        	[:tr
		        		[:td
			        		(common/home-button)
			        		(if (= grouping :by-prio)
			        			[:a.link-flex {:href "/projects/by-tag"} "Kat-Sort"]
			        			[:a.link-flex {:href "/projects/by-prio"} "Pri-Sort"])
			        		[:a.link-flex {:href "/clear-projects"} "Rensa"]
			        		(hf/submit-button {:class "button button1"} "Updatera!")]]
			        [:tr
			        	[:td
			        		(if (= grouping :by-prio)
			        			(by-prio projects)
			        			(by-tags projects))]]
					[:tr
						[:td
							[:table
								[:tr
									[:th.proj-head-th {:colspan 4}
										(hf/label
											{:class "proj-head-val"}
											:xxx "Nya projekt ")]]
								(for [x (range num-new-proj)]
									(mk-proj-row x))]]]
					[:tr
						[:td
							[:table
								[:tr
			        				[:th.proj-head-th
			        					(hf/label
			        						{:class "proj-head-val"}
			        						:xxx "Avklarade")]]
			        			(let [projs (sort-by proj-comp (filter finished? projects))]
			        				(map mk-finished-row projs))]]]]
    	))))

;;-----------------------------------------------------------------------------

(defn lspy
	[l]
	(prn "lspy:" (type l))
	(doseq [e l] (prn "lspy:" e))
	l)

(defn mk-proj-tags
	[params pkey]
	(map #(dbtags/add-tag %)
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
		(dbprojects/update-project {:_id        pkey
					   	    :entryname f-name
					   		:tags      f-tags
					   		:priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))}))
	(doseq [pkey (range num-new-proj)
		    :let [f-name (get params (mk-tag txt-name pkey))
		          f-tags (mk-proj-tags params pkey)]
			:when (and (seq f-name) (seq f-tags))]
		(dbprojects/add-project {:entryname f-name
					   	 :tags      f-tags
					   	 :priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))}))
	(ring/redirect "/projects"))

(defn unfinish-project
	[id]
	(dbprojects/unfinish-project id)
	(ring/redirect "/projects"))

(defn finish-project
	[id]
	(dbprojects/finish-project id)
	(ring/redirect "/projects"))

(defn clear-projects
	[]
	(dbprojects/clear-projects)
	(ring/redirect "/projects"))
