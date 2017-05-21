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
	(mc-find-maps "get-recipe-names" recipes {} {:_id true :entryname true}))

(defn get-recipes
	[]
	{:post [(q-valid? :shop/recipes %)]}
	(mc-find-maps "get-recipes" recipes))

(defn get-recipe
	[id]
	{:pre [(q-valid? :shop/_id id)]
	 :post [(q-valid? :shop/recipe %)]}
	(mc-find-one-as-map "get-recipe" recipes {:_id id}))

(defn add-recipe
	[entry]
	{:pre [(q-valid? :shop/recipe* entry)]
	 :post [(q-valid? :shop/recipe %)]}
	(dbtags/add-tags (:tags entry))
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-recipe" recipes entry*)
		entry*))

(defn update-recipe
	[recipe]
	{:pre [(q-valid? :shop/recipe* recipe)]}
	(mc-update-by-id "update-recipe" recipes (:_id recipe)
		{$set (select-keys recipe [:entryname :url :items :text])})
	; now update the recipe in menus
	(mc-update "update-recipe" menus {:recipe._id (:_id recipe)}
		{$set {:recipe (select-keys recipe [:_id :entryname])}}
		{:multi true}))

