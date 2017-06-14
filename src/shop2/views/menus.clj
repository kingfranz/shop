(ns shop2.views.menus
  	(:require 	(shop2 			[db         	:as db]
  								[utils			:as utils])
            	(shop2.views 	[layout     	:as layout]
            					[common     	:as common]
            					[css 			:refer :all])
          		(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(garden 		[core       	:as g]
            					[units      	:as u]
            					[selectors  	:as sel]
            					[stylesheet 	:as ss]
            					[color      	:as color])
            	(clj-time 		[core       	:as t]
            					[local      	:as l]
            					[format     	:as f]
            					[coerce 		:as c]
            					[periodic   	:as p])
            	(hiccup 		[core       	:as h]
            					[def        	:as hd]
            					[element    	:as he]
            					[form       	:as hf]
            					[page       	:as hp]
            					[util       	:as hu])
            	(ring.util 		[anti-forgery 	:as ruaf]
            					[response     	:as ring])
              	(clojure 		[string     	:as str]
            					[pprint 		:as pp]
            					[set        	:as set])))

;;-----------------------------------------------------------------------------

(defn mk-mtag
	[s dt]
	(keyword (str s "-" (utils/menu-date-key dt))))

(defn mk-recipe-link
	[menu r-link?]
	(when (:recipe menu)
		[:a.link-thin {:href (str "/user/recipe/" (:_id (:recipe menu)))}
					  (:entryname (:recipe menu))]))

(defn mk-recipe-add-del
	[menu r-link?]
	(when r-link?
		(if (:recipe menu)
			[:a.link-thin {:href (str "/user/remove-recipe/" (utils/menu-date-key (:date menu)))} "-"]
			[:a.link-thin {:href (str "/user/choose-recipe/" (utils/menu-date-key (:date menu)))} "+"]
			)))

(defn mk-menu-row
	[menu r-link?]
	; "Tue 03-22" "Steamed fish, rice, sauce, greens" ""
	(let [date-id (when (utils/is-today? (:date menu)) "today")]
		[:tr
			[:td.menu-date-td
				(hf/label {:id date-id :class "menu-date"} :x
						  (utils/menu-date-short menu))]
			(if r-link?
				[:td.menu-text-td
					(hf/hidden-field (mk-mtag "id" (:date menu)) (:_id menu))
					(hf/text-field {:class "menu-text"}
								   (mk-mtag "txt" (:date menu))
								   (:entryname menu))]
				[:td.menu-text-td
					(hf/label {:class "menu-text-old"} :x
							  (:entryname menu))])
			[:td.menu-ad-td (mk-recipe-add-del menu r-link?)]
			[:td.menu-link-td (mk-recipe-link menu r-link?)]
			]))

(defn show-menu-page
    [request]
    (layout/common "Veckomeny" [css-menus]
        (hf/form-to
    		[:post "/user/update-menu"]
        	(ruaf/anti-forgery-field)
    		[:table.menu-table
        		[:tr
        			[:td
        				(common/home-button)
        				(hf/submit-button {:class "button button1"} "Updatera!")]]]
	        [:table.menu-table
	        	(map #(mk-menu-row % false) (dbmenus/get-menus (utils/old-menu-start) (utils/today)))
	        	(map #(mk-menu-row % true)  (dbmenus/get-menus (utils/today) (utils/new-menu-end)))])))

;;-----------------------------------------------------------------------------

(defn update-menu!
	[{params :params}]
	(let [db-menus (dbmenus/get-menus (utils/today) (utils/new-menu-end))]
		(doseq [dt (utils/menu-new-range)
				:let [id (get params (mk-mtag "id" dt))
					  txt (get params (mk-mtag "txt" dt))
					  db-menu (some #(when (= (:date %) dt) %) db-menus)]
				:when (and (seq txt) (not= txt (:entryname db-menu)))]
			;(println "update-menu!:" (mk-mtag "txt" dt) id txt db-menu)
			(if (seq id)
				(dbmenus/update-menu (merge db-menu {:entryname txt}))
				(dbmenus/add-menu {:date dt :entryname txt}))))
	(ring/redirect "/user/menu"))

;;-----------------------------------------------------------------------------

(defn add-recipe-to-menu
	[request recipe-id menu-date]
	(dbmenus/add-recipe-to-menu (f/parse menu-date) recipe-id)
	(ring/redirect "/user/menu"))

(defn remove-recipe-from-menu
	[request menu-date]
	(dbmenus/remove-recipe-from-menu (f/parse menu-date))
	(ring/redirect "/user/menu"))

;;-----------------------------------------------------------------------------

(defn choose-recipe
	[request menu-date]
	(layout/common "Välj recept" [css-menus]
		[:table.menu-table
    		[:tr
    			[:td.menu-head-td (common/home-button)]
    			[:td.menu-head-td [:a.link-head {:href "/menu"} "Cancel"]]]]
		[:table.menu-table
			(map (fn [r]
				[:tr
					[:td
						[:a.link-thin {:href (str "/user/add-recipe-to-menu/" (:_id r) "/" menu-date)}
							(:entryname r)]]])
				(dbrecipes/get-recipes))]))

;;-----------------------------------------------------------------------------

