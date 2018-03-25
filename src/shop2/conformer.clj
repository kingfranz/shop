(ns shop2.conformer
    (:require [clj-time.core :as t]
              [clj-time.local :as l]
              [clj-time.coerce :as c]
              [clj-time.format :as f]
              [clj-time.periodic :as p]
              [clojure.spec.alpha :as s]
              [shop2.db :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.string :as str]
              [clojure.set :as set]
              [clojure.pprint :refer [pprint]]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn ensure-keys
    [m ks-d]
    (try+
        (into {} (map (fn [[k d]] [k (try+ (get-in m [k] d) (catch Throwable e (println "ensure:" m k d) (throw+ e)))]) ks-d))
        (catch Throwable e (println "ensure2:" m ks-d) (throw+ e))))

(defn- validate
    [obj spec]
    ;(println spec)
    ;(println "## VALIDATE:" (with-out-str (pprint obj)))
    (if (s/valid? spec obj)
        obj
        (do
            (println "Validate -----------------------------")
            (println (with-out-str (pprint obj)))
            (println "--------------------------------------")
            (println spec)
            (println "--------------------------------------")
            (println (s/explain-str spec obj))
            (println "--------------------------------------")
            (throw+ {:msg "Conform error" :type :conform-error :spec spec :obj obj}))))

;;-----------------------------------------------------------------------------
;(s/def :shop/tag*  (s/keys :req-un [:shop/entryname]
;                           :opt-un [:shop/parent]))
;
;(s/def :shop/tag   (s/keys :req-un [:shop/_id :shop/created
;                                    :shop/entryname :shop/entrynamelc
;                                    :shop/parent]))

(defn conform-tag
    [tag]
    ;(println "## conform-tag:" (with-out-str (pprint tag)))
    (try+
        (if (s/valid? :shop/tag tag)
            tag
            (-> tag
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:parent nil]])
                (assoc :entrynamelc (-> tag :entryname mk-enlc))
                (validate :shop/tag)))
        (catch Throwable e (println "## conform-tag:" (with-out-str (pprint tag))) (throw+ e))))

;;-----------------------------------------------------------------------------
;(s/def :shop/item*   (s/keys :req-un [:shop/entryname]
;                             :opt-un [:shop/tags
;                                      :shop/finished
;                                      :shop/numof :shop/url :shop/price
;                                      :shop/project
;                                      :shop/parent]))
;
;(s/def :shop/item    (s/keys :req-un [:shop/_id :shop/created
;                                      :shop/entryname :shop/entrynamelc
;                                      :item/tag
;                                      :shop/url :item/price
;                                      :shop/project
;                                      :shop/parent
;                                      :item/oneshot]))

(defn conform-item
    [item]
    ;(println "## conform-item:" (with-out-str (pprint item)))
    (try+
        (if (s/valid? :shop/item item)
            item
            (-> item
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:url ""] [:price nil] [:project nil] [:parent nil]])
                (assoc :entrynamelc (-> item :entryname mk-enlc))
                (assoc :oneshot false)
                (assoc :tag (some-> item :tags first conform-tag))
                (validate :shop/item)))
        (catch Throwable e (println "## conform-item:" (with-out-str (pprint item))) (throw+ e))))


;;-----------------------------------------------------------------------------
;(s/def :shop/list*   (s/keys :req-un [:shop/_id :shop/created :shop/entryname]
;                             :opt-un [:shop/items :list/parent :list/last]))
;
;(s/def :shop/list  (s/keys :req-un [:shop/_id :shop/created
;                                    :shop/entryname :shop/entrynamelc
;                                    :list/items :list/parent :list/last]))
;(s/def :list/item  (s/merge :shop/item :list/numof :shop/finished))

; :shop/_id :shop/created :shop/entryname :shop/entrynamelc :item/tag :shop/url
; :item/price :item/project :shop/parent :item/oneshot :list/numof :shop/finished

(defn- conform-list-item
    [item]
    ;(println "## conform-list-item:" (with-out-str (pprint item)))
    (try+
        (if (s/valid? :list/item item)
            item
            (-> item
                (utils/spy)
                (conform-item)
                (assoc :numof (get item :numof))
                (assoc :finished (:finished item))
                (validate :list/item)))
        (catch Throwable e (println "## conform-list-item:" (with-out-str (pprint item))) (throw+ e))))

