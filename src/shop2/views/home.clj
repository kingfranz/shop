(ns shop2.views.home
  	(:require 	(shop2 		 [db           :as db]
  							 [utils        :as utils])
            	(shop2.views [layout       :as layout]
            				 [common       :as common])
            	(clj-time 	 [core            :as t]
            				 [local           :as l]
            				 [coerce          :as c]
            				 [format          :as f]
            				 [periodic        :as p])
            	(garden 	 [core       :as g]
            				 [units      :as u]
            				 [selectors  :as sel]
            				 [stylesheet :as ss]
            				 [color      :as color]
            				 [arithmetic        :as ga])
            	(hiccup 	 [core              :as h]
            				 [def               :as hd]
            				 [element           :as he]
            				 [form              :as hf]
            				 [page              :as hp]
            				 [util              :as hu])
            	(ring.util 	 [anti-forgery   :as ruaf])
            	(clojure 	 [string           :as str]
            				 [set              :as set])))

;;-----------------------------------------------------------------------------

(def indentation layout/line-size)

(def css-home-header
	(g/css
		[:.home-header {
	    	:width             (u/percent 50)
	    	:height            (u/px 36)}]))

(def css-home-tree
	(g/css
		[:.tree
		 ".tree ul" {
			:margin [[0 0 0 indentation]] ;/* indentation */
			:padding 0
			:list-style :none
			:color :white
			:position :relative}]
		[".tree ul" {
		 	:margin-left (u/px-div indentation 2)}]
		[:.tree:before
		 ".tree ul:before" {
			:content "\"\""
			:display :block
			:width 0
			:position :absolute
			:top 0
			:bottom 0
			:left 0
			:border-left [[(u/px 1) :solid]]}]
		[".tree li" {
			:margin 0
			:padding [[0 0 0 (u/px+ indentation (u/px 12))]] ;/* indentation + .5em */
			:line-height (u/px 36) ;/* default list item's `line-height` */
			:font-weight :bold
			:position :relative}]
		[".tree li:before" {
			:content "\"\""
			:display :block
			:width indentation ;/* same with indentation */
			:height 0
			:border-top [[(u/px 1) :solid]]
			:margin-top (u/px -1) ;/* border top width */
			:position :absolute
			:top (u/px 18) ;/* (line-height/2) */
			:left 0}]
		[".tree li:last-child:before" {
			:height :auto
			:top (u/px 18) ;/* (line-height/2) */
			:bottom 0}]
		[:.tree-top {:vertical-align :top}]))

(def css-home-misc
	(g/css
		[:.date-col {
			:width (u/px 90)}]
		[:.r-align {
			:text-align :right}]
		[:.v-align {
			:text-align :left}]
		[:.home-margin {
			:padding [[0 0 0 (u/px 12)]]}]
		[:.menu-txt {
			:height (u/px 30)
			:overflow :hidden
		}]
		[:.home-box {
			:height (u/px 400)
			:overflow-x :hidden
			:overflow-y :auto
			}]
		[:.proj-pri {:width (u/px 20)}]
		[:.home-table {:width (u/px 1080)}]
		))

;;-----------------------------------------------------------------------------

(defn mk-list-name
	[slist]
	(str (:entryname slist) " - " (count (filter #(nil? (:finished %)) (:items slist)))))

(defn sub-tree
	[slist]
	(let [sub-lists (db/get-sub-lists (:_id slist))]
		[:li [:a.link-thick {:href (str "/list/" (:_id slist))} (mk-list-name slist)]
			(when (seq sub-lists)
				[:ul (map sub-tree sub-lists)])]))

(defn list-tree
	[]
	[:ul.tree (map sub-tree (db/get-top-lists))])

(defn mk-menu-row
	[menu]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	[:tr
		[:td.home-margin.r-align.date-col (utils/menu-date-short menu)]
		[:td [:div.menu-txt (hf/label {:class "home-margin"} :x (:entryname menu))]]])

(defn menu-list
	[]
	[:table
		(map mk-menu-row (db/get-menus (utils/today) (utils/new-menu-end)))])

(defn mk-proj-row
	[r]
	[:tr
		[:td.proj-pri
			(hf/label
				{:class "home-margin"} :dummy
				(str (:priority r)))]
		[:td
			(hf/label
				{:class "home-margin"} :dummy
				(:entryname r))]])

(defn projekt-list
	[]
	[:table
		(->> (db/get-active-projects)
			 (map mk-proj-row))])

(defn recipe-list
	[]
	[:table
		(map (fn [r]
			[:tr
				[:td.home-margin
					[:a.link-thin {:href (str "/recipe/" (:_id r))}
					              (:entryname r)]]])
			(take 10 (db/get-recipes)))])

(defn item-list
	[]
	[:table
		(map (fn [i]
			[:tr
				[:td.home-margin
					[:a.link-thin {:href (str "/item/" (:_id i))}
					              (:entryname i)]]])
			(sort-by #(str/lower-case (:entryname %)) (db/get-items)))])

(defn tags-list
	[]
	[:table
		(map (fn [t]
			[:tr
				[:td.home-margin
					[:a.link-thin {:href (str "/tag/" (:_id t))}
					              (:entryname t)]]])
			(sort-by #(str/lower-case (:entryname %)) (db/get-tags)))])

(defn home-page
	[]
	(layout/common "Shopping" [css-home-header css-home-tree css-home-misc]
	  	[:table.home-table
	  		[:tr
	  			[:th.home-header [:a.link-home {:href "/new-list"} "Listor"]]
	  			[:th.home-header [:a.link-home {:href "/menu"} "Veckomeny"]]]
	  		[:tr
	  			[:td.tree-top [:div.home-box (list-tree)]]
	  			[:td.tree-top (menu-list)]]
	  		[:tr
	  			[:th.home-header [:a.link-home {:href "/projects"} "Projekt"]]
	  			[:th.home-header [:a.link-home {:href "/new-recipe"} "Recept"]]]
	  		[:tr
	  			[:td.tree-top [:div.home-box (projekt-list)]]
	  			[:td.tree-top [:div.home-box (recipe-list)]]]
	  		[:tr
	  			[:th.home-header [:a.link-home {:href "/projects"} "Saker"]]
	  			[:th.home-header [:a.link-home {:href "/new-recipe"} "Kategorier"]]]
	  		[:tr
	  			[:td.tree-top [:div.home-box (item-list)]]
	  			[:td.tree-top [:div.home-box (tags-list)]]]
	  	]))

;;-----------------------------------------------------------------------------
