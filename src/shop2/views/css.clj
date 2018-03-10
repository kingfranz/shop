(ns shop2.views.css
  	(:require 	(garden [core       :as g]
            			[units      :as u]
            			[selectors  :as sel]
            			[stylesheet :as ss]
            			[color      :as color]
            			[arithmetic :as ga])))

;;-----------------------------------------------------------------------------

(def line-size (u/px 24))
(def transparent (color/rgba 200 200 200 0))
(def full (u/percent 100))
(def half (u/percent 50))

(defn grey% [p] (color/as-rgb (* (/ p 100) 255)))

;;-----------------------------------------------------------------------------

(def css-admin
	(g/css
     [:.date-col {
                     :width (u/px 90)}]
     [:.ddown-col {
                     :width (u/px 300)
                     :font-size line-size
                     :margin (u/px 5)
                     :vertical-align :middle}]
     [:.spacer {
			:width (u/px 30)}]
		[:.vspace {
			:height (u/px 30)}]
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
	    	:width half}
	    	(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:float :none
	    			 :width full}])]
		[:.header {
	    	:margin (u/px 0)}]
		[:.home-box {
			:height (u/px 410)
			:overflow-x :hidden
			:overflow-y :auto
			}]
		[:.proj-pri {:width (u/px 20)}]
		[:.clip {:overflow :hidden
			     :white-space :nowrap}]
		[:.login-txt {
			:font-size line-size
			:color :black
		}]
		[:.uname-txt {
			:font-size line-size
			:color :white
		}]
		[:input.new-cb {
			:margin [[0 (u/px 10) 0 0]]
			:transform "scale(2)"
		}]
		))

;;-----------------------------------------------------------------------------

(def css-auth
	(g/css
		[:html {
			:background (grey% 10)
			:background-size :contain}]
		[:body {
			:font-family ["HelveticaNeue" "Helvetica Neue" "Helvetica" "Arial" "sans-serif"]
			:font-size line-size
			:color :white
	        :-webkit-font-smoothing :antialiased
	        :-webkit-text-size-adjust :none
			}
			(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:font-size (u/px 18)}])]
		[:.login-txt {
			:font-size line-size
			:color :black}
			(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:font-size (u/px 18)}])]))

;;-----------------------------------------------------------------------------

(def css-items
    (g/css
     [:.item-div {
                     :table-layout :fixed
                     :display       :inline-block
                     :vertical-align :middle}]
     [:.item-table {
                       :table-layout :fixed
                       }]
     [:.items-block {
                        :margin        (u/px 4)
                        :display       :inline-block
                        :border        [[(u/px 2) :white :solid]]
                        :border-radius (u/px 8)
                        :padding       (u/px 8)
                        }]
     ;------------------------------------------------------
     ; name field
     [:.item-txt-td {
                        :width         (u/px 225)
                        :display       :inline-block
                        ;:text-overflow :clip
                        }]
     [:.item-txt {
                     :text-align  :left
                     :white-space   :nowrap
                     :overflow      :hidden
                     :text-overflow :clip
                     }]
     ;------------------------------------------------------
     ; checkbox field
     [:.item-cb-td {
                       :display       :inline-block
                       :text-overflow :clip
                       }]
     [:.item-cb {
                    :display       :inline-block
                    }]
     ;------------------------------------------------------
     ; tags field
     [:.item-tags-td {
                         :width (u/px 100)
                         :display       :inline-block
                         }]
     [:.item-tags {
                      :text-align  :left
                      :margin      [[(u/px 5) (u/px 5) (u/px 0) (u/px 5)]]
                      :font-size   (u/px 16)
                      :white-space :nowrap
                      :overflow      :hidden
                      :text-overflow :clip
                      }]

     ;------------------------------------------------------
     [:.sort-div {
                     :margin [[(u/px 10) 0 (u/px 10) 0]]
                     }]
     [:a.r-space {
                     :margin [[0 (u/px 5) 0 0]]
                     }]
     [:a.lr-space {
                      :margin [[0 (u/px 5) 0 (u/px 5)]]
                      }]
     [:a.l-space {
                     :margin [[0 0 0 (u/px 5)]]
                     }]
     [:.url-td {
                   :width (u/px 500)
                   }]
     [:.new-item-txt {
                         :font-size (u/px 24)
                         :width full
                         }]
     [:.tags-head {
                      :font-size (u/px 24)
                      :background-color     (grey% 30)
                      :color                :white
                      :width full
                      }]
     [:.info-head {
                      :font-size (u/px 24)
                      :padding (u/px 10)
                      }]
     ))

