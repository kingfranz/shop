(ns shop2.views.projects
    (:refer-clojure :exclude [read-string])
    (:require [shop2.extra :refer :all]
              [mongolib.core :as db]
              [shopdb.misc :refer :all]
              [shopdb.user :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shopdb.items :refer :all]
              [shopdb.lists :refer :all]
              [shopdb.menus :refer :all]
              [shopdb.projects :refer :all]
              [shopdb.recipes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.string :as str]
              [clojure.edn :refer [read-string] :as edn]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [utils.core :as utils]
              [clj-time.format :as f]
              [clj-time.coerce :as c]
              [clj-time.core :as t]
              [clj-time.local :as l]))

;;-----------------------------------------------------------------------------

(def ^:private pri-name "proj-pri-")
(def ^:private txt-name "proj-txt-")
(def ^:private parent-name "proj-par-")
(def ^:private date-name "proj-date-")

;;-----------------------------------------------------------------------------

(defn- dt->long
    [dt]
    (if (nil? dt)
        1000000000
        (- (c/to-long dt) (c/to-long (l/local-now)))))

(defn- proj-date-sort
    [projs]
    (->> projs
         (maplist-sort [#(dt->long (:deadline %))
                        #(:priority %)
                        #(dt->long (:created %))
                        (fn [_] 0)])))

(defn proj-prio-sort
    [projs]
    (->> projs
         (maplist-sort [#(:priority %)
                        #(dt->long (:created %))
                        (fn [_] 0)])))

(defn- proj-sort-type
    [req]
    (some->> req udata :properties :projects :group-type keyword))

(defn- proj-sort-btn
    [req small?]
    (if small?
        (case (proj-sort-type req)
            :proj [:a.link-flex {:href "/user/project/prio/home"} "P"]
            :prio [:a.link-flex {:href "/user/project/date/home"} "D"]
            :date [:a.link-flex {:href "/user/project/proj/home"} "T"]
            [:a.link-flex {:href "/user/project/proj/home"} "T"])
        (case (proj-sort-type req)
            :proj [:a.link-flex {:href "/user/project/prio/proj"} "Prio Sort"]
            :prio [:a.link-flex {:href "/user/project/date/proj"} "Date Sort"]
            :date [:a.link-flex {:href "/user/project/proj/proj"} "Tree Sort"]
            [:a.link-flex {:href "/user/project/proj/proj"} "Tree Sort"])))

(defn small-proj-sort-btn
    [req]
    (proj-sort-btn req true))

(defn large-proj-sort-btn
    [req]
    (proj-sort-btn req false))

;;-----------------------------------------------------------------------------

(defn-spec ^:private mk-prio-dd any?
    [proj (s/nilable (s/keys :req-un [:shop/_id :shop/priority]))]
    (hf/drop-down {:class "fnt-18px"}
                  (utils/mk-tag pri-name (or (:_id proj) "new"))
                  (range 1 6)
                  (or (:priority proj) 1)))

(defn- dt-diff
    [dt]
    (if (t/before? dt (end-of-today))
        (utils/neg (t/in-hours (t/interval dt (end-of-today))))
        (t/in-hours (t/interval (end-of-today) dt))))

(defn- txt-style
    [proj]
    (cond
        (nil? proj)                             "base-txt"
        (nil? (:deadline proj))                 "base-txt"
        (neg? (dt-diff (:deadline proj)))       "red-txt"
        (< (dt-diff (:deadline proj)) 24)       "orange-txt"
        (< (dt-diff (:deadline proj)) (* 24 7)) "yellow-txt"
        :else                                   "base-txt"
        ))

(defn- proj-name-style
    [proj]
    (if (empty? (:children proj))
        (str (txt-style proj) " fnt-24px")
        (str (txt-style proj) " fnt-24px" " bold")))

(defn-spec ^:private mk-name-field any?
    [proj (s/nilable (s/keys :req-un [:shop/_id :shop/entryname]))]
    (hf/text-field {:class (str (proj-name-style proj) " width-100p")}
                   (utils/mk-tag txt-name (or (:_id proj) "new"))
                   (:entryname proj)))

(defn-spec ^:private mk-date-field any?
    [proj (s/nilable (s/keys :req-un [:shop/_id :project/deadline]))]
    (hf/text-field {:class "fnt-12px invert-txt width-100px"}
                   (utils/mk-tag date-name (or (:_id proj) "new"))
                   (dt->str (:deadline proj))))

(defn-spec ^:private mk-parent-field any?
    [proj (s/nilable (s/keys :req-un [:shop/_id :shop/parent])), proj-dd :shop/dd]
    (hf/drop-down {:class "fnt-12px width-100px"}
                  (utils/mk-tag parent-name (or (:_id proj) "new"))
                  proj-dd
                  (:parent proj)))

