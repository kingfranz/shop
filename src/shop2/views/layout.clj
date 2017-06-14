(ns shop2.views.layout
  	(:require 	(hiccup 		[core 	    :as hc]
          						[page 	    :as hp]
          						[form       :as hf]
          						[element    :as he])
  				(shop2.views 	[common     :as common]
  								[css 		:refer :all])
          		(garden 		[core       :as g]
            					[units      :as u]
            					[selectors  :as sel]
            					[stylesheet :as ss]
            					[color      :as color])
          		(clojure 		[string 	:as str])))

(defn common*
	[title css refresh & body]
	(hp/html5
		[:head {:lang "sv"}
			[:meta {:charset "utf-8"}]
			[:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
			[:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
			refresh
			[:title title]
			[:style css-html]
			[:style css-misc]
			(map (fn [x] [:style x]) css)]
		[:body
			[:table
				[:tr
					[:td
						[:a.link-shop {:href "/user/home"}
							(str "Shopping " common/shop-version)]]
					[:td
						[:a {:href "/logout" :style "margin: 0px 10px 10px 20px"}
							(he/image "/images/logout.png")]]
					[:td
						[:a {:href "/admin/" :style "margin: 0px 10px 10px 0px"}
							(he/image "/images/settingsw.png")]]]]
			body]))

(defn common
	[title css & body]
	(common* title css nil body))

(defn common-refresh
	[title css & body]
	(common* title css [:meta {:http-equiv :refresh :content (* 60 5)}] body))

(defn four-oh-four
	[]
  	(common "Page Not Found" "/css/home.css"
            [:div {:id "four-oh-four"} "The page you requested could not be found"]))
