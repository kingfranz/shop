(ns shop2.views.home
  	(:require 	[shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.lists 			:refer :all]
                 [shop2.views.layout :refer :all]
                 [shop2.views.common       	:refer :all]
                 [shop2.views.css          	:refer :all]
                 [shop2.db.tags :refer :all]
                 [shop2.db.items			:refer :all]
                 [shop2.db.menus 			:refer :all]
                 [shop2.db.projects 		:refer :all]
                 [shop2.db.recipes 		:refer :all]
                 [cemerick.friend :as friend]
                 [cemerick.friend.workflows :as workflows]
                 [cemerick.friend.credentials 	:as creds]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
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
                 [ring.util.response     	:as ring]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 ))

;;-----------------------------------------------------------------------------

(defn want-tree?
	[req]
	(= (->> req udata :properties :home :list-type) "tree"))

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

(defn list-cmp*
  	[x1 x2]
    (if (> (:count x1) (:count x2))
        -1
        (if (< (:count x1) (:count x2))
        	1
        	(compare (:entryname x1) (:entryname x2)))))

(defn list-cmp
  	[x1 x2]
    (cond
      	(and (:last x1) (:last x2)) (list-cmp* x1 x2)
        (and (:last x1) (not (:last x2))) 1
        (and (not (:last x1)) (:last x2)) -1
        :else (list-cmp* x1 x2)))

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
	(let [lists (get-lists-with-count)]
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
						  (menu-date-short menu))]
		[:td [:div.menu-txt (hf/label {:class "home-margin"} :x (:entryname menu))]]])

(defn menu-list
	[]
	[:table
		(map mk-menu-row (get-menus (today) (new-menu-end)))])

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
				(:entryname r))]
		[:td.r-align
			(hf/label
				{:class "proj-tags"} :dummy
				(frmt-tags (:tags r)))]
  	])

(defn projekt-list
	[]
	[:table {:style "width:100%"}
		(->> (get-active-projects)
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
               		 (get-recipe-names)))])

(defn- lo-admin
    [req]
    (if (-> req udata :roles (contains? :admin))
        [:a.link-flex.admin-btn {:href "/admin/"} (he/image "/images/settingsw.png")]
        [:a.link-flex.admin-btn {:href "/logout"} (he/image "/images/logout.png")]))

(defn home-page
	[request]
	(common-refresh request "Shopping" [css-home-tree css-home css-menus]
	  	[:div.column
			(if (want-tree? request)
				[:a.link-home {:href "/user/home/prio"} "Num"]
				[:a.link-home {:href "/user/home/tree"} "Tree"])
            (lo-admin request)
			[:div.home-box (list-tree request)]]
		[:div.column
			[:p.header [:a.link-home {:href "/user/edit-menu"} "Veckomeny"]]
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
	(set-user-property (uid request) :home {:list-type list-type})
	(ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

