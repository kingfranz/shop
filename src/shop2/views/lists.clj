(ns shop2.views.lists
  	(:require 	(shop2 			[db                 :as db]
  								[utils    :as utils])
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
            	[shop2.views.home         :as home]
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
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
            	[garden.core              :as g]
            	[garden.units             :as u]
            	[garden.selectors         :as sel]
            	[garden.stylesheet        :as ss]
            	[garden.color             :as color]
            	(ring.util 		[anti-forgery :as ruaf]
            					[response     :as ring])
              	[clojure.string           :as str]
            	[clojure.set              :as set]))

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
			:background-color (layout/grey% 50)
			:text-decoration :line-through}]
		[:.done-td {:padding [[(u/px 20) 0 (u/px 10) 0]]}]
		[:.tags-row {
			:text-align :left
			:color :white
			:background-color layout/transparent
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
			[:& {:width layout/full}])
		(ss/at-media {:screen true
					  :min-width (u/px 1024)}
			[:& {:width (u/px 690)}])
		]

		[:.list-name-th {
			:text-align :center
			:background-color layout/transparent
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-name {
			:color :white
			:font-size (u/px 24)
			:padding [[(u/px 20) 0 (u/px 10) 0]]
		}]
		[:.list-add-th {
			:text-align :right
			:background-color layout/transparent
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

(def css-lists-new
	(g/css
		[:.new-name {
			:color :white
			:font-size (u/px 24)
			:background-color layout/transparent
			:width layout/full
			:border [[(u/px 1) :solid :grey]]
			:margin [[(u/px 0) (u/px 0) 0 0]]
		}]
		[:.new-parent {
			:font-size (u/px 24)
			:margin [[(u/px 0) (u/px 0) 0 0]]
		}]
		))

;;-----------------------------------------------------------------------------

(defn new-list-page
    []
    (layout/common "Skapa ny lista" [css-lists-new]
        (hf/form-to {:enctype "multipart/form-data"}
        	[:post "/new-list"]
        	(ruaf/anti-forgery-field)
    		[:div
    			(common/home-button)
    			(hf/submit-button {:class "button button1"} "Skapa")]
            (common/named-div "Listans namn:"
            	(hf/text-field {:class "new-name"} :entryname))
        	(common/named-div "Överornad lista:"
        		(hf/drop-down {:class "new-parent"}
        					:list-parent
        					(conj (map :entryname (dblists/get-list-names)) common/top-lvl-name)))
            )))

(defn mk-parent-map
	[params]
	(when-not (or (str/blank? (:list-parent params))
				  (= (:list-parent params) common/top-lvl-name))
		(let [p-list (dblists/find-list-by-name (:list-parent params))]
			(select-keys p-list [:_id :entryname :parent]))))

(defn added-list!
	[{params :params}]
	(if (seq (:entryname params))
		(dblists/add-list {:entryname (:entryname params)
					  :parent (mk-parent-map params)})
		(throw (Exception. "list name is blank")))
	(ring/redirect "/"))

;;-----------------------------------------------------------------------------

(defn edit-list-page
    [list-id]
    (let [a-list (dblists/get-list list-id)]
    	(layout/common "Ändra lista" [css-lists-new]
	        (hf/form-to
	        	[:post "/edit-list"]
	        	(ruaf/anti-forgery-field)
	        	(hf/hidden-field :list-id list-id)
	    		[:div
	    			(common/home-button)
	    			[:a.link-head {:href (str "/delete-list/" list-id)} "Ta bort"]
	    			(hf/submit-button {:class "button button1"} "Uppdatera")]
	            (common/named-div "Listans namn:"
	            	(hf/text-field {:class "new-name"} :entryname (:entryname a-list)))
	        	(common/named-div "Överornad lista:"
	        		(hf/drop-down {:class "new-parent"}
	        					:list-parent
	        					(conj (map :entryname (dblists/get-list-names)) common/top-lvl-name)
	        					(some->> a-list :parent :entryname)))
            ))))

(defn edit-list!
	[{params :params}]
	(if (seq (:entryname params))
		(dblists/update-list {:_id (:list-id params)
						 :entryname (:entryname params)
						 :parent (mk-parent-map params)})
		(throw (Exception. "list name is blank")))
	(ring/redirect "/"))

(defn delete-list!
	[list-id]
	(dblists/delete-list list-id)
	(ring/redirect "/"))

;;-----------------------------------------------------------------------------

(defn mk-tags-row
	[tags]
	[:tr [:td.tags-row {:colspan 3} tags]])

(defn mk-name
	[item]
	(if (> (or (:numof item) 1) 1)
		(format "%s (%d)" (:entryname item) (:numof item))
		(:entryname item)))

(defn mk-item-a
	[a-list item active? text]
	[:a.item-text
		{:href (str (if active? "/item-done/" "/item-undo/") (:_id a-list) "/" (:_id item))}
		text])

(defn imenu
	[item]
	(get-in item [:menu :entryname]))

(defn ilink
	[item]
	(get item :url))

(defn mk-item-row*
	[a-list item active?]
	(println "mk-item-row*")
	(list
		[:td.item-text-td (mk-item-a a-list item active? (mk-name item))]
		(when active? (list
			[:td
				[:a.arrow {:href (str "/list-up/" (:_id a-list) "/" (:_id item))} "▲"]]
			[:td
				[:a.arrow {:href (str "/list-down/" (:_id a-list) "/" (:_id item))} "▼"]]
			(cond
				(and (imenu item) (ilink item)) (list
					[:td.item-menu-td
						[:a.item-text {:href "/menu"} "Meny"]]
					[:td.item-menu-td
						[:a.item-text {:href (ilink item) :target "_blank"} "Link"]])
				(imenu item)
					[:td.item-menu-td
						[:a.item-text {:href "/menu"} "Meny"]]
				(ilink item)
					[:td.item-menu-td
						[:a.item-text {:href (ilink item) :target "_blank"} "Link"]])))))

(defn mk-item-row
	[a-list item active?]
	(println "mk-item-row")
	(if active?
		[:tr.item-text-tr      (mk-item-row* a-list item active?)]
		[:tr.item-text-tr.done (mk-item-row* a-list item active?)]))

(defn mk-items
	[a-list row-type]
	(let [filter-func   (if (= row-type :active)
		                    (fn [i] (nil? (:finished i)))
		                    (fn [i] (some? (:finished i))))
		  item-list     (filter filter-func (:items a-list))]
		(for [[tags items] (group-by :tags item-list)
	    	:when (seq items)]
    		(list
    			(->> tags common/frmt-tags mk-tags-row)
	    		(for [item items]
	    			(mk-item-row a-list item (= row-type :active)))))))
	
(defn mk-list-tbl
	[a-list]
	[:table.list-tbl
    	; row with list name
    	[:tr
    		[:td
    			[:table {:style "width:100%"}
    				[:tr
    					[:th.align-l (common/home-button)]
    					[:th.list-name-th
    						(hf/label {:class "list-name"} :xxx
    							(:entryname a-list))]
    					[:th.align-r
    						[:a.link-flex {:href (str "/add-items/" (:_id a-list))} "+"]]]]]]
    	; rows with not-done items
    	[:tr
    		[:td
    			[:table {:style "width:100%"}
					(mk-items a-list :active)]]]
    	[:tr
    		[:td
    			[:table {:style "width:100%"}
    				[:tr
    					[:td.done-td {:colspan "1"} "Avklarade"]
    					[:td.done-td.align-r {:colspan "1"}
    					[:a.link-thin {:href (str "/clean-list/" (:_id a-list))} "Rensa"]]]]]]
        ; rows with done items
        (mk-items a-list :inactive)])

(defn show-list-page
    [list-id]
	(layout/common (:entryname (dblists/get-list list-id)) [css-lists]
    	(loop [listid  list-id
			   acc     []]
			(if (some? listid)
				(let [slist (dblists/get-list listid)]
					(recur (-> slist :parent :_id) (conj acc (mk-list-tbl slist))))
				(seq acc)))))

;;-----------------------------------------------------------------------

(defn item-done
	[list-id item-id]
	(dblists/finish-list-item list-id item-id)
	(ring/redirect (str "/list/" list-id)))

(defn item-undo
	[list-id item-id]
	(dblists/unfinish-list-item list-id item-id)
	(ring/redirect (str "/list/" list-id)))

(defn list-up
	[list-id item-id]
	(dblists/item->list list-id item-id 1)
	(ring/redirect (str "/list/" list-id)))

(defn list-down
	[list-id item-id]
	(dblists/item->list list-id item-id -1)
	(ring/redirect (str "/list/" list-id)))

(defn clean-list
	[list-id]
	(dblists/del-finished-list-items list-id)
	(ring/redirect (str "/list/" list-id)))
