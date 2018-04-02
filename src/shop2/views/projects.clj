(ns shop2.views.projects
    (:refer-clojure :exclude [read-string])
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.db.user :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
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

(def pri-name "proj-pri-")
(def txt-name "proj-txt-")
(def parent-name "proj-par-")

(defn- mk-prio-dd
    [id prio]
    (hf/drop-down {:class "proj-pri-val"} (utils/mk-tag pri-name id) (range 1 6) prio))

(defn- mk-name-field
    [id ename]
    (hf/text-field {:class "proj-txt-val"} (utils/mk-tag txt-name id) ename))

(defn- proj-checkmark
    [id]
    [:a {:class "proj-check-val" :href (str "/user/project/finish/" id)} "&#10004"])

(defn- mk-project-row
    [id prio ename parent proj-dd check?]
    [:table.width-100p
     [:tr
      (when (nil? id)
          [:td.proj-pri-td (mk-prio-dd "new" prio)])
      [:td.proj-check-td
       (when (and id check?)
           (proj-checkmark id))]
      [:td.proj-txt-td
       (hf/text-field {:class (if check? "proj-txt-val" "proj-txt-val bold")}
                      (utils/mk-tag txt-name (if id id "new"))
                      ename)]
      [:td.width-150px.r-align
       (hf/drop-down {:style "width:150px;text-align:right;"}
                     (utils/mk-tag parent-name (if id id "new"))
                     proj-dd
                     parent)]]])

(defn- mk-proj-row
    [proj proj-dd rf]
    (let [parent [:li (rf (:_id proj) nil (:entryname proj) (:parent proj) proj-dd (empty? (:children proj)))]
          children (mapcat #(mk-proj-row % proj-dd rf) (:children proj))]
        (if (empty? children)
            (list parent)
            (concat (list parent) (list [:ul children])))))

(defn- mk-proj-prio-row
    [proj]
    [:tr
     [:td.proj-pri-td (mk-prio-dd (:_id proj) (:priority proj))]
     [:td.proj-check-td
      [:a {:class "proj-check-val" :href (str "/user/project/finish/" (:_id proj))} "&#10004"]]
     [:td.proj-txt-td (mk-name-field (:_id proj) (:entryname proj))]])

(defn- mk-finished-row
    [proj]
    [:tr
     [:td
      [:a.finished-proj
       {:href (str "/user/project/unfinish/" (:_id proj))}
       (str (:priority proj) " " (:entryname proj))]]])

(defn- finished?
    [p]
    (some? (:finished p)))

(defn- proj-head
    [proj]
    [:label.proj-head-val (:entryname proj)])

(defn- proj-tree-sort
    [p1 p2]
    (or (comp-nil (empty? (:children p1)) (empty? (:children p2)))
        (compare (:entrynamelc p1) (:entrynamelc p2))))

(defn- mk-proj-tree
    [target projects]
    (for [proj (->> projects (filter #(= (:parent %) target)) (sort-by :entrynamelc))]
        (assoc proj :children (mk-proj-tree (:_id proj) (remove #(= (:_id %) (:_id proj)) projects)))))

(defn- by-proj
    [projects proj-dd]
    (let [proj-tree (mk-proj-tree nil projects)]
        [:ul
         (for [proj proj-tree]
             (mk-proj-row proj proj-dd mk-project-row))]))

;;-----------------------------------------------------------------------------

(defn- mk-display
    [proj]
    (let [parent   [:li [:label.proj-txt-td (:entryname proj)]]
          children (mapcat mk-display (:children proj))]
        (if (empty? (:children proj))
            (list parent)
            (concat (list parent) (list [:ul children])))))

(defn display-projects
    []
    [:ul
      (for [proj (mk-proj-tree nil (get-active-projects))]
          (mk-display proj))])

;;-----------------------------------------------------------------------------

(defn- prio-head
    [pri]
    [:label.proj-head-val (str "Prioritet " pri)])

(defn- by-prio
    [projects]
    (let [by-pri (group-by :priority projects)]
        [:table
         (for [pri-key (sort (keys by-pri))]
             (list
                 [:tr
                  [:th.proj-head-th {:colspan 4} (prio-head pri-key)]]
                 (map mk-proj-prio-row (->> pri-key
                                       (get by-pri)
                                       ;(sort-by identity proj-comp)
                                       ))))]))

(defn want-by-proj?
    [req]
    (some->> req udata :properties :projects :group-type keyword (= :proj)))

(defn edit-projects
    [request]
    (let [active-projects (get-active-projects)
          finished-projects (get-finished-projects)]
        (common request "Projekt" [css-projects css-items css-misc]
             (hf/form-to
                 [:post "/user/project/edit"]
                 (ruaf/anti-forgery-field)
                 (hf/hidden-field :project-ids (->> active-projects (map :_id) pr-str))
                 (let [proj-dd (get-projects-dd)]
                     [:table.proj-tbl
                      [:tr
                       [:td
                        (home-button)
                        (if (want-by-proj? request)
                            [:a.link-flex {:href "/user/project/prio"} "Pri-Sort"]
                            [:a.link-flex {:href "/user/project/proj"} "Proj-Sort"])
                        (hf/submit-button {:class "button"} "Updatera!")]]
                      [:tr
                       [:td.items-block
                        (mk-project-row nil 1 "" nil proj-dd true)]]
                      [:tr
                       [:td
                        (if (want-by-proj? request)
                            (by-proj active-projects proj-dd)
                            (by-prio active-projects))]]
                      [:tr
                       [:td.items-block
                        [:table
                         [:tr
                          [:td
                           [:label.proj-head-val "Avklarade"]
                           [:a.link-flex {:href "/user/project/clear"} "Rensa"]]]
                         (map mk-finished-row finished-projects)]]]
                      ])))))

(defn set-group-type
    [request group-type]
    (when (or (= group-type :prio) (= group-type :proj))
        (set-user-property (uid request) :projects {:group-type group-type}))
    (ring/redirect "/user/project/edit"))

;;-----------------------------------------------------------------------------

(defn- param-val
    [p n k]
    (get p (utils/mk-tag n k)))

(defn- get-priority
    [params tag-key]
    (try+
        (->> (param-val params pri-name tag-key)
             (Integer/valueOf))
        (catch Throwable _ 5)))

(defn- get-parent
    [params tag-key]
    (try+
        (when-let [id (param-val params parent-name tag-key)]
            (if (or (= id no-id) (not (s/valid? :shop/_id id)))
                nil
                (when (project-id-exist? id)
                    id)))
        (catch Throwable _ nil)))

(defn- do-proj-update
    [params pkey]
    (try+
        (let [db-proj (get-project pkey)
              pname (param-val params txt-name pkey)
              prio (get-priority params pkey)
              parent (get-parent params pkey)]
            (when (or (and (s/valid? :shop/entryname pname)
                           (not= pname (:entryname db-proj)))
                      (not= prio (:priority db-proj))
                      (not= parent (:parent db-proj)))
                (update-project (-> db-proj
                                    (set-name pname)
                                    (assoc :priority prio
                                           :parent parent)))))
        (catch Throwable _)))

(defn- do-new-proj
    [params]
    (let [npname (get params (utils/mk-tag txt-name "new"))
          parent (get-parent params "new")
          prio (get-priority params "new")]
        (when (s/valid? :shop/entryname npname)
            (add-project (create-project-obj npname parent prio)))))

(defn edit-projects!
    [{params :params}]
    (doseq [pkey (edn/read-string (:project-ids params))]
        (do-proj-update params pkey))
    (do-new-proj params)
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
