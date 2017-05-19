(ns shop2.db.menus
	(:require 	(clj-time	[core     		:as t]
            				[local    		:as l]
            				[coerce   		:as c]
            				[format   		:as f]
            				[periodic 		:as p])
            	(clojure 	[set      		:as set]
            				[pprint   		:as pp]
            				[spec     		:as s]
            				[string   		:as str])
            	(cheshire 	[core     		:refer :all])
            	(taoensso 	[timbre   		:as log])
            	(monger 	[core     		:as mg]
            				[credentials 	:as mcr]
            				[collection 	:as mc]
            				[joda-time  	:as jt]
            				[operators 		:refer :all])
            	(shop2 		[utils       	:as utils]
            				[spec       	:as spec]
            				[db 			:refer :all])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn add-menu
	[entry]
	{:pre [(q-valid? :shop/menu* entry)]
	 :post [(p-trace "add-menu" %) (q-valid? :shop/menu %)]}
	(let [entry* (merge entry (mk-std-field))]
		(log/trace "add-menu: (mc/insert shopdb menus " entry* ")")
		(mc/insert shopdb menus entry*)
		entry*))

(defn update-menu
	[entry]
	{:pre [(q-valid? :shop/menu entry)]
	 :post [(p-trace "update-menu" %)]}
	(log/trace "update-menu: (mc/update-by-id shopdb menus " (:_id entry) " {$set " entry "})")
	(mc/update-by-id shopdb menus (:_id entry)
		{$set (select-keys entry [:entryname :date :tags :recipe])}))

(defn add-recipe-to-menu
	[menu-dt recipe-id]
	{:pre [(q-valid? :shop/date menu-dt) (q-valid? :shop/_id recipe-id)]
	 :post [(p-trace "add-recipe-to-menu" %)]}
	(log/trace "add-recipe-to-menu: (get-recipe " recipe-id ")")
	(let [recipe (dbrecipes/get-recipe recipe-id)]
		(log/trace "add-recipe-to-menu: (mc/update shopdb menus {:date " menu-dt "} {$set {:recipe " (select-keys recipe [:_id :entryname]) "}})")
		(mc/update shopdb menus {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}})))

(defn remove-recipe-from-menu
	[menu-dt]
	{:pre [(q-valid? :shop/date menu-dt)]
	 :post [(p-trace "remove-recipe-from-menu" %)]}
	(log/trace "remove-recipe-from-menu: (mc/update shopdb menus {:date menu-dt} {$unset :recipe})")
	(mc/update shopdb menus {:date menu-dt} {$unset {:recipe nil}}))

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

