(ns shop2.views.items
  	(:require 	(shop2 			[db         	:as db]
  								[utils      	:as utils])
            	(shop2.views 	[layout     	:as layout]
            				 	[common     	:as common]
            					[home       	:as home]
            					[css        	:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(clj-time       [core       	:as t]
            	                [local      	:as l]
            	                [format     	:as f]
            	                [periodic   	:as p])
            	(garden			[core       	:as g]
            					[units      	:as u]
            					[selectors  	:as sel]
            					[stylesheet 	:as ss]
            					[color      	:as color])
            	(hiccup			[core       	:as h]
            					[def        	:as hd]
            					[element    	:as he]
            					[form       	:as hf]
            					[page       	:as hp]
            					[util       	:as hu])
            	(ring.util 		[response   	:as ring]
              					[anti-forgery 	:as ruaf])
            	(clojure.spec 	[alpha          :as s])
             	(clojure 		[string       	:as str]
            					[set          	:as set]
                                [pprint         :as pp])))

;;-----------------------------------------------------------------------------

(defn get-parents
	[a-list]
	(loop [parent (:parent a-list)
		   acc    #{(:_id a-list)}]
		(if (nil? parent)
			acc
			(recur (:parent parent) (conj acc (:_id parent))))))

(defn get-items
	[a-list]
	(let [items        (dbitems/get-items)
		  id-parents   (get-parents a-list)
    	  active-items (->> a-list
                            :items
                            (remove #(some? (:finished %)))
                            (map :_id)
                            set)]
		(->> items
       		 (filter #(contains? id-parents (:parent %)))
          	 (remove #(contains? active-items (:_id %))))))

(defn mk-add-item
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]
       [:td.item-tags-td
        [:div.item-tags (some->> item :tags common/frmt-tags)]]]]])

(defn mk-add-item-no-tag
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]]]])

(defn items-by-tags
    [items]
    (for [[k v] items]
        [:tr
            [:td.items-block
                [:table
                 [:tr
                  [:td.tags-head {:style "width: 100%"} (hf/label {:class "tags-head"} :x k)]]]
                (map mk-add-item-no-tag (sort-by :entrynamelc v))]]))

(defn mk-letter
    [items]
    [:tr [:td.items-block (map mk-add-item (sort-by :entrynamelc items))]])

(defn items-by-name
    [alpha]
    (map #(mk-letter (get-in alpha [:items %])) (-> alpha :found seq sort)))

(defn- items->alpha
    [items*]
    (loop [items items*
           acc   {:found #{} :items {}}]
        (if (empty? items)
            acc
            (let [item (first items)
                  c*   (-> item :entrynamelc first)
                  c    (if (<= (int \0) (int c*) (int \9)) \0 c*)]
                (recur (rest items) (-> acc (update :found conj c) (update-in [:items c] conj item)))))))

(defn old-items
	[a-list sort-type]
	(if (= sort-type :tags)
        (items-by-tags (->> (get-items a-list)
                           (map #(update % :tags common/frmt-tags))
                           (group-by :tags)
                           (into (sorted-map))))
        (items-by-name (->> a-list get-items items->alpha))))

(defn- sort-button
    [st list-id]
    (if (= st :tags)
        [:a.link-flex {:href (str "/user/add-items/" list-id "/name")} "N"]
        [:a.link-flex {:href (str "/user/add-items/" list-id "/tags")} "T"]))

(defn add-items-page
	[request list-id sort-type]
	(let [a-list (dblists/get-list list-id)]
		(layout/common request "Välj sak" [css-items css-tags-tbl]
                       (hf/form-to
                        [:post "/user/add-items"]
                        (ruaf/anti-forgery-field)
                        (hf/hidden-field :list-id list-id)
                        [:div
                         (common/homeback-button (str "/user/add-items/" list-id))
                         (sort-button sort-type list-id)
                         [:a.link-flex {:href (str "/user/mk-new-item/" list-id)} "+"]
                         [:a.link-flex (hf/submit-button {:class "button-s"} "\u2713")]]
                        [:div
                         [:table (old-items a-list sort-type)]]))))

;;-----------------------------------------------------------------------------

(defn add-items!
	[{params :params :as request}]
    (pp/pprint params)
    (try
        (let [list-id (:list-id params)]
            ;(dblists/item->list list-id item-id 1)
            ;(ring/redirect (str "/user/add-items/" list-id))
            ))
	)

;;-----------------------------------------------------------------------------

(defn info-part
	[]
	(common/named-div "Information"
		[:table
	    	[:tr
				[:td.new-item-td "Namn:"]
				[:td (hf/text-field {:class "new-item-txt" :autofocus true} "new-item-name")]]
			[:tr
				[:td.new-item-td "Enhet:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-unit")]]
			[:tr
				[:td.new-item-td "Mängd:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-amount")]]
			[:tr
				[:td.new-item-td "Pris:"]
				[:td (hf/text-field {:class "new-item-txt"} "new-item-price")]]
			[:tr
				[:td.new-item-td "URL:"]
				[:td.url-td (hf/text-field {:class "new-item-txt"} "new-item-url")]]]))

(defn mk-new-item-page
	[request list-id]
    (layout/common request "Skapa ny sak" [css-items css-tags-tbl]
		(hf/form-to
    		[:post "/user/new-item"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :list-id list-id)
	        [:div
    			(common/homeback-button (str "/user/add-items/" list-id))
    			[:a.link-head (hf/submit-button {:class "button button1"} "Skapa")]]
	        [:div
	        	(info-part)
                (common/named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
		    	(common/old-tags-tbl)
		    	])))

;;-----------------------------------------------------------------------------

(defn new-item!
	[{params :params :as request}]
    (try
        (let [old-tag-id (:tags params)
              new-tag-name (str/trim (:new-tag params))
              tag (cond
                      (and (seq old-tag-id) (seq new-tag-name)) (throw (Exception. "Bara en tag"))
                      (seq old-tag-id) (-> old-tag-id dbtags/get-tag :entryname)
                      (seq new-tag-name) new-tag-name
                      :else "")
			  new-item (dbitems/add-item
			  			(-> {:entryname (s/assert :shop/string (:new-item-name params))
							 :parent    (:list-id params)}
                            (utils/assoc-str-if :tags   tag)
                            (utils/assoc-num-if :amount (:new-item-amount params))
							(utils/assoc-str-if :unit   (:new-item-unit params))
							(utils/assoc-str-if :url    (:new-item-url params))
							(utils/assoc-num-if :price  (:new-item-price params))))]
			(dblists/item->list (:list-id params) (:_id new-item) 1)
			(ring/redirect (str "/user/add-items/" (:list-id params))))
		(catch AssertionError ae
			(mk-new-item-page (assoc request :err-msg (str "ASSERT: " (.getMessage ae)))
							  (:list-id params)))
		(catch Exception e
			(mk-new-item-page (assoc request :err-msg (str "Exception: " (.getMessage e)))
							  (:list-id params)))))

;;-----------------------------------------------------------------------------