(defn conform-list
    [lst]
    ;(println "## conform-list:" (with-out-str (pprint lst)))
    (try+
        (if (s/valid? :shop/list lst)
            lst
            (-> lst
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:parent nil]])
                (assoc :entrynamelc (-> lst :entryname mk-enlc))
                (assoc :last (or (:last lst) false))
                (assoc :items (mapv conform-list-item (:items lst)))
                (validate :shop/list)))
        (catch Throwable e (println "## conform-list:" (with-out-str (pprint lst))) (throw+ e))))

;;-----------------------------------------------------------------------------
;(s/def :shop/menu*     (s/keys :req-un [:shop/entryname :shop/date]
;                               :opt-un [:menu/recipe]))
;
;(s/def :shop/menu      (s/keys :req-un [:shop/_id :shop/created
;                                        :shop/entryname :shop/entrynamelc
;                                        :shop/date :menu/recipe]))

(defn conform-menu
    [menu]
    (try+
        (if (s/valid? :shop/menu menu)
            menu
            (-> menu
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:date nil] [:recipe nil]])
                (assoc :entrynamelc (-> menu :entryname mk-enlc))
                (validate :shop/menu)))
        (catch Throwable e (println "## conform-menu:" (with-out-str (pprint menu))) (throw+ e))))

;;-----------------------------------------------------------------------------
;(s/def :shop/recipe*  (s/keys :req-un [:shop/entryname]
;                              :opt-un [:recipe/items :shop/url :shop/text]))
;
;(s/def :shop/recipe   (s/keys :req-un [:shop/_id :shop/created
;                                       :shop/entryname :shop/entrynamelc
;                                       :recipe/items :shop/url :recipe/text]))

(defn- fix-amount
    [item]
    (update item :amount #(if % (str %) "")))

(defn conform-recipe
    [recipe]
    ;(println "## conform-recipe:" (with-out-str (pprint recipe)))
    (try+
        (if (s/valid? :shop/recipe recipe)
            recipe
            (-> recipe
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:url ""] [:text ""]])
                (assoc :entrynamelc (-> recipe :entryname mk-enlc))
                (assoc :items (mapv #(fix-amount (ensure-keys % [[:text ""] [:unit ""]])) (:items recipe)))
                (validate :shop/recipe)))
        (catch Throwable e (println "## conform-recipe:" (with-out-str (pprint recipe))) (throw+ e))))

;;-----------------------------------------------------------------------------
;(s/def :shop/project*   (s/keys :req-un [:shop/entryname :shop/priority]
;                                :opt-un [:shop/finished :shop/tags :shop/cleared]))
;
;(s/def :shop/project*   (s/keys :req-un [:shop/_id :shop/created
;                                         :shop/entryname :shop/entrynamelc
;                                         :project/priority :shop/finished :project/tag :project/cleared]))

(defn conform-project
    [project]
    ;(println "## conform-project:" (with-out-str (pprint project)))
    (try+
        (if (s/valid? :shop/project project)
            project
            (do
                (println "conform-project:" (s/explain-str :shop/project project))
                (-> project
                 (ensure-keys [[:_id nil] [:created (l/local-now)] [:entryname nil] [:priority 1] [:finished nil] [:cleared nil]])
                 (assoc :entrynamelc (-> project :entryname mk-enlc))
                 (assoc :tag (some-> project :tags first conform-tag))
                 (validate :shop/project))))
        (catch Throwable e (println "## conform-project:" (with-out-str (pprint project))) (throw+ e))))

;;-----------------------------------------------------------------------------
;(s/def :shop/user    (s/keys :req-un [:shop/_id :shopuser/created
;                                      :shop/username :shop/roles]
;                             :opt-un [:shopuser/properties]))
;
;(s/def :shop/user    (s/keys :req-un [:shop/_id :shop/created
;                                      :user/username :user/password
;                                      :user/roles :user/properties]))

(defn conform-user
    [user]
    ;(println "## conform-user:" (with-out-str (pprint user)))
    (try+
        (if (s/valid? :shop/user user)
            user
            (-> user
                (utils/spy)
                (ensure-keys [[:_id nil] [:created (l/local-now)] [:username nil] [:password nil] [:roles #{:user}]])
                (assoc :properties (or (:properties user) {}))
                (validate :shop/user)))
        (catch Throwable e (println "## conform-user:" (with-out-str (pprint user))) (throw+ e))))

