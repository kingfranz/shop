(ns shop2.views.projects
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.tags :refer :all]
              [shop2.db.items :refer :all]
              [shop2.db.lists :refer :all]
              [shop2.db.menus :refer :all]
              [shop2.db.projects :refer :all]
              [shop2.db.recipes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.string :as str]
              [clojure.edn :refer [read-string] :as edn]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [utils.core :as utils]
              [clojure.spec.alpha :as s]))

;;-----------------------------------------------------------------------------

(def num-new-proj 5)
(def pri-name "proj-pri-")
(def txt-name "proj-txt-")
(def tags-name "proj-tags-")

(defn- mk-proj-row
    [proj]
    (let [id (if (map? proj) (:_id proj) (str proj))]
        [:tr
         [:td.proj-pri-td (hf/drop-down {:class "proj-pri-val"}
                                        (utils/mk-tag pri-name id)
                                        (range 1 6) (:priority proj))]
         [:td.proj-check-td
          (when (map? proj)
              [:a {:class "proj-check-val"
                   :href  (str "/user/project/finish/" (:_id proj))} "&#10004"])]
         [:td.proj-txt-td (hf/text-field {:class "proj-txt-val"}
                                         (utils/mk-tag txt-name id)
                                         (:entryname proj))]
         [:td.proj-tags-td (hf/text-field {:class "proj-tags-val"}
                                          (utils/mk-tag tags-name id)
                                          (some-> proj :tag :entryname))]]))

(defn- mk-finished-row
    [proj]
    [:tr
     [:td
      [:a.finished-proj
       {:href (str "/user/project/unfinish/" (:_id proj))}
       (str (:priority proj) " "
            (:entryname proj) " "
            "[" (some-> proj :tag :entryname) "]")]]])

(defn- finished?
    [p]
    (some? (:finished p)))

(defn- proj-comp
    ([p] 0)
    ([p1 p2]
     (if (and (finished? p1) (not (finished? p2)))
         1
         (if (and (not (finished? p1)) (finished? p2))
             -1
             (if (< (:priority p1) (:priority p2))
                 -1
                 (if (> (:priority p1) (:priority p2))
                     1
                     (compare (:entryname p1) (:entryname p2))))))))

(defn- tags-head
    [stags]
    [:label.proj-head-val stags])

(defn- by-tags
    [projects]
    (let [by-tag (->> projects
                      (remove finished?)
                      (map #(assoc % :tag-txt (or (some-> % :tag :entryname) "Allmänt")))
                      (group-by :tag-txt))]
        [:table
         (for [tag-key (sort (keys by-tag))]
             (list
                 [:tr
                  [:th.proj-head-th {:colspan 4} (tags-head tag-key)]]
                 (map mk-proj-row (->> tag-key
                                       (get by-tag)
                                       (sort-by proj-comp)))))]))

(defn- prio-head
    [pri]
    [:label.proj-head-val (str "Prioritet " pri)])

(defn- by-prio
    [projects]
    (let [by-pri (group-by :priority (remove finished? projects))]
        [:table
         (for [pri-key (sort (keys by-pri))]
             (list
                 [:tr
                  [:th.proj-head-th {:colspan 4} (prio-head pri-key)]]
                 (map mk-proj-row (->> pri-key
                                       (get by-pri)
                                       (sort-by proj-comp)))))]))

(defn want-by-tag?
    [req]
    (some->> req udata :properties :projects :group-type keyword (= :by-tag)))

(defn edit-projects
    [request]
    (let [projects (get-projects)]
        (common request "Projekt" [css-projects]
                (hf/form-to
                    [:post "/user/project/edit"]
                    (ruaf/anti-forgery-field)
                    (hf/hidden-field :project-ids (->> projects (remove finished?) (map :_id) pr-str))
                    [:table.proj-tbl
                     [:tr
                      [:td
                       (home-button)
                       (if (want-by-tag? request)
                           [:a.link-flex {:href "/user/project/prio"} "Pri-Sort"]
                           [:a.link-flex {:href "/user/project/tag"} "Kat-Sort"])
                       (hf/submit-button {:class "button"} "Updatera!")]]
                     [:tr
                      [:td
                       (if (want-by-tag? request)
                           (by-tags projects)
                           (by-prio projects))]]
                     [:tr
                      [:td
                       [:table
                        [:tr
                         [:th.proj-head-th {:colspan 4}
                          [:label.proj-head-val "Nya projekt "]]]
                        (for [x (range num-new-proj)]
                            (mk-proj-row x))]]]
                     [:tr
                      [:td
                       [:table
                        [:tr
                         [:th.proj-head-th
                          [:label.proj-head-val "Avklarade"]]
                         [:a.link-flex {:href "/user/project/clear"} "Rensa"]
                         ]
                        (let [projs (sort-by proj-comp (filter finished? projects))]
                            (map mk-finished-row projs))]]]]
                    ))))

(defn set-group-type
    [request group-type]
    (set-user-property (uid request) :projects {:group-type group-type})
    (ring/redirect "/user/project/edit"))

;;-----------------------------------------------------------------------------

(defn- mk-proj-tag
    [params pkey]
    (let [param-value (get params (utils/mk-tag tags-name pkey))]
        (when (s/valid? :tags/entryname param-value)
            (add-tag param-value))))

(defn edit-projects!
    [{params :params}]
    (doseq [pkey (edn/read-string (:project-ids params))
            :let [f-name (get params (utils/mk-tag txt-name pkey))
                  f-tag (mk-proj-tag params pkey)]
            :when (and (seq f-name) (seq f-tag))]
        (update-project (-> (get-project pkey)
                            (set-name f-name)
                            (assoc :tag f-tag
                                   :priority (->> (utils/mk-tag pri-name pkey)
                                                  (get params)
                                                  (Integer/valueOf))))))
    (doseq [pkey (range num-new-proj)
            :let [f-name (get params (utils/mk-tag txt-name pkey))
                  f-tag (mk-proj-tag params pkey)]
            :when (and (seq f-name) (seq f-tag))]
        (add-project (create-project-obj f-name
                                         f-tag
                                         (->> (utils/mk-tag pri-name pkey)
                                              (get params)
                                              (Integer/valueOf)))))
    (ring/redirect "/user/project/edit"))

(defn unfinish-proj
    [_ id]
    (unfinish-project id)
    (ring/redirect "/user/project/edit"))

(defn finish-proj
    [_ id]
    (finish-project id)
    (ring/redirect "/user/project/edit"))

(defn clear-projs
    [_]
    (clear-projects)
    (ring/redirect "/user/project/edit"))
