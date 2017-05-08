(ns shop2.spec
  	(:require 	(clj-time 	[core     :as t]
            				[local    :as l]
            				[coerce   :as c]
            				[format   :as f]
            				[periodic :as p])
            	(clojure 	[string   :as str]
            				[spec     :as s]
            				[set      :as set])))

;;-----------------------------------------------------------------------------

(s/def :shop/string    (and string? seq))
(s/def :shop/strings   (s/* :shop/string))
(s/def :shop/date      #(instance? org.joda.time.DateTime %))
(s/def :shop/_id       :shop/string)
(s/def :shop/created   :shop/date)
(s/def :shop/entryname (and string? seq))
(s/def :shop/numof     (and int? pos?))
(s/def :shop/parent    (s/nilable :shop/_id))
(s/def :shop/amount    float?)
(s/def :shop/unit      (and string? seq))
(s/def :shop/price     float?)
(s/def :shop/text      (and string? seq))
(s/def :menu/recipe    (s/keys :req-un [:shop/_id :shop/entryname]))
(s/def :shop/priority  (s/int-in 1 6))
(s/def :shop/finished  (s/nilable :shop/date))
(s/def :shop/url       (and string? seq))

(s/def :shop/std-keys  (s/keys :req-un [:shop/_id :shop/created]))

;;-----------------------------------------------------------------------------

(s/def :shop/list*   (s/keys :req-un [:shop/entryname]
							 :opt-un [:shop/tags :shop/items :shop/parent]))

(s/def :shop/list    (s/and :shop/list* :shop/std-keys))
(s/def :shop/lists*  (s/* :shop/list*))
(s/def :shop/lists   (s/* :shop/list))

;;-----------------------------------------------------------------------------

(s/def :shop/item*   (s/keys :req-un [:shop/entryname]
							 :opt-un [:shop/tags :shop/finished :shop/numof :shop/url
							 		  :shop/amount :shop/unit :shop/price]))

(s/def :shop/item    (s/and :shop/item* :shop/std-keys))
(s/def :shop/items*  (s/* :shop/item*))
(s/def :shop/items   (s/* :shop/item))

;;-----------------------------------------------------------------------------

(s/def :shop/menu*     (s/keys :req-un [:shop/entryname :shop/date]
							   :opt-un [:shop/tags :menu/recipe]))

(s/def :shop/menu      (s/and :shop/menu* :shop/std-keys))
(s/def :shop/menus*    (s/* :shop/menu*))
(s/def :shop/menus     (s/* :shop/menu))
(s/def :shop/fill-menu (s/keys :req-un [:shop/date]))
(s/def :shop/x-menus   (s/+ (s/or :full :shop/menu :fill :shop/fill-menu)))

;;-----------------------------------------------------------------------------

(s/def :shop/project*   (s/keys :req-un [:shop/entryname :shop/priority]
							    :opt-un [:shop/finished :shop/tags]))

(s/def :shop/project    (s/and :shop/project* :shop/std-keys))
(s/def :shop/projects*  (s/* :shop/project*))
(s/def :shop/projects  (s/* :shop/project))

;;-----------------------------------------------------------------------------

(s/def :shop/recipe*  (s/keys :req-un [:shop/entryname]
							   :opt-un [:list/items :shop/url :shop/text]))

(s/def :shop/recipe   (s/and :shop/recipe* :shop/std-keys))
(s/def :shop/recipes* (s/* :shop/recipe*))
(s/def :shop/recipes  (s/* :shop/recipe))

;;-----------------------------------------------------------------------------

(s/def :shop/tag*  (s/keys :req-un [:shop/entryname]))

(s/def :shop/tag   (s/and :shop/tag* :shop/std-keys))
(s/def :shop/tags* (s/* :shop/tag*))
(s/def :shop/tags  (s/* :shop/tag))
