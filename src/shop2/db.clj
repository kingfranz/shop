(ns shop2.db
	(:require 	(clj-time	[core     :as t]
            				[local    :as l]
            				[coerce   :as c]
            				[format   :as f]
            				[periodic :as p])
            	(clojure 	[set      :as set]
            				[pprint   :as pp]
            				[spec     :as s]
            				[string   :as str])
            	(cheshire 	[core     :refer :all])
            	(taoensso 	[timbre   :as log])
            	(monger 	[core     :as mg]
            				[credentials :as mcr]
            				[collection :as mc]
            				[joda-time  :as jt]
            				[operators :refer :all])
            	(shop2 		[utils       :as utils]
            				[spec       :as spec])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defonce db-conn  (mg/connect))
(defonce shopdb   (mg/get-db db-conn "shopdb"))
(defonce lists    "lists")
(defonce recipes  "recipes")
(defonce menus    "menus")
(defonce projects "projects")
(defonce items    "items")
(defonce tags     "tags")
(defonce sessions "sessions")
(defonce item-usage "item-usage")

;;-----------------------------------------------------------------------------

(defmacro q-valid? [sp v]
  `(q-valid* ~*file*
  	         ~(:line (meta &form))
  	         ~sp
  	         ~v))

(defn spy
	[v]
	(prn (type v))
	(pp/pprint v)
	v)

(defn q-valid*
	[f l sp v]
	;(println "\nq-valid:" (str f ":" l) (pr-str sp) (pr-str v))
	(if-not (s/valid? sp v)
		(do
			(println "\n---------- " f l " ------------")
			(prn v)
			(println "---------------------------------------")
			(prn (s/explain-str sp v))
			(println "---------------------------------------"))
		true))

(defn p-trace
	[s v]
	(log/trace "\n" s "return:\n" (pr-str v) "\n")
	true)

(defn mk-id
	[]
	(str (java.util.UUID/randomUUID)))

(defn mk-std-field
	[]
	{:_id (mk-id) :created (utils/now)})

(defn get-tags
	[]
	{:post [(p-trace "get-tags" %) (q-valid? :shop/tags %)]}
	(log/trace "get-tags: (mc/find-maps shopdb tags)")
	(mc/find-maps shopdb tags))

(defn get-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-tag" %) (q-valid? :shop/tag %)]}
	(log/trace "get-tag: (mc/find-mapmap-by-id shopdb tags " id ")")
	(mc/find-map-by-id shopdb tags id))

(defn delete-tag
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(log/trace "delete-tag: (mc/remove-by-id shopdb tags " id ")")
	(mc/remove-by-id shopdb tags id))

(defn delete-tag-all
	[id]
	{:pre [(q-valid? :shop/_id id)]}
	(log/trace "delete-tag: (mc/remove-by-id shopdb tags " id ")")
	(delete-tag id)
	(mc/update shopdb lists {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb recipes {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb menus {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb projects {} {$pull {:tags {:_id id}}} {:multi true})
	(mc/update shopdb items {} {$pull {:tags {:_id id}}} {:multi true})
	)

(defn update-tag
	[tag-id tag-name]
	{:pre [(q-valid? :shop/_id tag-id)]
	 :post [(p-trace "update-tag" %)]}
	(log/trace "update-tag: (mc/update-by-id shopdb tags (:_id " tag-id ") {$set {:entryname " tag-name "}})")
	(mc/update-by-id shopdb tags (:_id tag-id)
		{$set {:entryname tag-name}}))

(defn add-item-usage
	[list-id item-id action numof]
	(log/trace "add-item-usage")
	(mc/insert shopdb item-usage
		(merge {:listid list-id :itemid item-id :action action :numof numof}
			   (mk-std-field))))

(defn get-tag-names
	[]
	{:post [(p-trace "get-tag-names" %) (q-valid? :shop/strings %)]}
	(some->> (get-tags)
			 (map :entryname)))

(defn get-list
	[listid]
	{:pre [(q-valid? :shop/_id listid)]
	 :post [(p-trace "get-list" %) (q-valid? :shop/list %)]}
	(log/trace "get-list: (mc/find-one-as-map shopdb lists {:_id " listid "})")
	(mc/find-one-as-map shopdb lists {:_id listid}))

(defn get-lists
	[]
	{:post [(p-trace "get-listss" %) (q-valid? :shop/lists %)]}
	(log/trace "get-lists: (mc/find-maps shopdb lists)")
	(mc/find-maps shopdb lists))

(defn get-list-names
	[]
	{:post [(p-trace "get-list-names" %) (q-valid? :shop/strings %)]}
	(some->> (get-lists)
			 (map :entryname)))

(defn get-top-lists
	[]
	{:post [(p-trace "get-top-lists" %) (q-valid? :shop/lists %)]}
	(log/trace "get-top-lists: (mc/find-maps shopdb lists {:parent nil})")
	(mc/find-maps shopdb lists {:parent nil}))

(defn get-sub-lists
	[listid]
	{:pre [(q-valid? :shop/_id listid)]
	 :post [(p-trace "get-sub-lists" %) (q-valid? :shop/lists %)]}
	(log/trace "get-sub-lists: (mc/find-maps shopdb lists {:parent " listid "})")
	(mc/find-maps shopdb lists {:parent listid}))

(defn get-recipes
	[]
	{:post [(p-trace "get-recipes" %) (q-valid? :shop/recipes %)]}
	(log/trace "get-recipes: (mc/find-maps shopdb recipes)")
	(mc/find-maps shopdb recipes))

(defn get-recipe
	[id]
	{:pre [(q-valid? :shop/_id id)] :post [(p-trace "get-recipe" %) (q-valid? :shop/recipe %)]}
	(log/trace "get-recipe: (mc/find-maps shopdb recipes {:_id " id "})")
	(mc/find-one-as-map shopdb recipes {:_id id}))

(defn get-projects
	[]
	{:post [(p-trace "get-projects" %) (q-valid? :shop/projects %)]}
	(log/trace "get-projects: (mc/find-maps shopdb projects)")
	(mc/find-maps shopdb projects))

(defn get-active-projects
	[]
	{:post [(p-trace "get-active-projects" %) (q-valid? :shop/projects %)]}
	(log/trace "get-active-projects: (mc/find-maps shopdb projects {:finished nil})")
	(->> (mc/find-maps shopdb projects {:finished nil})
		 (sort-by :priority)))

(defn get-project
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-project" %) (q-valid? :shop/project %)]}
	(log/trace "get-project: (mc/find-one-as-map shopdb projects {:_id " id "})")
	(mc/find-one-as-map shopdb projects {:_id id}))

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn get-menus
	[from to]
	{:pre [(q-valid? :shop/date from) (q-valid? :shop/date to)]
	 :post [(p-trace "get-menus" %) (q-valid? :shop/x-menus %)]}
	(log/trace "get-menus: (mc/find-maps shopdb menus {:date {$gte " from " $lt " to "}})")
	(let [db-menus* (mc/find-maps shopdb menus {:date {$gte from $lt to}})
		  db-menus  (map fix-date db-menus*)
		  new-menus (set/difference (set (utils/time-range from to (t/days 1)))
		  	                        (set (map :date db-menus)))]
		(sort-by :date (concat db-menus (map (fn [dt] {:date dt}) new-menus)))))

(defn get-items
	[]
	{:post [(p-trace "get-items" %) (q-valid? :shop/items %)]}
	(log/trace "get-items: (mc/find-maps shopdb items {})")
	(mc/find-maps shopdb items {}))

(defn get-item
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(p-trace "get-item" %) (q-valid? :shop/item %)]}
	(log/trace "get-item: (mc/find-maps shopdb items {:_id " id "})")
	(mc/find-one-as-map shopdb items {:_id id}))

(defn add-tag
	[tag-name]
	{:pre [(q-valid? :shop/string tag-name)]
	 :post [(p-trace "add-tag" %) (q-valid? :shop/tag %)]}
	(log/trace "add-tag: (get-tags)")
	(let [db-tags      (get-tags)
		  db-tag-names (->> db-tags (map :entryname) set)
		  clean-tag    (->> tag-name str/trim str/capitalize)
		  new-tag      (merge {:entryname clean-tag} (mk-std-field))]
		(if (some #{clean-tag} db-tag-names)
			(some #(when (= (:entryname %) clean-tag) %) db-tags)
			(do
				(log/trace "add-tag: (mc/insert shopdb tags " new-tag ")")
				(mc/insert shopdb tags new-tag)
				new-tag))))

(defn add-tags
	[tags*]
	{:pre [(q-valid? :shop/tags tags*)]
	 :post [(p-trace "add-tags" %)]}
	(log/trace "add-tags: (get-tags)")
	(let [db-tag-names    (->> (get-tags) (map :entryname) set)
		  clean-tag-names (->> tags*
		  					   (map #(->> (:entryname %) str/trim str/capitalize))
		  					   set)
		  new-tag-names   (set/difference clean-tag-names db-tag-names)
		  new-tags        (mapv #(merge {:entryname %} (mk-std-field)) new-tag-names)]
		(when (seq new-tags)
			(if (q-valid? :shop/tags new-tags)
				(do
					(log/trace "add-tags: (mc/insert-batch shopdb tags " new-tags ")")
					(mc/insert-batch shopdb tags new-tags))
				(throw (Exception. "Invalid tags"))))))

(defn add-list
	[entry]
	{:pre [(q-valid? :shop/list* entry)]
	 :post [(p-trace "add-list" %) (q-valid? :shop/list %)]}
	(add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-list: (mc/insert shopdb lists " entry* ")")
		(mc/insert shopdb lists entry*)
		entry*))

(defn add-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]
	 :post [(p-trace "add-item" %) (q-valid? :shop/item %)]}
	(add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(add-item-usage nil (:_id entry*) :create 0)
		(log/trace "add-item: (mc/insert shopdb items " entry* ")")
		(mc/insert shopdb items entry*)
		entry*))

(defn update-item
	[entry]
	{:pre [(q-valid? :shop/item* entry)]
	 :post [(p-trace "update-item" %)]}
	(add-item-usage nil (:_id entry) :update 0)
	(log/trace "update-item: (mc/update-by-id shopdb items " (:_id entry) " " (select-keys entry [:entryname :unit :url :amount :price :tags]) ")")
	(mc/update-by-id shopdb items (:_id entry)
		{$set (select-keys entry [:entryname :unit :url :amount :price :tags])}))

(defn delete-item
	[item-id]
	{:pre [(q-valid? :shop/_id item-id)]
	 :post [(p-trace "delete-item" %)]}
	(add-item-usage nil item-id :delete 0)
	(log/trace "delete-item: (mc/remove-by-id shopdb items " item-id ")")
	(mc/remove-by-id shopdb items item-id))

(defn add-project
	[entry]
	{:pre [(q-valid? :shop/project* entry)]
	 :post [(p-trace "add-project" %) (q-valid? :shop/project %)]}
	(add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-project: (mc/insert shopdb projects " entry* ")")
		(mc/insert shopdb projects entry*)
		entry*))

(defn add-recipe
	[entry]
	{:pre [(q-valid? :shop/recipe* entry)]
	 :post [(p-trace "add-recipe" %) (q-valid? :shop/recipe %)]}
	(add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-recipe: (mc/insert shopdb recipes " entry* ")")
		(mc/insert shopdb recipes entry*)
		entry*))

(defn add-menu
	[entry]
	{:pre [(q-valid? :shop/menu* entry)]
	 :post [(p-trace "add-menu" %) (q-valid? :shop/menu %)]}
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-menu: (mc/insert shopdb menus " entry* ")")
		(mc/insert shopdb menus entry*)
		entry*))

(defn add-recipe-to-menu
	[menu-dt recipe-id]
	{:pre [(q-valid? :shop/date menu-dt) (q-valid? :shop/_id recipe-id)]
	 :post [(p-trace "add-recipe-to-menu" %)]}
	(log/trace "add-recipe-to-menu: (get-recipe " recipe-id ")")
	(let [recipe (get-recipe recipe-id)]
		(log/trace "add-recipe-to-menu: (mc/update shopdb menus {:date " menu-dt "} {$set {:recipe " (select-keys recipe [:_id :entryname]) "}})")
		(mc/update shopdb menus {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}})))

(defn remove-recipe-from-menu
	[menu-dt]
	{:pre [(q-valid? :shop/date menu-dt)]
	 :post [(p-trace "remove-recipe-from-menu" %)]}
	(log/trace "remove-recipe-from-menu: (mc/update shopdb menus {:date menu-dt} {$unset :recipe})")
	(mc/update shopdb menus {:date menu-dt} {$unset {:recipe nil}}))

;;-----------------------------------------------------------------------------

(defn finish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]
	 :post [(p-trace "finish-list-item" %)]}
	(add-item-usage list-id item-id :finish 0)
	(log/trace "finish-list-item: (mc/update shopdb lists {:_id " list-id " :items._id " item-id "} {$set {:items.$.finished " (l/local-now) "}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished (l/local-now)}}))

(defn unfinish-list-item
	[list-id item-id]
	{:pre [(q-valid? :shop/_id list-id) (q-valid? :shop/_id item-id)]
	 :post [(p-trace "unfinish-list-item" %)]}
	(add-item-usage list-id item-id :unfinish 0)
	(log/trace "unfinish-list-item: (mc/update shopdb lists {:_id " list-id " :items {:_id " item-id" }} {$set {:finished nil}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$set {:items.$.finished nil}}))

(defn- remove-item
	[list-id item-id]
	(add-item-usage list-id item-id :remove 0)
	(log/trace "remove-item: (mc/update shopdb lists {:_id " list-id "} {$pull {:items {:_id " item-id "}}})")
	(mc/update shopdb lists
		{:_id list-id}
		{$pull {:items {:_id item-id}}}))

(defn- mod-item
	[list-id item-id num-of]
	(add-item-usage list-id item-id :mod num-of)
	(log/trace "mod-item: (mc/update shopdb lists {:_id " list-id " :items._id " item-id "} {$inc {:items.$.numof " num-of "}})")
	(mc/update shopdb lists
		{:_id list-id :items._id item-id}
		{$inc {:items.$.numof num-of}}))

(defn item->list
	[list-id item-id num-of]
	{:pre [(q-valid? :shop/_id list-id)
		   (q-valid? :shop/_id item-id)
		   (q-valid? int? num-of)]
	 :post [(p-trace "item->list" %)]}
	(log/trace "item->list: (->> " list-id " get-list :items (some #(= (:_id %) " item-id ")))")
	(if (->> list-id get-list :items (some #(= (:_id %) item-id)))
		(if (zero? num-of)
			(remove-item list-id item-id)
			(mod-item list-id item-id num-of))
		(when (pos? num-of)
			(add-item-usage list-id item-id :add-to 0)
			(log/trace "item->list: (mc/update-by-id shopdb lists {$addToSet {:items (assoc (get-item " item-id ") :numof " num-of ")}})")
			(mc/update-by-id shopdb lists list-id
			{$addToSet {:items (assoc (get-item item-id) :numof num-of)}}))))

(defn update-recipe
	[recipe]
	{:pre [(q-valid? :shop/recipe* recipe)]
	 :post [(p-trace "update-recipe" %)]}
	(log/trace "update-recipe: (mc/update-by-id shopdb recipes " (:_id recipe) " {$set " (select-keys recipe [:entryname :url :items :text]) "})")
	(mc/update-by-id shopdb recipes (:_id recipe)
		{$set (select-keys recipe [:entryname :url :items :text])})
	; now update the recipe in menus
	(log/trace "update-recipe: (mc/update shopdb menus {:recipe._id " (:_id recipe) "} {$set {:recipe " (select-keys recipe [:_id :entryname]) "}} {:multi true})")
	(mc/update shopdb menus {:recipe._id (:_id recipe)}
		{$set {:recipe (select-keys recipe [:_id :entryname])}}
		{:multi true}))

(defn finish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]
	 :post [(p-trace "finish-project" %)]}
	(log/trace "finish-project: (mc/update-by-id shopdb projects " project-id " {$set {:finished " (l/local-now) "}})")
	(mc/update-by-id shopdb projects project-id {$set {:finished (l/local-now)}}))

(defn unfinish-project
	[project-id]
	{:pre [(q-valid? :shop/_id project-id)]
	 :post [(p-trace "unfinish-project" %)]}
	(log/trace "unfinish-project: (mc/update-by-id shopdb projects " project-id " {$unset :finished})")
	(mc/update-by-id shopdb projects project-id {$set {:finished nil}}))

(defn update-menu
	[entry]
	{:pre [(q-valid? :shop/menu entry)]
	 :post [(p-trace "update-menu" %)]}
	(log/trace "update-menu: (mc/update-by-id shopdb menus " (:_id entry) " {$set " entry "})")
	(mc/update-by-id shopdb menus (:_id entry)
		{$set (select-keys entry [:entryname :date :tags :recipe])}))

(defn update-project
	[proj]
	{:pre [(q-valid? :shop/project* proj)]
	 :post [(p-trace "update-project" %)]}
	(log/trace "update-project: (mc/update-by-id shopdb projects " (:_id proj) " {$set " proj "})")
	(mc/update-by-id shopdb projects (:_id proj)
		{$set (select-keys proj [:entryname :priority :finished :tags])}))

(defn find-list-id
	[e-name]
	{:pre [(q-valid? :shop/string e-name)]
	 :post [(p-trace "find-list-id" %) (q-valid? :shop/_id %)]}
	(log/trace "find-list-id: (mc/find-one-as-map shopdb lists {:entryname " e-name "})")
	(get (mc/find-one-as-map shopdb lists {:entryname e-name}) :_id))

;;-----------------------------------------------------------------------------

(defn save-session-data
	[key data]
	(mc/insert shopdb sessions (assoc {:_id key} :data data)))

(defn read-session-data
	[key]
	(get (mc/find-one-as-map shopdb sessions {:_id key}) :data))

(defn delete-session-data
	[key]
	(mc/remove-by-id shopdb sessions key))

