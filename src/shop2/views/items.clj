(ns shop2.views.items
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
                 [garden.core :as g]
                 [garden.units :as u]
                 [garden.selectors :as sel]
                 [garden.stylesheet :as ss]
                 [garden.color :as color]
                 [garden.arithmetic :as ga]
                 [hiccup.core :as h]
                 [hiccup.def :as hd]
                 [hiccup.element :as he]
                 [hiccup.form :as hf]
                 [hiccup.page :as hp]
                 [hiccup.util :as hu]
                 [ring.util.anti-forgery :as ruaf]
                 [ring.util.response :as ring]
                 [environ.core :refer [env]]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.pprint :as pp]
                 [clojure.set :as set]))

;;-----------------------------------------------------------------------------

(defn- get-parents
	[a-list]
	(loop [parent (:parent a-list)
		   acc    #{(:_id a-list)}]
		(if (nil? parent)
			acc
			(recur (:parent parent) (conj acc (:_id parent))))))

(defn- get-list-items
	[a-list]
	(let [items        (get-items)
		  id-parents   (get-parents a-list)
    	  active-items (->> a-list
                            :items
                            (remove #(some? (:finished %)))
                            (map :_id)
                            set)]
		(->> items
       		 (filter #(contains? id-parents (:parent %)))
          	 (remove #(contains? active-items (:_id %))))))

(defn- mk-add-item
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]
       [:td.item-tags-td
        [:div.item-tags (some->> item :tags frmt-tags)]]]]])

(defn- mk-add-item-no-tag
    [item]
    [:div.item-div
     [:table.item-table
      [:tr
       [:td.item-cb-td
        [:div.item-cb (hf/check-box {:class "new-cb"} (:_id item))]]
       [:td.item-txt-td
        [:div.item-txt (:entryname item)]]]]])

(defn- items-by-tags
    [items]
    (for [[k v] items]
        [:tr
            [:td.items-block
                [:table
                 [:tr
                  [:td.tags-head {:style "width: 100%"} (hf/label {:class "tags-head"} :x k)]]]
                (map mk-add-item-no-tag (sort-by :entrynamelc v))]]))

(defn- mk-letter
    [items]
    [:tr [:td.items-block (map mk-add-item (sort-by :entrynamelc items))]])

(defn- items-by-name
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

(defn- item-list
	[a-list sort-type]
	(if (= sort-type :tags)
        (items-by-tags (->> (get-list-items a-list)
                           (map #(update % :tags frmt-tags))
                           (group-by :tags)
                           (into (sorted-map))))
        (items-by-name (->> a-list get-list-items items->alpha))))

(defn- sort-button
    [st list-id]
    (if (= st :tags)
        [:a.link-flex {:href (str "/user/add-items/set-sort/" list-id "/name")} "N"]
        [:a.link-flex {:href (str "/user/add-items/set-sort/" list-id "/tags")} "T"]))

(defn add-items
    [request list-id]
    (try
        (let [a-list (get-list list-id)
              sort-type (or (some-> request udata :properties :items :sort-type keyword) :name)]
            (common request "Välj sak" [css-items css-tags-tbl]
                           (hf/form-to
                            [:post "/user/add-items"]
                            (ruaf/anti-forgery-field)
                            (hf/hidden-field :list-id list-id)
                            [:div
                             (homeback-button (str "/user/list/" list-id))
                             (sort-button sort-type list-id)
                             [:a.link-flex {:href (str "/user/mk-new-item/" list-id)} "+"]
                             [:a.link-flex (hf/submit-button {:class "button-s"} "\u2713")]]
                            [:div
                             [:table (item-list a-list sort-type)]])))
        (catch Throwable e (error-page request "/user/add-items" "" e))))

;;-----------------------------------------------------------------------------

(defn add-items!
	[{params :params :as request}]
    (try
        ;(pp/pprint params)
        (let [list-id (:list-id params)
              item-ids (->> params keys (filter #(s/valid? :shop/_id %)))]
            (doseq [item-id item-ids]
                (item->list list-id item-id))
            (ring/redirect (str "/user/add-items/" list-id))
            )
        (catch Throwable e (error-page request "/user/add-items!" "" e))))

;;-----------------------------------------------------------------------------

(defn info-part
	[]
	(named-div "Information"
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

(defn new-list-item
	[request list-id]
    (try
        (common request "Skapa ny sak" [css-items css-tags-tbl]
		(hf/form-to
    		[:post "/user/new-item"]
        	(ruaf/anti-forgery-field)
        	(hf/hidden-field :list-id list-id)
	        [:div
    			(homeback-button (str "/user/add-items/" list-id))
    			[:a.link-head (hf/submit-button {:class "button"} "Skapa")]]
	        [:div
	        	(info-part)
                (named-div "Ny kategori:" (hf/text-field {:class "new-tag"} :new-tag))
		    	(old-tags-tbl)
		    	]))
        (catch Throwable e (error-page request "/user/new-item" "" e))))

;;-----------------------------------------------------------------------------

(defn new-list-item!
	[{params :params :as request}]
    (try
        (let [old-tag-id (:tags params)
              new-tag-name (str/trim (:new-tag params))
              tag (cond
                      (and (seq old-tag-id) (seq new-tag-name)) (throw (Exception. "Bara en tag"))
                      (seq old-tag-id) (-> old-tag-id get-tag :entryname)
                      (seq new-tag-name) new-tag-name
                      :else "")
			  new-item (add-item
			  			(-> {:entryname (s/assert :shop/string (:new-item-name params))
							 :parent    (:list-id params)}
                            (assoc-str-if :tags   tag)
                            (assoc-num-if :amount (:new-item-amount params))
							(assoc-str-if :unit   (:new-item-unit params))
							(assoc-str-if :url    (:new-item-url params))
							(assoc-num-if :price  (:new-item-price params))))]
			(item->list (:list-id params) (:_id new-item) 1)
			(ring/redirect (str "/user/add-items/" (:list-id params))))
        (catch Throwable e (error-page request "/user/new-item!" "" e))))

;;-----------------------------------------------------------------------------

(defn set-item-sort
    [request listid sort-type]
    (try
        (set-user-property (uid request) :items {:sort-type sort-type})
        (ring/redirect (str "/user/add-items/" listid))
        (catch Throwable e (error-page request "set-item-sort" "" e))))

