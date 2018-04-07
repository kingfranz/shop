(ns shop2.views.layout
    (:require [hiccup.core :as hc]
              [hiccup.page :as hp]
              [hiccup.form :as hf]
              [hiccup.element :as he]
              [slingshot.slingshot :refer [throw+ try+]]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [environ.core :refer [env]]
              [clojure.string :as str]
              [clojure.pprint :as pp]))

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

(defn- str?
    [s]
    (when-not (str/blank? s)
        s))

(defn- err-block-s
    [header s]
    (when-let [output (some-> s (str/replace "\n" "<br>") str?)]
        [:tr [:td (named-block header [:div.error-msg-s output])]]))

(defn- err-block
    [header s]
    (when-let [output (some-> s (str/replace "\n" "<br>") str?)]
        [:tr [:td (named-block header [:div.error-msg output])]]))

(defn error-page
    [request etype throw-context cause src]
    (try+
        {:status  400
         :headers {"title" "ERROR!"}
         :body    (hp/html5
                      [:style css-html css-misc css-admin css-items]
                      [:table
                       [:tr [:td (home-button)]]
                       [:tr [:td [:label (str "Type " etype)]]]
                       (when src [:tr [:td [:label (str "Source " src)]]])
                       (err-block-s "Cause" cause)
                       (err-block-s "Msg" (:message throw-context))
                       (err-block "Params" (with-out-str (pp/pprint (:params request))))
                       (err-block "Stacktrace" (->> (:stack-trace throw-context)
                                                    seq
                                                    (map StackTraceElement->vec)
                                                    (str/join "<br>")))])}
        (catch Throwable e (println "error-page:" (.getMessage e) "\n" e))))

(st/instrument)
