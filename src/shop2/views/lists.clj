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
            	[garden.stylesheet        :as stylesheet]
            	[garden.color             :as color]
            	[ring.util.anti-forgery   :as ruaf]
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
		}]))

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
    (layout/common "Skapa ny lista" [css-lists-new]
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
        					(conj (map :entry-name (db/get-lists)) top-lvl-name))]]]
            (common/old-tags-tbl)
            (common/new-tags-tbl)
            
            		)))

;;-----------------------------------------------------------------------------

(defn added-list!
	[{params :params}]
	(let [parent-name (:list-parent params)
		  parent      (if (= parent-name top-lvl-name)
		  				nil
		  				(if (nil? (db/find-list-id parent-name))
		  					(throw (Exception. "Unknown list parent"))
		  					(db/find-list-id parent-name)))
		  listname (:new-list-name params)
		  tags     (common/extract-tags params)]
		(if (seq listname)
			(db/add-list {:_id (db/mk-id)
						  :entry-name listname
						  :tags tags
						  :created (common/now-str)
		  				  :parent parent})
			(layout/common "That didn't work" [css-lists]
				[:h3 "Failed to add list"]
				[:h3 (str "Parent " (:list-parent params) (if parent " is OK" " is unknown"))]
				[:h3 (str "Listname " (:new-list-name params) (if (seq listname) " is OK" " is unknown"))]
				[:h3 (str "Tags " (prn-str tags) (if (seq tags) " is OK" " is unknown"))]
				[:h1 "WTF!"]))))

;;-----------------------------------------------------------------------------


(defn mk-tags-row
	[tags]
	[:tr [:td.tags-row {:colspan "2"} tags]])

(defn mk-name
	[item]
	(if (> (or (:num-of item) 1) 1)
		(format "%s (%d)" (:entry-name (db/get-item (:_id item))) (:num-of item))
		(:entry-name (db/get-item (:_id item)))))

(defn mk-menu-name
	[item]
	(when (:menu item)
		(get-in (db/get-menu) [:items (:menu item) :text])))

(defn mk-item-a
	[a-list func item]
	[:a.item-text
		{:href (str "/item-undone/" (:_id a-list) "/" (:_id item))}
		(func item)])

(defn mk-item-row*
	[a-list item]
	(list
		[:td.item-text-td (mk-item-a a-list mk-name item)]
		[:td.item-menu-td (mk-item-a a-list mk-menu-name item)]))

(defn mk-item-row
	[a-list done? item]
	(if done?
		[:tr.item-text-tr.done (mk-item-row* a-list item)]
		[:tr.item-text-tr      (mk-item-row* a-list item)]))

(defn mk-items
	[a-list]
	(let [item-list     (map #(db/get-item (:_id %)) (:items a-list))
		  upd-tags      (fn [it lt] (common/filter-tags lt it))
		  item-tag-list (map #(update % :tags upd-tags (:tags a-list)) item-list)
		  ]
		(for [[tags items] (group-by :tags item-tag-list)
	    	:when (seq items)]
    		(list
    			(->> tags common/frmt-tags mk-tags-row)
	    		(for [item items]
	    			(mk-item-row a-list false item))))))
	
(defn mk-list-tbl
	[a-list]
	[:table.master-table
    	; row with list name
    	[:tr
    		[:th.list-name-th
    			(hf/label {:class "list-name"} :xxx (:entry-name a-list))]
    		[:th.list-add-th
    			[:a.list-add {:href (str "/add-items/" (:_id a-list))} "+"]]]
    	; rows with not-done items
    	(mk-items a-list)
    	[:tr [:td.done-td {:colspan "2"} "Avklarade"]]
        ; rows with done items
        (for [item (:done-items a-list)]
    		(mk-item-row a-list true item))])

(defn show-list-page
    [the-list-id]
    (layout/common (:entry-name (db/get-list the-list-id)) [css-lists]
    	[:h3 [:a.link-head {:href "/"} "Home"]]
        (loop [list-id  the-list-id
			   acc      []]
			(if-let [slist (db/get-list list-id)]
				(recur (:parent slist)
					   (conj acc (mk-list-tbl slist)))
				(seq acc)))))

;;-----------------------------------------------------------------------.done-td