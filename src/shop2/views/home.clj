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

(def css-home
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
		[:.column {
	    	:float  :left
	    	:height (u/px 500)
	    	:width layout/half}
	    	(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:float :none
	    			 :width layout/full}])]
		[:.header {
	    	:margin (u/px 0)}]
		[:.home-box {
			:height (u/px 410)
			:overflow-x :hidden
			:overflow-y :auto
			}]
		[:.proj-pri {:width (u/px 20)}]
		))

;;-----------------------------------------------------------------------------

(defn mk-list-name
	[slist]
	(str (:entryname slist) " - " (count (filter #(nil? (:finished %)) (:items slist)))))

(defn sub-tree
	[action slist]
	(let [sub-lists (db/get-sub-lists (:_id slist))
		  link      (if (= action :edit)
		  				(str "/edit-list/" (:_id slist))
		  				(str "/list/" (:_id slist)))]
		[:li [:a.link-thick {:href link} (mk-list-name slist)]
			(when (seq sub-lists)
				[:ul (map #(sub-tree action %) sub-lists)])]))

(defn list-tree
	[action]
	[:ul.tree (map #(sub-tree action %) (db/get-top-lists))])

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

(defn pick-list
	[]
	(layout/common "Välj lista" [css-home-tree css-home]
		[:table {:style "width:500px"}
			[:tr
				[:td {:style "width:500px"}
					(common/home-button)]]
			[:tr
				[:td {:style "width:500px"}
					[:div
						(hf/label {} :x "Välj lista att editera")
						(list-tree :edit)]]]]))

(defn home-page
	[]
	(layout/common "Shopping" [css-home-tree css-home]
	  	[:div.column
			[:a.link-half {:href "/new-list" :style "width:40%"} "Ny"]
			[:a.link-half {:href "/pick-list" :style "width:40%"} "Edit"]
			[:div.home-box (list-tree :show)]]
		[:div.column
			[:p.header [:a.link-home {:href "/menu"} "Veckomeny"]]
			[:div.home-box (menu-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/projects"} "Projekt"]]
			[:div.home-box (projekt-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/new-recipe"} "Recept"]]
			[:div.home-box (recipe-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/projects"} "Saker"]]
			[:div.home-box (item-list)]]
		[:div.column
			[:p.header [:a.link-home {:href "/new-recipe"} "Kategorier"]]
			[:div.home-box (tags-list)]]
	  	))

;;-----------------------------------------------------------------------------
