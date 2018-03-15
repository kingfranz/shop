(ns shop2.views.lists
  	(:require 	[shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.views.layout :refer :all]
                 [shop2.views.common       	:refer :all]
                 [shop2.views.css          	:refer :all]
                 [shop2.db.tags :refer :all]
                 [shop2.db.items			:refer :all]
                 [shop2.db.lists 			:refer :all]
                 [shop2.db.menus 			:refer :all]
                 [shop2.db.projects 		:refer :all]
                 [shop2.db.recipes 		:refer :all]
                 [clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 [garden.core :as g]
                 [garden.units        	:as u]
                 [garden.selectors    	:as sel]
                 [garden.stylesheet   	:as ss]
                 [garden.color        	:as color]
                 [garden.arithmetic   	:as ga]
                 [hiccup.core :as h]
                 [hiccup.def          	:as hd]
                 [hiccup.element      	:as he]
                 [hiccup.form         	:as hf]
                 [hiccup.page         	:as hp]
                 [hiccup.util         	:as hu]
                 [ring.util.anti-forgery :as ruaf]
                 [ring.util.response     	:as ring]))

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
		{:href (str (if active? "/user/item-done/" "/user/item-undo/") (:_id a-list) "/" (:_id item))}
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
		(when active? (list
			[:td
				[:a.arrow {:href (str "/user/list-up/" (:_id a-list) "/" (:_id item))} "▲"]]
			[:td
				[:a.arrow {:href (str "/user/list-down/" (:_id a-list) "/" (:_id item))} "▼"]]
			(cond
				(and (imenu item) (ilink item)) (list
					[:td.item-menu-td
						[:a.item-text {:href "/user/menu"} "Meny"]]
					[:td.item-menu-td
						[:a.item-text {:href (ilink item) :target "_blank"} "Link"]])
				(imenu item)
					[:td.item-menu-td
						[:a.item-text {:href "/user/menu"} "Meny"]]
				(ilink item)
					[:td.item-menu-td
						[:a.item-text {:href (ilink item) :target "_blank"} "Link"]])))))

(defn mk-item-row
	[a-list item active?]
	(if active?
		[:tr.item-text-tr      (mk-item-row* a-list item active?)]
		[:tr.item-text-tr.done (mk-item-row* a-list item active?)]))

(defn- sort-items
  	[item-list]
   	(let [items-by-tag (group-by #(frmt-tags (:tags %)) item-list)
          tags (sort (keys items-by-tag))]
      	(map #(hash-map :tag % :items (sort-by :entrynamelc (get items-by-tag %))) tags)))

(defn mk-items
	[a-list row-type]
	(let [filter-func (if (= row-type :active)
		                  (fn [i] (nil? (:finished i)))
		                  (fn [i] (some? (:finished i))))
		  item-list   (filter filter-func (:items a-list))]
		(for [{tags :tag items :items} (sort-items item-list)
	    	:when (seq items)]
    		(list
    			(mk-tags-row tags)
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
    					[:th.align-l (home-button)]
    					[:th.list-name-th
    						(hf/label {:class "list-name"} :xxx
    							(:entryname a-list))]
    					[:th.align-r
    						[:a.link-flex {:href (str "/user/add-items/" (:_id a-list))} "+"]]]]]]
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
    					[:a.link-thin {:href (str "/user/clean-list/" (:_id a-list))} "Rensa"]]]]]]
        ; rows with done items
        (mk-items a-list :inactive)])

(defn show-list-page
    [request list-id]
	(common-refresh request (:entryname (get-list list-id)) [css-lists]
    	(loop [listid  list-id
			   acc     []]
			(if (some? listid)
				(let [slist (get-list listid)]
					(recur (-> slist :parent :_id) (conj acc (mk-list-tbl slist))))
				(seq acc)))))

;;-----------------------------------------------------------------------

(defn item-done
	[request list-id item-id]
	(finish-list-item list-id item-id)
	(ring/redirect (str "/user/list/" list-id)))

(defn item-undo
	[request list-id item-id]
	(unfinish-list-item list-id item-id)
	(ring/redirect (str "/user/list/" list-id)))

(defn list-up
	[request list-id item-id]
	(item->list list-id item-id 1)
	(ring/redirect (str "/user/list/" list-id)))

(defn list-down
	[request list-id item-id]
	(item->list list-id item-id -1)
	(ring/redirect (str "/user/list/" list-id)))

(defn clean-list
	[request list-id]
	(del-finished-list-items list-id)
	(ring/redirect (str "/user/list/" list-id)))