;;-----------------------------------------------------------------------------

(def css-lists
	(g/css
		[:.item-text-tr {
			:background :white
			:border     [[(u/px 2) :solid :lightgrey]]}]
		[:.item-text {
			:text-align :left
			:text-decoration :none
			:display    :block
			:color      :black
			:font-size  (u/px 24)
			:padding    (u/px 5)}]
		[:.item-text-td {
			:width (u/percent 90)}]
		[:.item-menu-td {
			:width (u/percent 30)}]
		[:.done {
			:background-color (grey% 50)
			:text-decoration :line-through}]
		[:.done-td {:padding [[(u/px 20) 0 (u/px 10) 0]]}]
		[:.tags-row {
			:text-align :left
			:color :white
			:background-color transparent
			:border [[(u/px 1) :solid :grey]]
			:font-size (u/px 18)
			:padding (u/px 5)
			:height (u/px 20)
		}]

		[:.list-tbl {
			:border [[(u/px 1) :white :solid]]
			:border-radius (u/px 8)
			:padding (u/px 8)
			:margin [[(u/px 5) (u/px 0)]]
		}
		(ss/at-media {:screen true
					  :min-width (u/px 360)}
			[:& {:width full}])
		(ss/at-media {:screen true
					  :min-width (u/px 1024)}
			[:& {:width (u/px 690)}])
		]

		[:.list-name-th {
			:text-align :center
			:background-color transparent
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-name {
			:color :white
			:font-size (u/px 24)
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-add-th {
			:text-align :right
			:background-color transparent
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-add {
			:display :block
			:text-decoration :none
			:color :white
			:font-size (u/px 24)
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-plus {:font-size (u/px 36)}]
		[:.arrow {
			:display :block
			:text-decoration :none
			:color :black
			:width (u/px 25)
		}]
		[:.align-r {:text-align :right}]
		[:.align-l {:text-align :left}]
		))

;;-----------------------------------------------------------------------------

(def css-lists-new
	(g/css
		[:.new-name {
			:color :white
			:font-size (u/px 24)
			:background-color transparent
			:width full
			:border [[(u/px 1) :solid :grey]]
			:margin [[(u/px 0) (u/px 0) 0 0]]
		}]
		[:.new-parent {
			:font-size (u/px 24)
			:margin [[(u/px 0) (u/px 0) 0 0]]
		}]
		))

;;-----------------------------------------------------------------------------

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
			:background-color transparent
			:font-size (u/px 24)
			:width (u/px 182)
			:border [[(u/px 1) :solid :grey]]
			:margin [[0 (u/px 10) 0 0]]
		}]
		[:.new-tags {
			:font-size (u/px 24)
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

;;-----------------------------------------------------------------------------

(def indentation line-size)

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
     [:.admin-btn {
                      :position :absolute ; Reposition logo from the natural layout
                      :left (u/px 9)
                      :top (u/px 10)
                      :z-index 2
                     }]
     [:.column {
	    	:float  :left
	    	:height (u/px 500)
	    	:width half}
	    	(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:float :none
	    			 :width full}])]
		[:.header {
	    	:margin (u/px 0)}]
		[:.home-box {
			:height (u/px 410)
			:overflow-x :hidden
			:overflow-y :auto
			}]
		[:.proj-tags {
			;:width full
			:font-size (u/px 14)
   			:text-align :right
			:border 0
			:color :lightgrey
			:background-color transparent
		}]
		[:.proj-pri {:width (u/px 20)}]
		[:.clip {:overflow :hidden
			     :white-space :nowrap}]
		))