(defn-spec ^:private proj-checkmark any?
    [id :shop/_id]
    [:a.proj-check-val {:href (str "/user/project/finish/" id)} "&#10004"])

(defn-spec ^:private mk-project-row any?
    [proj (s/nilable :shop/project), proj-dd :shop/dd]
    [:table.width-100p
     [:tr
      (when (nil? proj)
          [:td.width-50px (mk-prio-dd nil)])
      [:td.width-20px
       (when (and proj (empty? (:children proj)))
           (proj-checkmark (:_id proj)))]
      [:td.width-400px (mk-name-field proj)]
      [:td.r-align.width-100px (mk-date-field proj)]
      [:td.r-align.width-100px (mk-parent-field proj proj-dd)]]])

(defn-spec ^:private mk-proj-row any?
    [proj :shop/project, proj-dd :shop/dd]
    (let [parent [:li (mk-project-row proj proj-dd)]
          children (mapcat #(mk-proj-row % proj-dd) (:children proj))]
        (if (empty? children)
            (list parent)
            (concat (list parent) (list [:ul children])))))

(defn-spec ^:private mk-proj-prio-row any?
    [proj :shop/project, proj-dd :shop/dd]
    [:tr
     [:td.width-50px
      (mk-prio-dd proj)]
     [:td.width-20px
      [:a.proj-check-val {:href (str "/user/project/finish/" (:_id proj))} "&#10004"]]
     [:td.width-100p (mk-name-field proj)]
     [:td.width-100px.r-align (mk-date-field proj)]
     [:td.width-100px.r-align (mk-parent-field proj proj-dd)]])

(defn-spec ^:private mk-finished-row any?
    [proj :shop/project]
    [:tr
     [:td
      [:a.finished-proj
       {:href (str "/user/project/unfinish/" (:_id proj))}
       (str (:priority proj) " " (:entryname proj))]]])

(defn-spec ^:private finished? boolean?
    [p :shop/project]
    (some? (:finished p)))

(defn-spec ^:private proj-head any?
    [proj :shop/project]
    [:label.proj-head-val (:entryname proj)])

(defn-spec ^:private mk-proj-tree any?
    [target :shop/parent, projects :shop/projects]
    (for [proj (->> projects (filter #(= (:parent %) target)) (sort-by :entrynamelc))]
        (assoc proj :children (mk-proj-tree (:_id proj) (remove #(= (:_id %) (:_id proj)) projects)))))

(defn-spec ^:private by-proj any?
    [projects :shop/projects proj-dd :shop/dd]
    (let [proj-tree (mk-proj-tree nil projects)]
        [:ul
         (for [proj proj-tree]
             (mk-proj-row proj proj-dd))]))

;;-----------------------------------------------------------------------------

(defn-spec ^:private mk-display-row any?
           [proj :shop/project]
           [:table.width-100p
            [:tr
             [:td.width-20px
              [:label (:priority proj)]]
             [:td
              [:label {:class (proj-name-style proj)} (:entryname proj)]]
             [:td.r-align.width-150px
              [:label.proj-txt-td (dt->str (:deadline proj))]]]])

(defn-spec ^:private mk-display any?
    [proj :shop/project]
    (let [parent   [:li [:label {:class (proj-name-style proj)} (:entryname proj)]]
          children (mapcat mk-display (:children proj))]
        (if (empty? (:children proj))
            (list parent)
            (concat (list parent) (list [:ul children])))))

(defn- display-by-proj
    []
    [:ul
     (for [proj (mk-proj-tree nil (get-active-projects))]
         (mk-display proj))])

(defn- display-by-prio
    []
    (->> (get-active-projects)
         (proj-prio-sort)
         (map mk-display-row)))

(defn- display-by-date
    []
    (->> (get-active-projects)
         (proj-date-sort)
         (map mk-display-row)))

(defn display-projects
    [req]
    (case (proj-sort-type req)
        :proj (display-by-proj)
        :prio (display-by-prio)
        :date (display-by-date)))

;;-----------------------------------------------------------------------------

(defn-spec ^:private prio-head any?
    [pri integer?]
    [:label.proj-head-val (str "Prioritet " pri)])

(defn-spec ^:private by-prio any?
    [projects :shop/projects, proj-dd :shop/dd]
    (let [by-pri (->> projects
                      (filter #(empty? (:children %)))
                      (group-by :priority))]
        [:table.width-100p
         (for [pri-key (sort (keys by-pri))]
             (list
                 [:tr
                  [:td.width-100p.border-1.border-r8.height-50px {:colspan 5} (prio-head pri-key)]]
                 (map #(mk-proj-prio-row % proj-dd) (get by-pri pri-key))))]))

;;-----------------------------------------------------------------------------

(defn-spec ^:private proj-date-row any?
           [proj :shop/project, proj-dd :shop/dd]
           [:tr
            [:td.width-50px
             (mk-prio-dd proj)]
            [:td.width-20px
             [:a.proj-check-val {:href (str "/user/project/finish/" (:_id proj))} "&#10004"]]
            [:td.width-100p (mk-name-field proj)]
            [:td.width-100px.r-align (mk-date-field proj)]
            [:td.width-100px.r-align (mk-parent-field proj proj-dd)]])

(defn-spec ^:private by-date any?
           [projects :shop/projects, proj-dd :shop/dd]
           [:table.width-100p
            (->> projects
                 (filter #(empty? (:children %)))
                 (proj-date-sort)
                 (map #(proj-date-row % proj-dd)))])

;;-----------------------------------------------------------------------------

(defn-spec ^:private show-projects any?
    [req map?, active-projects :shop/projects, proj-dd :shop/dd]
    (case (proj-sort-type req)
        :proj (by-proj active-projects proj-dd)
        :prio (by-prio active-projects proj-dd)
        :date (by-date active-projects proj-dd)))

;;-----------------------------------------------------------------------------

(defn-spec edit-projects any?
           [request map?]
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
                                  (large-proj-sort-btn request)
                                  (hf/submit-button {:class "button"} "Updatera!")]]
                                [:tr
                                 [:td (mk-project-row nil proj-dd)]]
                                [:tr
                                 [:td (show-projects request active-projects proj-dd)]]
                                [:tr
                                 [:td
                                  [:label.proj-head-val "Avklarade"]
                                  [:a.link-flex {:href "/user/project/clear"} "Rensa"]]]
                                (map mk-finished-row finished-projects)
                                ])))))

(defn-spec set-group-type any?
    [request map?, group-type string?, target string?]
    (when (and (contains? #{"prio" "proj" "date"} group-type)
               (contains? #{"proj" "home"} target))
        (set-user-property (uid request) :projects {:group-type (keyword group-type)})
        (if (= target "proj")
            (ring/redirect "/user/project/edit")
            (ring/redirect "/user/home"))))

;;-----------------------------------------------------------------------------

(defn-spec ^:private param-val (s/nilable string?)
    [p map?, n string?, k string?]
    (get p (utils/mk-tag n k)))

(defn-spec ^:private get-priority (s/int-in 1 6)
    [params map?, proj (s/nilable :shop/project)]
    (try+
        (->> (or (:_id proj) "new")
             (param-val params pri-name)
             (Integer/valueOf))
        (catch Throwable _ (or (:priority proj) 1))))

(defn- str->dt
    [s]
    (let [ldt (f/parse-local-date s)]
        (t/date-time (t/year ldt) (t/month ldt) (t/day ldt) 23 59 59)))

(defn-spec ^:private get-deadline :project/deadline
           [params map?, proj (s/nilable :shop/project)]
           (try+
               (->> (or (:_id proj) "new")
                    (param-val params date-name)
                    (str->dt))
               (catch Throwable _ (:deadline proj))))

(defn-spec ^:private get-parent (s/nilable :shop/parent)
    [params map?, proj (s/nilable :shop/project)]
    (try+
        (if-let [id (param-val params parent-name (or (:_id proj) "new"))]
            (if (or (= id no-id) (not (s/valid? :shop/_id id)))
                nil
                (when (project-id-exist? id)
                    id))
            (:parent proj))
        (catch Throwable _ (:parent proj))))

(defn-spec ^:private do-proj-update any?
    [params map?, proj-id :shop/_id]
    (let [db-proj  (get-project proj-id)
          pname    (param-val params txt-name proj-id)
          prio     (get-priority params db-proj)
          parent   (get-parent params db-proj)
          deadline (get-deadline params db-proj)]
        (when (or (and (s/valid? :shop/entryname pname)
                       (not= pname (:entryname db-proj)))
                  (not= prio (:priority db-proj))
                  (not= parent (:parent db-proj))
                  (not= deadline (:deadline db-proj)))
            (update-project (-> db-proj
                                (set-name pname)
                                (assoc :priority prio
                                       :parent   parent
                                       :deadline deadline))))))

(defn-spec ^:private do-new-proj any?
    [params map?]
    (let [npname   (get params (utils/mk-tag txt-name "new"))
          parent   (get-parent params nil)
          prio     (get-priority params nil)
          deadline (get-deadline params nil)]
        (when (s/valid? :shop/entryname npname)
            (add-project (create-project-obj npname parent prio deadline)))))

(defn edit-projects!
    [{params :params}]
    (doseq [proj-id (edn/read-string (:project-ids params))]
        (do-proj-update params proj-id))
    (do-new-proj params)
    (ring/redirect "/user/project/edit"))

;;-----------------------------------------------------------------------------

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

;;-----------------------------------------------------------------------------


(st/instrument)
