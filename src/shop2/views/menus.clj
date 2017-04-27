(ns shop2.views.menus
  	(:require 	(shop2 			[db         :as db]
  								[utils		:as utils])
            	(shop2.views 	[layout     :as layout]
            					[common     :as common])
          		(garden 		[core       :as g]
            					[units      :as u]
            					[selectors  :as sel]
            					[stylesheet :as ss]
            					[color      :as color])
            	(clj-time 		[core       :as t]
            					[local      :as l]
            					[format     :as f]
            					[coerce 	:as c]
            					[periodic   :as p])
            	(hiccup 		[core       :as h]
            					[def        :as hd]
            					[element    :as he]
            					[form       :as hf]
            					[page       :as hp]
            					[util       :as hu])
            	[ring.util.anti-forgery     :as ruaf]
            	(clojure 		[string     :as str]
            					[set        :as set])))

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
			:font-size (u/px 24)
		}]
		[:.menu-text-old {
		}]
		[:.menu-link-td {
			:width (u/px 110)
		}]))


;;-----------------------------------------------------------------------------

(def delta-days 10)

(defn before-from
	[]
	(c/to-date (t/minus (c/from-date (utils/today)) (t/days delta-days))))

(defn before-to
	[]
	(c/to-date (t/minus (c/from-date (utils/today)) (t/days 1))))

(defn after-from
	[]
	(utils/today))

(defn after-to
	[]
	(c/to-date (t/plus (c/from-date (utils/today)) (t/days delta-days))))

(defn after-range
	[]
	(utils/time-range (l/local-now)
        		       (t/plus (l/local-now) (t/days delta-days))
        		       (t/days 1)))

(defn mk-recipe-link
	[menu r-link?]
	(let [recipe-id (:recipe menu)]
		(if recipe-id
			[:a.link-thin {:href (str "/recipe/" recipe-id)} "Recept"]
			(when r-link?
				[:a.link-thin {:href (str "/choose-recipe/" (utils/menu-date-key menu))} "+"]))))

(defn mk-menu-row
	[menu r-link?]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	(let [date-id (when (utils/is-today? (:date menu)) "today")]
		[:tr
			[:td.menu-date-td
				(hf/label {:id date-id :class "menu-date"} :x
						  (utils/menu-date-show menu))]
			(if r-link?
				[:td.menu-text-td
					(hf/text-field {:class "menu-text"}
								   (utils/menu-date-key menu)
								   (:text menu))]
				[:td.menu-text-td
					(hf/label {:class "menu-text-old"} :x
								(:text menu))])
			[:td.menu-link-td (mk-recipe-link menu r-link?)]]))

(defn mk-menu-rows
	[menu-part]
	(let [range-start (if (= menu-part :old) (before-from) (utils/today))
		  range-end   (if (= menu-part :old) (utils/yesterday) (after-to))
		  menus       (db/get-menus range-start range-end)]
		(for [day (utils/time-range range-start range-end ())]
			)))

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
	        	(mk-menu-rows :old)
	        	(map #(mk-menu-row % false) (db/get-menus (before-from) (utils/yesterday)))
	        	(map #(mk-menu-row % true)  (db/get-menus (utils/today) (after-to)))])))

;;-----------------------------------------------------------------------------

(defn update-menu!
	[{params :params}]
	(doseq [dt (after-range)
			:let [dk (common/menu-date-key dt)]
			:when (seq (get params dk))]
		(db/update-menu dk :text (get params dk))))

;;-----------------------------------------------------------------------------

(defn add-recipe-to-menu
	[recipe-id menu-date]
	(db/add-recipe-to-menu menu-date recipe-id))

;;-----------------------------------------------------------------------------

(defn choose-recipe
	[menu-date]
	(layout/common "Välj recept" [css-menus]
		[:table
			[:tr [:th [:a {:href "/menu"} "Cancel"]]]
			(map (fn [r]
				[:tr
					[:td
						[:a {:href (str "/add-recipe-to-menu/" (:_id r) "/" menu-date)}
							(:entryname r)]]])
				(db/get-recipes))]))

;;-----------------------------------------------------------------------------

