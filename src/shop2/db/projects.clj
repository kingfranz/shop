(ns shop2.db.projects
	(:require 	[clj-time.core :as t]
                 [clj-time.local :as l]
                 [clj-time.coerce :as c]
                 [clj-time.format :as f]
                 [clj-time.periodic :as p]
                 [slingshot.slingshot :refer [throw+ try+]]
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
                 [hiccup.form :as hf]
                 [shop2.extra :refer :all]
                 [shop2.db :refer :all]
                 [shop2.db.tags :refer :all]
                 [shop2.conformer :refer :all]
                 [utils.core :as utils]
            ))

;;-----------------------------------------------------------------------------

(defn create-project-obj
    [pname tag priority]
    (-> (create-entity pname)
        (assoc :tag tag
               :priority priority
               :finished nil
               :cleared nil)))

(defn get-projects
    []
    {:post [(utils/valid? :shop/projects %)]}
    (map conform-project (mc-find-maps "get-projects" "projects" {:cleared nil})))

(defn get-db-projects
    []
    {:post [(utils/valid? :shop/projects %)]}
    (map conform-project (mc-find-maps "get-projects" "projects")))

(defn- proj-comp
    [p1 p2]
    (if (= (:priority p1) (:priority p2))
        (compare (:created p1) (:created p2))
        (compare (:priority p1) (:priority p2))))

(defn get-active-projects
	[]
	(->> (mc-find-maps "get-active-projects" "projects" {:finished nil})
         (map conform-project)
		 (sort-by identity proj-comp)))

(defn get-project
	[id]
	{:pre [(utils/valid? :shop/_id id)]
	 :post [(utils/valid? :shop/project %)]}
    (conform-project (mc-find-one-as-map "get-project" "projects" {:_id id})))

(defn get-project-dd
    []
    (->> (get-active-projects)
         (map (fn [l] [(:entryname l) (:_id l)]))
         (concat [["" no-id]])))

(defn mk-project-dd
    [current-id dd-name dd-class]
    (hf/drop-down {:class dd-class} dd-name (get-project-dd) current-id))

(defn add-project
	[entry]
	{:pre [(utils/valid? :shop/project entry)]
	 :post [(utils/valid? :shop/project %)]}
    (when (some? (:tag entry))
        (add-tag (-> entry :tag :entryname)))
	(mc-insert "add-project" "projects" entry)
	entry)

(defn finish-project
	[project-id]
	{:pre [(utils/valid? :shop/_id project-id)]}
	(mc-update-by-id "finish-project" "projects" project-id {$set {:finished (l/local-now)}}))

(defn unfinish-project
	[project-id]
	{:pre [(utils/valid? :shop/_id project-id)]}
	(mc-update-by-id "unfinish-project" "projects" project-id {$set {:finished nil}}))

(defn update-project
	[proj]
	{:pre [(utils/valid? :shop/project proj)]}
	(mc-replace-by-id "update-project" "projects" proj))

(defn clear-projects
	[]
	(mc-update clear-projects "projects"
		{:finished {$type "date"}}
		{$set {:cleared (l/local-now)}}
		{:multi true}))

