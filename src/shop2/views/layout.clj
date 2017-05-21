(ns shop2.views.layout
  	(:require 	(hiccup [core 	   :as hc]
          				[page 	   :as hp])
          		(garden [core       :as g]
            			[units      :as u]
            			[selectors  :as sel]
            			[stylesheet :as ss]
            			[color      :as color])
          		(clojure [string 	:as str])))

(def line-size (u/px 24))
(def transparent (color/rgba 200 200 200 0))
(def full (u/percent 100))
(def half (u/percent 50))

(defn grey% [p] (color/as-rgb (* (/ p 100) 255)))

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
		 :a.link-thin:visited (mk-lnk 0 6 0 0 24 1)]
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
		 :a.link-flex:visited (mk-lnk 8 16 4 2 36 2)]
		[:a.link-flex:hover
		 :a.link-flex:active {:background-color :green}]
		[:a.link-icon:link
		 :a.link-icon:visited (assoc (mk-lnk 8 16 4 2 36 2) :width (u/px 60))]
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
		[:.button (assoc (mk-lnk 8 16 4 2 36 2) :width (u/px 250))]
		[:.button:hover {:background-color :green}]
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

(defn common
	[title css & body]
	(hp/html5
		[:head {:lang "sv"}
			[:meta {:charset "utf-8"}]
			[:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
			[:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
			[:title title]
			[:style css-html]
			[:style css-misc]
			(map (fn [x] [:style x]) css)]
		[:body
			[:div
				[:p {:style "margin: 0px 0px 3px 2px"} "Shopping 0.6.0"]
				body]]))

(defn four-oh-four
	[]
  	(common "Page Not Found" "/css/home.css"
            [:div {:id "four-oh-four"} "The page you requested could not be found"]))
