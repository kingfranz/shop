(ns shop2.views.lists
  	(:require 	[shop2.db                 :as db]
            	[shop2.views.layout       :as layout]
            	[shop2.views.common       :as common]
            	[shop2.views.home         :as home]
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
			:width (u/percent 70)}]
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
		}]
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
			:text-align :center
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
		[:.list-plus {:font-size (u/px 36)}]))

(def css-lists-new
	(g/css
		[:.new-name {
			:color :white
			:font-size (u/px 24)
			:background-color layout/transparent
			:border [[(u/px 1) :solid :grey]]
			:margin [[(u/px 20) (u/px 20) 0 0]]
		}]
		[:.new-parent {
			:font-size (u/px 24)
			:margin [[(u/px 20) (u/px 20) 0 0]]
		}]
		[:.new-lbl {
			:color :white
			:font-size (u/px 24)
			;:margin [[0 (u/px 10) 0 0]]
		}]
		[:.new-lbl-td {
			:text-align :right
			:background-color layout/transparent
			:border 0
			:width (u/px 200)
			:padding [[(u/px 20) (u/px 20) 0 0]]
		}]))


(def top-lvl-name "Ingen")

(defn new-list-page
    []
    (layout/common "Skapa ny lista" [css-lists-new common/css-tags-tbl]
        (hf/form-to {:enctype "multipart/form-data"}
        	[:post "/new-list"]
        	(ruaf/anti-forgery-field)
    		[:table.master-table
    			[:tr
    				[:td
    					[:a.link-head {:href "/"} "Home"]]
    				[:td
    					(hf/submit-button {:class "button button1"} "Skapa")]]]
            [:table.master-table
            	[:tr
            		[:td.new-lbl-td
            			(hf/label {:class "new-lbl"} :xxx "Listans namn:")]
            		[:td.new-name-td
            			(hf/text-field {:class "new-name"} "new-list-name")]]
        		[:tr
        			[:td.new-lbl-td
        				(hf/label {:class "new-lbl"} :xxx "Överornad lista:")]
        			[:td.new-name-td
        				(hf/drop-down {:class "new-parent"}
        					:list-parent
        					(conj (db/get-list-names) top-lvl-name))]]]
            (common/old-tags-tbl)
            (common/new-tags-tbl))))

;;-----------------------------------------------------------------------------

(defn added-list!
	[{params :params}]
	(let [parent-name (:list-parent params)
		  parent      (when (not= parent-name top-lvl-name)
		  				(or (db/find-list-id parent-name)
		  					(throw (Exception. "Unknown list parent"))))
		  listname (:new-list-name params)
		  tags     (common/extract-tags params)]
		(if (seq listname)
			(db/add-list {:entryname listname
						  :tags tags
						  :parent parent})
			(throw (Exception. (str "Failed to add list, "
				"Parent " (:list-parent params) (if parent " is OK" " is unknown") ", "
				"Listname " (:new-list-name params) (if (seq listname) " is OK" " is unknown") ", "
				"Tags " (prn-str tags) (if (seq tags) " is OK" " is unknown"))))))
	(ring/redirect "/"))

;;-----------------------------------------------------------------------------


(defn mk-tags-row
	[tags]
	[:tr [:td.tags-row {:colspan "2"} tags]])

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
	(list
		[:td.item-text-td (mk-item-a a-list item active? (mk-name item))]
		(cond
			(and (imenu item) (ilink item)) (list
				[:td.item-menu-td [:a.item-text {:href "/menu"} "Meny"]]
				[:td.item-menu-td [:a.item-text {:href (ilink item) :target "_blank"} "Link"]])
			(imenu item)
				[:td.item-menu-td [:a.item-text {:href "/menu"} "Meny"]]
			(ilink item)
				[:td.item-menu-td [:a.item-text {:href (ilink item) :target "_blank"} "Link"]])))

(defn mk-item-row
	[a-list active? item]
	(if active?
		[:tr.item-text-tr      (mk-item-row* a-list item active?)]
		[:tr.item-text-tr.done (mk-item-row* a-list item active?)]))

(defn mk-items
	[a-list row-type]
	(let [filter-func   (if (= row-type :active)
		                    (fn [i] (nil? (:finished i)))
		                    (fn [i] (some? (:finished i))))
		  item-list     (filter filter-func (:items a-list))
		  upd-tags      (fn [it lt] (common/filter-tags lt it))
		  item-tag-list (map #(update % :tags upd-tags (:tags a-list)) item-list)
		  ]
		(for [[tags items] (group-by :tags item-tag-list)
	    	:when (seq items)]
    		(list
    			(->> tags common/frmt-tags mk-tags-row)
	    		(for [item items]
	    			(mk-item-row a-list (= row-type :active) item))))))
	
(defn mk-list-tbl
	[a-list]
	[:table.master-table
    	; row with list name
    	[:tr
    		[:th.list-name-th
    			(hf/label {:class "list-name"} :xxx (:entryname a-list))]
    		[:th.list-add-th
    			[:a.list-add {:href (str "/add-items/" (:_id a-list))}
    				(hf/label {:class "list-plus"} :x "+")]]]
    	; rows with not-done items
    	(mk-items a-list :active)
    	[:tr [:td.done-td {:colspan "2"} "Avklarade"]]
        ; rows with done items
        (mk-items a-list :inactive)])

(defn show-list-page
    [list-id]
	(layout/common (:entryname (db/get-list list-id)) [css-lists]
    	[:h3 [:a.link-head {:href "/"} "Home"]]
        (loop [listid  list-id
			   acc     []]
			(if (some? listid)
				(let [slist (db/get-list listid)]
					(recur (:parent slist) (conj acc (mk-list-tbl slist))))
				(seq acc)))))

;;-----------------------------------------------------------------------

(defn item-done
	[list-id item-id]
	(db/finish-list-item list-id item-id)
	(ring/redirect (str "/list/" list-id)))

(defn item-undo
	[list-id item-id]
	(db/unfinish-list-item list-id item-id)
	(ring/redirect (str "/list/" list-id)))