;;-----------------------------------------------------------------------------

(defn mk-lnk
	[padd-v padd-h margin-v margin-h fnt-sz border-sz]
	{
	:background-color     (grey% 30)
 	:color                :white
	:padding              [[(u/px padd-v) (u/px padd-h)]]
	:margin               [[(u/px margin-v) (u/px margin-h)]]
    :text-align           :center
	:text-decoration      :none
	:font-weight          :bold
	:font-size            (u/px fnt-sz)
	:border               [[(u/px border-sz) :white :solid]]
	:border-radius        (u/px 8)
	:display              :inline-block
	:cursor               :pointer})

(def css-misc
	(g/css
		[:a.link-thin:link
		 :a.link-thin:visited (mk-lnk 0 6 0 0 18 1)]
		[:a.link-thin:hover
		 :a.link-thin:active {:background-color :green}]
		[:a.link-thick:link
		 :a.link-thick:visited (mk-lnk 0 6 6 0 24 1)]
		[:a.link-thick:hover
		 :a.link-thick:active {:background-color :green}]
		[:a.link-head:link
		 :a.link-head:visited (assoc (mk-lnk 8 16 4 2 36 2) :width (u/px 250))]
		[:a.link-head:hover
		 :a.link-head:active {:background-color :green}]
		[:a.link-flex:link
		 :a.link-flex:visited (mk-lnk 6 12 4 2 36 2)]
		[:a.link-flex:hover
		 :a.link-flex:active {:background-color :green}]
		[:a.link-shop:link
		 :a.link-shop:visited (mk-lnk 6 12 4 10 24 2)]
		[:a.link-shop:hover
		 :a.link-shop:active {:background-color :green}]
		[:a.link-icon:link
		 :a.link-icon:visited (assoc (mk-lnk 6 12 4 2 36 2) :width (u/px 60))]
		[:a.link-icon:hover
		 :a.link-icon:active {:background-color :green}]
		[:a.link-half:link
		 :a.link-half:visited (assoc (mk-lnk 8 16 4 2 36 2) :width (u/px 120))]
		[:a.link-half:hover
		 :a.link-half:active {:background-color :green}]
		[:div.link-head:link
		 :div.link-head:visited (assoc (mk-lnk 8 16 4 2 36 2) :width (u/px 250))]
		[:div.link-head:hover
		 :div.link-head:active {:background-color :green}]
		[:a.link-home:link
		 :a.link-home:visited (assoc (mk-lnk 8 16 4 2 36 2) :width (u/percent 90))]
		[:a.link-home:hover
		 :a.link-home:active {:background-color :green}]
     [:.button (assoc (mk-lnk 8 16 4 2 32 2) :width (u/px 250))]
     [:.button:hover {:background-color :green}]
     [:.button-120 (assoc (mk-lnk 8 16 4 2 32 2) :width (u/px 120))]
     [:.button-120:hover {:background-color :green}]
     [:.button-s (mk-lnk 8 16 4 2 32 2)]
     [:.button-s:hover {:background-color :green}]
     [:.width-100p {:width (u/percent 100)}]
    		))

(def css-html
	(g/css
		[:html {
			;:background (color/linear-gradient [(color/as-rgb 32) (color/as-rgb 64)])
			:background (grey% 10)
			:background-size :contain}]
		[:body {
			:font-family ["HelveticaNeue" "Helvetica Neue" "Helvetica" "Arial" "sans-serif"]
			:font-size line-size
			:color :white
	        :-webkit-font-smoothing :antialiased
	        :-webkit-text-size-adjust :none
			}]))

