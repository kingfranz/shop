(ns shop2.views.home
  	(:require 	(shop2 		 		[db          	:as db]
  							 		[utils       	:as utils])
            	(shop2.views 		[layout      	:as layout]
            				 		[common      	:as common]
            				 		[css 			:refer :all])
            	(shop2.db 			[tags 		 	:as dbtags]
  									[items		 	:as dbitems]
  									[lists 		 	:as dblists]
  									[menus 		 	:as dbmenus]
  									[projects 	 	:as dbprojects]
  									[recipes 	 	:as dbrecipes])
				(cemerick 			[friend      	:as friend])
            	(cemerick.friend 	[workflows 	 	:as workflows]
                             		[credentials 	:as creds])
            	(clj-time 	 		[core        	:as t]
            				 		[local       	:as l]
            				 		[coerce      	:as c]
            				 		[format      	:as f]
            				 		[periodic    	:as p])
            	(garden 	 		[core        	:as g]
            				 		[units       	:as u]
            				 		[selectors   	:as sel]
            				 		[stylesheet  	:as ss]
            				 		[color      	:as color]
            				 		[arithmetic     :as ga])
            	(hiccup 	 		[core           :as h]
            				 		[def            :as hd]
            				 		[element        :as he]
            				 		[form           :as hf]
            				 		[page           :as hp]
            				 		[util           :as hu])
            	(ring.util 			[anti-forgery 	:as ruaf]
            						[response     	:as ring])
            	(clojure 	 		[string         :as str]
            				 		[set            :as set])))

;;-----------------------------------------------------------------------------

(defn want-tree?
	[req]
	(= (->> req common/udata :properties :home :list-type) "tree"))

(defn mk-list-name
	[slist]
	(str (:entryname slist) " - " (:count slist)))

(defn sub-tree
	[lists slist]
	(let [sub-lists (->> lists
                         (filter #(some->> % :parent :_id (= (:_id slist))))
                         (sort-by :entryname))]
		[:li
   			[:a.link-thick {:href (str "/user/list/" (:_id slist))} (mk-list-name slist)]
			(when (seq sub-lists)
				[:ul
     				(map #(sub-tree lists %) sub-lists)])]))

(defn list-cmp
  	[x1 x2]
    (if (> (:count x1) (:count x2))
        -1
        (if (< (:count x1) (:count x2))
        	1
        	(compare (:entryname x1) (:entryname x2)))))

(defn list-tbl
	[lists]
	[:table
		(for [a-list (->> lists
						  (filter #(nil? (:finished %)))
						  (sort-by identity list-cmp))]
			[:tr [:td
				[:a.link-thick {:href (str "/user/list/" (:_id a-list))}
					(str (:entryname a-list) " " (:count a-list))]]])])

(defn list-tree
	[request]
	(let [lists (dblists/get-lists-with-count)]
		(if (want-tree? request)
			[:ul.tree
    			(map #(sub-tree lists %)
					(->> lists
          				 (filter #(nil? (:parent %)))
               			 (sort-by :entryname)))]
			(list-tbl lists))))

(defn mk-menu-row
	[menu]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	[:tr
		[:td.menu-date-td (hf/label {:class "menu-date"} :x
						  (utils/menu-date-short menu))]
		[:td [:div.menu-txt (hf/label {:class "home-margin"} :x (:entryname menu))]]])

(defn menu-list
	[]
	[:table
		(map mk-menu-row (dbmenus/get-menus (utils/today) (utils/new-menu-end)))])

(defn mk-proj-row
	[r]
	[:tr
		[:td.proj-pri
			(hf/label
				{:class "home-margin"} :dummy
				(str (:priority r)))]
		[:td
			(hf/label
				{:class "home-margin clip"} :dummy
				(:entryname r))]])

(defn projekt-list
	[]
	[:table
		(->> (dbprojects/get-active-projects)
			 (map mk-proj-row))])

(defn recipe-list
	[]
	[:table
		(map (fn [r]
			[:tr
				[:td.home-margin
					[:a.link-thin {:href (str "/user/recipe/" (:_id r))}
					              (:entryname r)]]])
			(sort-by :entryname
            		 #(compare (str/lower-case %1) (str/lower-case %2))
               		 (dbrecipes/get-recipe-names)))])

(defn home-page
	[request]
	(layout/common-refresh request "Shopping" [css-home-tree css-home css-menus]
	  	[:div.column
			(if (want-tree? request)
				[:a.link-flex {:href "/user/home/prio"} "Num"]
				[:a.link-flex {:href "/user/home/tree"} "Tree"])
			[:div.home-box (list-tree request)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/menu"} "Veckomeny"]]
			[:div.home-box (menu-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/projects"} "Projekt"]]
			[:div.home-box (projekt-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/new-recipe"} "Recept"]]
			[:div.home-box (recipe-list)]]
	  	))

(defn set-home-type
	[request list-type]
	(db/set-user-property (common/uid request) :home {:list-type list-type})
	(ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

