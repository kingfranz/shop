(ns shop2.db.projects
	(:require 	 [clj-time.local :as l]
                 [slingshot.slingshot :refer [throw+ try+]]
                 [clojure.spec.alpha :as s]
                  [orchestra.core :refer [defn-spec]]
                  [orchestra.spec.test :as st]
                  [cheshire.core :refer :all]
                 [taoensso.timbre :as log]
                  [monger.operators :refer :all]
                  [hiccup.form :as hf]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.tags :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn-spec create-project-obj :shop/project
    [pname :shop/entryname, parent :shop/parent, priority :project/priority]
    (-> (create-entity pname)
        (assoc :parent   parent
               :priority priority
               :finished nil
               :cleared  nil)))

(defn- upd-proj
    [old-proj]
    (if (s/valid? :shop/project old-proj)
        old-proj
        (-> old-proj
           (dissoc :tag)
           (assoc :parent nil))))

(defn-spec get-projects :shop/projects
           []
           (map upd-proj (mc-find-maps "get-projects" "projects" {:cleared nil})))

(defn-spec project-id-exist? boolean?
           [id :shop/_id]
           (some? (mc-find-one-as-map "get-project-id-exist?" "projects" {:_id id} {:_id true})))

(defn get-raw-projects
           []
           (mc-find-maps "get-raw-projects" "projects" {}))

(defn-spec proj-comp integer?
    [p1 :shop/project, p2 :shop/project]
    (or (comp-nil (some? (:finished p1)) (some? (:finished p2)))
        (comp-nil (:priority p1) (:priority p2))
        (comp-nil (:created p1) (:created p2))
        0))

(defn-spec get-active-projects :shop/projects
           []
           (->> (mc-find-maps "get-active-projects" "projects" {:finished nil})
                (map upd-proj)
                (sort-by identity proj-comp)))

(defn-spec get-finished-projects :shop/projects
           []
           (->> (mc-find-maps "get-finished-projects" "projects" {:finished {$ne nil} :cleared nil})
                (map upd-proj)
                (sort-by identity proj-comp)))

(defn-spec get-project (s/nilable :shop/project)
	[id :shop/_id]
    (upd-proj (mc-find-one-as-map "get-project" "projects" {:_id id})))

(defn-spec get-project-names (s/coll-of (s/keys :req-un [:shop/_id :shop/entryname]))
    []
    (->> (mc-find-maps "get-project-names" "projects" {:finished nil} {:_id true :entryname true})
        (sort-by :entryname)))

(defn-spec get-projects-dd (s/coll-of (s/cat :str string? :id :shop/_id))
    []
    (->> (get-project-names)
         (map (fn [l] [(:entryname l) (:_id l)]))
         (concat [["" no-id]])))

(defn-spec mk-project-dd any?
    [current-id (s/nilable :shop/_id), dd-name keyword?, dd-class string?]
    (hf/drop-down {:class dd-class} dd-name (get-projects-dd) current-id))

(defn-spec add-project :shop/project
	[entry :shop/project]
	(mc-insert "add-project" "projects" entry)
	entry)

(defn-spec finish-project any?
	[project-id :shop/_id]
	(mc-update-by-id "finish-project" "projects" project-id {$set {:finished (l/local-now)}}))

(defn-spec unfinish-project any?
	[project-id :shop/_id]
	(mc-update-by-id "unfinish-project" "projects" project-id {$set {:finished nil}}))

(defn-spec update-project any?
	[proj :shop/project]
	(mc-replace-by-id "update-project" "projects" proj))

(defn clear-projects
	[]
	(mc-update clear-projects "projects"
		{:finished {$type "date"}}
		{$set {:cleared (l/local-now)}}
		{:multi true}))

(st/instrument)