(def css-menus
	(g/css
		[:.menu-table {
			;:width (u/px (+ 106 10 500 6 ))
		}]
		[:.menu-head-td {
			:width half
			:text-align :center
		}]
		[:.menu-date-td {
			:width (u/px 100)
			:text-align :right
			:padding [[0 (u/px 10) 0 0]]}
	    	(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:font-size (u/px 18)
	    			 :width (u/px 75)
	    			 :padding [[0 (u/px 5) 0 0]]}])]
		[:.menu-date {
			;:width (u/px 106)
			:padding (u/px 2)
		}]
		[:#today.menu-date {
			:background-color (grey% 80)
		    :color :black
		}]
		[:.menu-text-td {
			:width (u/px 540)
		}]
		[:.menu-text {
			:width (u/percent 95)
			:background (grey% 80)
			:font-size (u/px 24)}
	    	(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:font-size (u/px 18)}])]
		[:.menu-text-old {
		}]
		[:.menu-link-td {
			;:width (u/px 110)
		}]
		[:.menu-ad-td {
			;:width (u/px 30)
		}]
		))


;;-----------------------------------------------------------------------------

(def css-projects
	(g/css
		[:.proj-tbl {
			:width (u/px (+ 43 22 501 202))}
			(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:width full}])]
		[:.proj-head-td {
			:width half
			:text-align :center
		}]
		[:.proj-head-th {
			:height (u/px 50)
			:width full
			:border [[(u/px 1) :white :solid]]
		    :border-radius (u/px 8)
		}]
		[:.proj-head-val {
			:font-size (u/px 36)}
			(ss/at-media {:screen true :max-width (u/px 700)}
	    		[:& {:font-size (u/px 24)}])]
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
			:width full
			:font-size (u/px 18)
			:text-align :center
			:color :black
		}]
		[:.proj-check-val {
			:width full
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
			:width full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color transparent
		}]
		[:.proj-tags-val {
			:width full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color transparent
		}]
		[:.finished-proj {
			:text-decoration :line-through
			:width full
			:font-size (u/px 18)
			:border 0
			:color :white
			:background-color transparent
		}]))

(def css-recipe
	(g/css
		[:.rec-buttons-td {
			:width (u/px 267)
			:text-align :center
		}]
		[:.btn-spacer {
			:height (u/px 25)
		}]
		[:.rec-title-txt-td {
			:width full
			:text-align :center
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.rec-title-txt {
			:width full
			:background-color transparent
			:color :white
			:border 0
			:font-size (u/px 24)
		}]
		[:.recipe-item {
			:background-color transparent
			:font-size (u/px 18)
			:color :white
			:border 0
		}]
		[:.rec-item-td {
			:border [[(u/px 1) :solid :grey]]
			:padding [[0 0 0 (u/px 10)]]
		}]
		[:.recipe-item-name {
			:width (u/px 590)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-item-unit {
			:width (u/px 70)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-item-amount {
			:width (u/px 110)
			:border-collapse :collapse
			:padding 0
		}]
		[:.recipe-table {
			:border-collapse :collapse
		}]
		[:.recipe-url-table {
			:width full
			:height (u/px 48)
		}]
		[:.recipe-url1 {
			:width (u/px 70)
			:font-size (u/px 24)
			:height (u/px 45)
		}]
		[:.recipe-url2 {
			:width (u/px 650)
			:height (u/px 45)
			:padding [[0 (u/px 10) (u/px 6) 0]]
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.recipe-url-field {
			:width full
			:font-size (u/px 18)
			:vertical-align :center
		}]
		[:.rec-area-div {
			:width full
			:height (u/px 300)
		}]
		[:.recipe-area {
			:width full
			:height full
			:border [[(u/px 1) :solid :grey]]}]))

;;-----------------------------------------------------------------------------

(def css-tags
	(g/css
		[:.group {
			:border-collapse :collapse
			:border        [[(u/px 1) :white :solid]]
			:border-radius (u/px 8)
			:margin        [[(u/px 50) 0 (u/px 20) 0]]
			:padding       (u/px 8)
		}]
		[:.head-td {:text-align :center}]
		[:.group-head-th {
			:padding   (u/px 10)
			:font-size (u/px 36)
		}]
		[:.inner {
			:width (u/percent 95)
		}]
		[:.new-item-txt {
			:font-size (u/px 24)
			}]
		))

;;-----------------------------------------------------------------------------

