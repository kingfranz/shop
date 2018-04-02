(ns shop2.db.menus
	(:require 	 [clj-time.core :as t]
                 [clj-time.coerce :as c]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clojure.spec.alpha :as s]
                  [orchestra.core :refer [defn-spec]]
                  [orchestra.spec.test :as st]
                  [clojure.set :as set]
                  [monger.operators :refer :all]
                  [monger.joda-time :refer :all]
                  [cheshire.core :refer :all]
                 [taoensso.timbre :as log]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.recipes :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn-spec add-menu :shop/menu
	[entry :shop/menu]
	(mc-insert "add-menu" "menus" entry)
	entry)

(defn-spec update-menu any?
	[entry :shop/menu]
	(mc-replace-by-id "update-menu" "menus" entry))

(defn-spec add-recipe-to-menu any?
	[menu-dt :shop/date, recipe-id :shop/_id]
	(let [recipe (get-recipe recipe-id)]
		(mc-update "add-recipe-to-menu" "menus" {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}} {:upsert true})))

(defn-spec remove-recipe-from-menu any?
	[menu-dt :shop/date]
	(mc-update "remove-recipe-from-menu" "menus" {:date menu-dt} {$unset {:recipe nil}}))

(defn-spec get-menus :shop/x-menus
    [from :shop/date, to :shop/date]
    (let [db-menus (->> (mc-find-maps "get-menus" "menus" {:date {$gte from $lt to}})
                        (map fix-date))
          new-menus (set/difference (set (time-range from to (t/days 1)))
                                    (set (map :date db-menus)))]
        (sort-by :date (concat db-menus (map (fn [dt] {:date dt}) new-menus)))))

(defn-spec get-db-menus :shop/menus
    []
    (->> (mc-find-maps "get-db-menus" "menus")
         (map fix-date)))

(st/instrument)
