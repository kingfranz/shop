(ns shop2.db.recipes
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
            				[spec       :as spec]
            				[db 		:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								;[menus 			:as dbmenus]
  								[projects 		:as dbprojects])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn get-recipe-names
	[]
	{:post [(p-trace "get-recipe-names" %)]}
	(log/trace "get-recipe-names: (mc/find-maps shopdb recipes)")
	(mc/find-maps shopdb recipes {} {:_id true :entryname true}))

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

(defn add-recipe
	[entry]
	{:pre [(q-valid? :shop/recipe* entry)]
	 :post [(p-trace "add-recipe" %) (q-valid? :shop/recipe %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-recipe: (mc/insert shopdb recipes " entry* ")")
		(mc/insert shopdb recipes entry*)
		entry*))

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

