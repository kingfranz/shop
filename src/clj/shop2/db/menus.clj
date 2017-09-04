(ns shop2.db.menus
	(:require 	(clj-time		[core     		:as t]
            					[local    		:as l]
            					[coerce   		:as c]
            					[format   		:as f]
            					[periodic 		:as p])
            	(clojure 		[set      		:as set]
            					[pprint   		:as pp]
            					[string   		:as str])
            	(clojure.spec 	[alpha          :as s])
             	(cheshire 		[core     		:refer :all])
            	(taoensso 		[timbre   		:as log])
            	(monger 		[core     		:as mg]
            					[credentials 	:as mcr]
            					[collection 	:as mc]
            					[joda-time  	:as jt]
            					[operators 		:refer :all])
            	(shop2 			[utils       	:refer :all]
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
	 :post [(q-valid? :shop/menu %)]}
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-menu" menus entry*)
		entry*))

(defn update-menu
	[entry]
	{:pre [(q-valid? :shop/menu entry)]}
	(mc-update-by-id "update-menu" menus (:_id entry)
		{$set (select-keys entry [:entryname :date :tags :recipe])}))

(defn add-recipe-to-menu
	[menu-dt recipe-id]
	{:pre [(q-valid? :shop/date menu-dt) (q-valid? :shop/_id recipe-id)]}
	(let [recipe (dbrecipes/get-recipe recipe-id)]
		(mc-update "add-recipe-to-menu" menus {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}})))

(defn remove-recipe-from-menu
	[menu-dt]
	{:pre [(q-valid? :shop/date menu-dt)]}
	(mc-update "remove-recipe-from-menu" menus {:date menu-dt} {$unset {:recipe nil}}))

(defn get-menus
	[from to]
	{:pre [(q-valid? :shop/date from) (q-valid? :shop/date to)]
	 :post [(q-valid? :shop/x-menus %)]}
	(let [db-menus* (mc-find-maps "get-menus" menus {:date {$gte from $lt to}})
		  db-menus  (map fix-date db-menus*)
		  new-menus (set/difference (set (time-range from to (t/days 1)))
		  	                        (set (map :date db-menus)))]
		(sort-by :date (concat db-menus (map (fn [dt] {:date dt}) new-menus)))))

