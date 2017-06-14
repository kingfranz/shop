(ns shop2.views.common
  	(:require 	(shop2 			[db             :as db])
  				(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(clj-time 		[core         	:as t]
            					[local        	:as l]
            					[coerce       	:as c]
            					[format       	:as f]
            					[periodic     	:as p])
            	(taoensso 		[timbre       	:as log])
				(garden 		[core         	:as g]
            					[units        	:as u]
            					[selectors    	:as sel]
            					[stylesheet 	:as ss]
            					[color        	:as color])
            	(hiccup 		[core         	:as h]
            					[def          	:as hd]
            					[element      	:as he]
            					[form         	:as hf]
            					[page         	:as hp]
            					[util         	:as hu])
            	(clojure 		[string       	:as str]
            					[spec 			:as s]
            					[set          	:as set])))

;;-----------------------------------------------------------------------------

(defonce shop-version "0.7.0")

(defonce top-lvl-name "Ingen")
(defonce old-tag-head "old-tag-")
(defonce valid-tag-name "[a-zA-ZåäöÅÄÖ0-9_-]+")
(defonce old-tag-regex (re-pattern (str "^" old-tag-head valid-tag-name "$")))

(defn find-old-tags
	[params]
	(some->> params
			 keys
			 (map name)
			 (filter #(re-matches old-tag-regex %))
			 (map #(str/replace-first % (re-pattern old-tag-head) ""))))

(defn find-new-tags
	[params]
	(if (-> params :new-tags str/blank?)
		[]
		(let [raw-strings (-> params :new-tags (str/split #","))
			  trimmed     (some->> raw-strings (map str/trim) (map str/capitalize))
			  good        (filter #(re-matches (re-pattern valid-tag-name) %) trimmed)]
			(if (= (count trimmed) (count good))
				good
				(throw (ex-info "Invalid tag" {:cause :invalid}))))))

(defn extract-tags
	[params]
	(let [tags (concat (find-old-tags params) (find-new-tags params))]
		(when-not (empty? tags)
			(dbtags/add-tag-names tags))))

(defn named-div
	([d-name input]
	(named-div :break d-name input))
	([line-type d-name input]
	[:div.named-div
		(if (= line-type :inline)
			(hf/label {:class "named-div-l"} :x d-name)
			[:p.named-div-p d-name])
		input
		[:div {:style "clear:both;float:none;"}]]))

(defn frmt-tags
	[tags]
	(->> tags
		 (map :entryname)
		 sort
		 (str/join " ")))

(defn mk-tag-entry
	[tag-strings tname]
	[:div.cb-div
		(hf/label {:class "new-cb-n"} :xxx tname)
		(hf/check-box
			{:id tname :class "new-cb"}
			(keyword (str old-tag-head tname))
			(contains? tag-strings tname))])

(defn old-tags-tbl
	([]
	(old-tags-tbl []))
	([tags]
    (let [tag-strings (set (map :entryname tags))]
    	(named-div "Existerande kategorier:"
	    	(map #(mk-tag-entry tag-strings %)
	    		(sort (map :entryname (dbtags/get-tag-names))))))))

(defn new-tags-tbl
	[]
	(named-div "Nya kategorier:"
	    (hf/text-field {:class "new-tags"} :new-tags)))

(defn home-button
	[]
	[:a.link-flex {:href "/user/home"} (he/image "/images/home32w.png")])

(defn back-button
	[target]
	[:a.link-flex {:href target} (he/image "/images/back32w.png")])

(defn homeback-button
	[target]
	(list
		[:a.link-flex {:href "/user/home"} (he/image "/images/home32w.png")]
		[:a.link-flex {:href target} (he/image "/images/back32w.png")]))

(defn udata
	[req]
	(if-let [current (get-in req [:session :cemerick.friend/identity :current])]
		(if-let [udata (get-in req [:session :cemerick.friend/identity :authentications current])]
			(if (s/valid? :shop/user udata)
				(db/get-user (:username udata))
				(throw (ex-info (s/explain-str :shop/user udata) {:cause (str udata)})))
			(throw (ex-info "invalid session2" {:cause (str udata)})))
		(throw (ex-info "invalid request" {:cause (str req)}))))

(defn uid
	[req]
	(-> req udata :_id))

(defn mk-parent-dd
	[item]
	(let [lists      (dblists/get-list-names)
		  list-names (sort (map :entryname lists))
		  tl-name    (some #(when (= (:_id %) (:parent item)) (:entryname %)) lists)]
		(hf/drop-down {:class "new-item-txt"} :parent list-names tl-name)))


