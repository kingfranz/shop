(ns shop2.views.projects
  	(:require 	[shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.views.layout :refer :all]
                 [shop2.views.common       	:refer :all]
                 [shop2.views.css          	:refer :all]
                 [shop2.db.tags :refer :all]
                 [shop2.db.items			:refer :all]
                 [shop2.db.lists 			:refer :all]
                 [shop2.db.menus 			:refer :all]
                 [shop2.db.projects 		:refer :all]
                 [shop2.db.recipes 		:refer :all]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 [garden.core :as g]
                 [garden.units        	:as u]
                 [garden.selectors    	:as sel]
                 [garden.stylesheet   	:as ss]
                 [garden.color        	:as color]
                 [garden.arithmetic   	:as ga]
                 [hiccup.core :as h]
                 [hiccup.def          	:as hd]
                 [hiccup.element      	:as he]
                 [hiccup.form         	:as hf]
                 [hiccup.page         	:as hp]
                 [hiccup.util         	:as hu]
                 [ring.util.anti-forgery :as ruaf]
                 [ring.util.response     	:as ring]))

;;-----------------------------------------------------------------------------

(def num-new-proj 5)
(def pri-name "proj-pri-")
(def txt-name "proj-txt-")
(def tags-name "proj-tags-")

(defn- mk-tag
	[s  i]
	(keyword (str s i)))

(defn- mk-proj-row
	[proj]
	(let [id (if (map? proj) (:_id proj) (str proj))]
		[:tr
			[:td.proj-pri-td (hf/drop-down {:class "proj-pri-val"}
				(mk-tag pri-name id)
				(range 1 6) (:priority proj))]
			[:td.proj-check-td
				(when (map? proj)
					[:a {:class "proj-check-val"
						 :href (str "/user/finish-project/" (:_id proj))} "&#10004"])]
			[:td.proj-txt-td (hf/text-field {:class "proj-txt-val"}
				(mk-tag txt-name id)
				(:entryname proj))]
			[:td.proj-tags-td (hf/text-field {:class "proj-tags-val"}
				(mk-tag tags-name id)
				(str/join ", " (map :entryname (:tags proj))))]]))

(defn- mk-finished-row
	[proj]
	[:tr
		[:td
			[:a.finished-proj
				{:href (str "/user/unfinish-project/" (:_id proj))}
				(str (:priority proj) " " 
					 (:entryname proj) " " 
					 "[" (str/join ", " (map :entryname (:tags proj))) "]" )]]])

(defn- finished?
	[p]
	(some? (:finished p)))

(defn- proj-comp
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

(defn- tags-head
	[stags]
	(hf/label {:class "proj-head-val"} :xxx stags))

(defn- t->s
	[tags]
	(if-let [s (some->> tags
						(map :entryname)
						sort
						(str/join " "))]
		s
		"Allmänt"))

(defn- by-tags
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

(defn- prio-head
	[pri]
	(hf/label {:class "proj-head-val"} :xxx (str "Prioritet " pri)))

(defn- by-prio
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

(defn edit-projects
    [request grouping]
    (let [projects (get-projects)]
    	(common request "Projekt" [css-projects]
	        (hf/form-to
	    		[:post "/user/update-projects"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :proj-keys (->> projects
	        									 (remove finished?)
	        									 (map :_id)
	        									 (str/join "@")))
	        	[:table.proj-tbl
		        	[:tr
		        		[:td
			        		(home-button)
			        		(if (= grouping :by-prio)
			        			[:a.link-flex {:href "/user/projects/by-tag"} "Kat-Sort"]
			        			[:a.link-flex {:href "/user/projects/by-prio"} "Pri-Sort"])
			        		[:a.link-flex {:href "/user/clear-projects"} "Rensa"]
			        		(hf/submit-button {:class "button"} "Updatera!")]]
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

(defn- mk-proj-tags
	[params pkey]
	(map #(add-tag %)
		(some-> params
		    (get (mk-tag tags-name pkey))
		    (str/replace "\"" "")
		    (str/split #"(,| )+")
			set)))

(defn edit-projects!
	[{params :params}]
	(doseq [pkey (str/split (:proj-keys params) #"@")
		    :let [f-name (get params (mk-tag txt-name pkey))
		          f-tags (mk-proj-tags params pkey)]
			:when (and (seq f-name) (seq f-tags))]
		(update-project {:_id        pkey
					   	    :entryname f-name
					   		:tags      f-tags
					   		:priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))}))
	(doseq [pkey (range num-new-proj)
		    :let [f-name (get params (mk-tag txt-name pkey))
		          f-tags (mk-proj-tags params pkey)]
			:when (and (seq f-name) (seq f-tags))]
		(add-project {:entryname f-name
					   	 :tags      f-tags
					   	 :priority  (Integer/valueOf (get params (mk-tag pri-name pkey)))}))
	(ring/redirect "/user/projects"))

(defn unfinish-proj
	[request id]
	(unfinish-project id)
	(ring/redirect "/user/projects"))

(defn finish-proj
	[request id]
	(finish-project id)
	(ring/redirect "/user/projects"))

(defn clear-projs
	[request]
	(clear-projects)
	(ring/redirect "/user/projects"))
