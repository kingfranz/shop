(ns shop2.views.home
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
            	[garden.core       :as g]
            	[garden.units      :as u]
            	[garden.selectors  :as sel]
            	[garden.stylesheet :as stylesheet]
            	[garden.color      :as color]
            	[garden.arithmetic        :as ga]
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

(def indentation layout/line-size)

(def css-home
	(g/css
		[:.home-header {
	    	:width             (u/percent 50)
	    	:height            (u/px 36)}]
		[:a.home-header-link:link
		 :a.home-header-link:visited {
		    :background-color (color/as-rgb 200)
		    :color            :black
		    :padding          [[(u/px 10) (u/px 6)]]
		    :text-align       :center
		    :text-decoration  :none
		    :font-size         (u/px 48)
	    	:font-weight      :bold
		    :border           [[(u/px 1) :green :solid]]
		    :border-radius    (u/px 8)
			:display          :inline-block
			:width            (u/percent 90)}
			[:&:hover
			 :&:active {
    			:background-color :green
    			:color :white}]]
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
		[:.r-align {
			:text-align :right}]
		[:.v-align {
			:text-align :left}]
		[:.home-margin {
			:padding [[0 0 0 (u/px 24)]]}]))

;;-----------------------------------------------------------------------------

(defn sub-lists
	[slist]
	(filter #(= (:parent %) (:_id slist)) (db/get-lists)))

(defn sub-tree
	[slist]
	[:li [:a.link-thick {:href (str "/list/" (:_id slist))} (:entry-name slist)]
		(when (seq (sub-lists slist))
			[:ul (map sub-tree (sub-lists slist))])])

(defn top-lists
	[]
	(filter #(nil? (:parent %)) (db/get-lists)))

(defn list-tree
	[]
	[:ul.tree (map sub-tree (top-lists))])

(defn get-menu-txt
	[menu dt]
	(get-in menu [:items (common/menu-date-key dt) :text]))

(defn mk-menu-row
	[menu dt]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	[:tr
		[:td.home-margin.r-align (common/menu-date-short dt)]
		[:td (hf/label {:class "home-margin"} :dummy (get-menu-txt menu dt))]])

(defn menu-range
	[]
	(common/time-range  (l/local-now)
	        			(t/plus (l/local-now) (t/days 10))
	        			(t/days 1)))

(defn menu-list
	[]
	[:table
		(map #(mk-menu-row (db/get-menu) %) (menu-range))])

(defn mk-proj-row
	[r]
	[:tr [:td (hf/label {:class "home-margin"} :dummy (:entry-name r))]])

(defn projekt-list
	[]
	[:table
		(->> (db/get-projects)
			(remove :finished)
			(sort-by :priority)
			(take 10)
			(map mk-proj-row))])

(defn recipe-list
	[]
	[:table
		(map (fn [r] [:tr [:td.home-margin [:a.link-thin {:href (str "/recipe/" (:_id r))} (:entry-name r)]]])
			(take 10 (db/get-recipes)))])

(defn home-page
	[]
	(layout/common "Shopping" [css-home]
	  	[:table
	  		[:tr
	  			[:th.home-header [:a.link-head {:href "/new-list"} "Listor"]]
	  			[:th.home-header [:a.link-head {:href "/menu"} "Veckomeny"]]]
	  		[:tr
	  			[:td (list-tree)]
	  			[:td (menu-list)]]
	  		[:tr
	  			[:th.home-header [:a.link-head {:href "/projects"} "Projekt"]]
	  			[:th.home-header [:a.link-head {:href "/new-recipe"} "Recept"]]]
	  		[:tr
	  			[:td {:valign "top"} (projekt-list)]
	  			[:td {:valign "top"} (recipe-list)]]
	  	]))

;;-----------------------------------------------------------------------------
