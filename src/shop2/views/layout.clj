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
            	(environ 		[core 		:refer [env]])
          		(clojure 		[string 	:as str])))

;;-----------------------------------------------------------------------------

(defn common*
	[request title css refresh & body]
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
				(when (some? (:err-msg request))
					[:tr
						[:td {:colspan 3} (:err-msg request)]])]
			body]))

;;-----------------------------------------------------------------------------

(defn common
	[request title css & body]
	(common* request title css nil body))

(defn common-refresh
	[request title css & body]
	(common* request title css [:meta {:http-equiv :refresh :content (* 60 5)}] body))

;;-----------------------------------------------------------------------------

(defn four-oh-four
	[]
  	(common "Page Not Found" "/css/home.css"
            [:div {:id "four-oh-four"} "The page you requested could not be found"]))

;;-----------------------------------------------------------------------------
