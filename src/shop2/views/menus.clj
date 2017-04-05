(ns shop2.views.menus
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
          		[garden.core       :as g]
            	[garden.units      :as u]
            	[garden.selectors  :as sel]
            	[garden.stylesheet :as stylesheet]
            	[garden.color      :as color]
            	[clj-time.core            :as t]
            	[clj-time.local           :as l]
            	[clj-time.format          :as f]
            	[clj-time.periodic        :as p]
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

(def css-menus
	(g/css
		[:.menu-table {
			:width layout/full
		}]
		[:.menu-head-td {
			:width layout/half
			:text-align :center
		}]
		[:.menu-date-td {
			:width (u/px 150)
			:text-align :right
			:padding [[0 (u/px 15) 0 0]]
		}]
		[:.menu-date {
			:padding (u/px 4)
		}]
		[:#today.menu-date {
			:background-color (layout/grey% 80)
		    :color :black
		}]
		[:.menu-text-td {
			:width (u/px 540)
		}]
		[:.menu-text {
			:width (u/percent 95)
			:background (layout/grey% 80)
		}]
		[:.menu-text-old {
		}]
		[:.menu-link-td {
			:width (u/px 110)
		}]))


(def delta-days 10)

(defn before-range
	[]
	(common/time-range (t/minus (l/local-now) (t/days delta-days))
	        		   (t/minus (l/local-now) (t/days 1))
	        		   (t/days 1)))

(defn after-range
	[]
	(common/time-range (l/local-now)
	        		   (t/plus (l/local-now) (t/days delta-days))
	        		   (t/days 1)))

(defn mk-recipe-link
	[menu r-link? dt]
	(let [recipe-id (get-in menu [:items (common/menu-date-key dt) :recipe])]
		(if recipe-id
			[:a.link-thin {:href (str "/recipe/" recipe-id)} "Recept"]
			(when r-link?
				[:a.link-thin {:href (str "/choose-recipe/" (common/menu-date-key dt))} "+"]))))

(defn mk-menu-row
	[menu r-link? dt]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	(let [date-id (when (common/is-today? dt) "today")]
		[:tr
			[:td.menu-date-td (hf/label {:id date-id :class "menu-date"} :dummy (common/menu-date-show dt))]
			(if r-link?
				[:td.menu-text-td (hf/text-field {:class "menu-text"} (common/menu-date-key dt)
								(get-in menu [:items (common/menu-date-key dt) :text]))]
				[:td.menu-text-td (hf/label {:class "menu-text-old"} :dummy
								(get-in menu [:items (common/menu-date-key dt) :text]))])
			[:td.menu-link-td (mk-recipe-link menu r-link? dt)]]))

(defn show-menu-page
    []
    (layout/common "Veckomeny" [css-menus]
        (hf/form-to {:enctype "multipart/form-data"}
    		[:post "/update-menu"]
        	(ruaf/anti-forgery-field)
    		[:table.menu-table
        		[:tr
        			[:td.menu-head-td [:a.link-head {:href "/"} "Home"]]
        			[:td.menu-head-td (hf/submit-button {:class "button button1"} "Updatera!")]]]
	        [:table.menu-table
	        	(map #(mk-menu-row (db/get-menu) false %) (before-range))
	        	(map #(mk-menu-row (db/get-menu) true %) (after-range))])))

;;-----------------------------------------------------------------------------

(defn update-menu!
	[{params :params}]
	(doseq [dt (after-range)
			:let [dk (common/menu-date-key dt)]
			:when (some? (get params dk))]
		(db/update-menu dk :text (get params dk))))

(defn add-recipe-to-menu
	[recipe-id menu-date]
	(db/update-menu menu-date :recipe recipe-id))

(defn choose-recipe
	[menu-date]
	(layout/common "Välj recept" [css-menus]
		[:table
			[:tr [:th [:a {:href "/menu"} "Cancel"]]]
			(map (fn [r] [:tr [:td [:a {:href (str "/add-recipe-to-menu/" (:_id r) "/" menu-date)}
										(:entry-name r)]]]) (db/get-recipes))]))

