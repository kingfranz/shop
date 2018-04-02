(ns shop2.db.recipes
	(:require 	 [slingshot.slingshot :refer [throw+ try+]]
                 [cheshire.core :refer :all]
                 [taoensso.timbre :as log]
                  [clojure.spec.alpha :as s]
                  [orchestra.core :refer [defn-spec]]
                  [orchestra.spec.test :as st]
                  [monger.operators :refer :all]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.tags :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn-spec get-recipe-names (s/* (s/keys :req-un [:shop/_id :shop/entryname]))
	[]
	(mc-find-maps "get-recipe-names" "recipes" {} {:_id true :entryname true}))

(defn-spec get-recipes :shop/recipes
	[]
	(mc-find-maps "get-recipes" "recipes"))

(defn-spec get-recipe :shop/recipe
	[id :shop/_id]
	(mc-find-one-as-map "get-recipe" "recipes" {:_id id}))

(defn-spec add-recipe :shop/recipe
	[entry :shop/recipe]
	(mc-insert "add-recipe" "recipes" entry)
	entry)

(defn-spec update-recipe :shop/recipe
	[recipe :shop/recipe]
	(mc-replace-by-id "update-recipe" "recipes" recipe)
    ; now update the recipe in menus
	(mc-update "update-recipe" "menus" {:recipe._id (:_id recipe)}
               {$set {:recipe (select-keys recipe [:_id :entryname])}}
               {:multi true})
    recipe)

(st/instrument)
