(ns shop2.views.common
  	(:require 	(shop2 			[db             :as db])
  				(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(shop2.views 	[layout       	:as layout])
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
            					[set          	:as set])))

;;-----------------------------------------------------------------------------

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
	;(log/debug params)
	(let [tags (concat (db/spy "old-tags:" (find-old-tags params)) (find-new-tags params))]
		(when-not (empty? tags)
			(dbtags/add-tag-names tags))))

(def css-tags-tbl
	(g/css
		[:.cat-choice {
			:font-size (u/px 24)
			:margin    [[0 (u/px 10) 0 0]]
		}]
		[:.cat-choice-th {
			:text-align :left
			;:font-size (u/px 36)
			:padding [[(u/px 20) (u/px 20) 0 0]]
		}]
		[:.cb-div {
			:float :left
			:text-align :right
			:width (u/px 200)
			;:border [[(u/px 1) :solid :grey]]
			:padding [[0 (u/px 10) 0 0]]
		}]
		[:.new-cb-n {
			:text-align :right
			:margin [[(u/px 5) (u/px 10) (u/px 5) 0]]
		}]
		[:input.new-cb {
			:margin [[0 (u/px 10) 0 0]]
			:transform "scale(2)"
		}]
		[:.new-tag-txt {
			:color :white
			:background-color layout/transparent
			:font-size (u/px 24)
			:width (u/px 182)
			:border [[(u/px 1) :solid :grey]]
			:margin [[0 (u/px 10) 0 0]]
		}]
		[:.new-tags {
			:font-size (u/px 24)
			:width (u/px 600)
		}]
		[:.named-div {
			:margin [[(u/px 20) (u/px 20) (u/px 20) (u/px 20)]]
			:border [[(u/px 1) :solid :grey]]
		}]
		[:.named-div-p {
			:margin [[0 0 (u/px 10) 0]]
		}]
		[:.named-div-l {
			:margin [[0 (u/px 10) 0 0]]
		}]
		))

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
	[:a.link-icon {:href "/"} (he/image "/images/home32w.png")])

(defn back-button
	[target]
	[:a.link-icon {:href target} (he/image "/images/back32w.png")])

(defn homeback-button
	[target]
	(list
		[:a.link-icon {:href "/"} (he/image "/images/home32w.png")]
		[:a.link-icon {:href target} (he/image "/images/back32w.png")]))


