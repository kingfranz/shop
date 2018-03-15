(ns shop2.db.menus
	(:require 	[clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [clojure.spec.alpha :as s]
                 [clojure.string :as str]
                 [clojure.set :as set]
                 [clojure.pprint :as pp]
                 [clojure.spec.alpha :as s]
                 [cheshire.core :refer :all]
                 [taoensso.timbre :as log]
                 [monger.core :as mg]
                 [monger.credentials :as mcr]
                 [monger.collection :as mc]
                 [monger.joda-time :as jt]
                 [monger.operators :refer :all]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.recipes :refer :all]
                 [utils.core :as utils]
            )
	(:import 	[java.util UUID])
	(:import 	[com.mongodb MongoOptions ServerAddress]))

;;-----------------------------------------------------------------------------

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn add-menu
	[entry]
	{:pre [(utils/valid? :shop/menu* entry)]
	 :post [(utils/valid? :shop/menu %)]}
	(let [entry* (merge entry (mk-std-field))]
		(mc-insert "add-menu" menus entry*)
		entry*))

(defn update-menu
	[entry]
	{:pre [(utils/valid? :shop/menu entry)]}
	(mc-update-by-id "update-menu" menus (:_id entry)
		{$set (select-keys entry [:entryname :date :tags :recipe])}))

(defn add-recipe-to-menu
	[menu-dt recipe-id]
	{:pre [(utils/valid? :shop/date menu-dt) (utils/valid? :shop/_id recipe-id)]}
	(let [recipe (get-recipe recipe-id)]
		(mc-update "add-recipe-to-menu" menus {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}})))

(defn remove-recipe-from-menu
	[menu-dt]
	{:pre [(utils/valid? :shop/date menu-dt)]}
	(mc-update "remove-recipe-from-menu" menus {:date menu-dt} {$unset {:recipe nil}}))

(defn get-menus
	[from to]
	{:pre [(utils/valid? :shop/date from) (utils/valid? :shop/date to)]
	 :post [(utils/valid? :shop/x-menus %)]}
	(let [db-menus* (mc-find-maps "get-menus" menus {:date {$gte from $lt to}})
		  db-menus  (map fix-date db-menus*)
		  new-menus (set/difference (set (time-range from to (t/days 1)))
		  	                        (set (map :date db-menus)))]
		(sort-by :date (concat db-menus (map (fn [dt] {:date dt}) new-menus)))))

