(ns shop2.db.user
    (:require [clj-time.core :as t]
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
              [monger.operators :refer :all]
              [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.conformer :refer :all]
              [utils.core :as utils]))

;;-----------------------------------------------------------------------------

(defn- fix-user
    [user]
    (some-> user
        (update :roles #(->> % (map keyword) set))
        (update :created str)))

(defn get-user
    [uname]
    {:pre [(utils/valid? :user/username uname)]
     :post [(utils/valid? (s/nilable :shop/user) %)]}
    (some-> (mc-find-one-as-map "get-user" users
                 {:username {$regex (str "^" (str/trim uname) "$") $options "i"}})
            (fix-user)
            (conform-user)))

;;-----------------------------------------------------------------------------

(defn get-user-by-id
    [uid]
    {:pre [(utils/valid? :shop/_id uid)]
     :post [(utils/valid? (s/nilable :shop/user) %)]}
    (some-> (mc-find-map-by-id "get-user-by-id" users uid)
            (fix-user)
            (conform-user)))

;;-----------------------------------------------------------------------------

(defn get-users
    []
    {:post [(utils/valid? (s/* :shop/user) %)]}
    (some->> (mc-find-maps "get-user" users {})
             (map fix-user)
             (map conform-user)))

;;-----------------------------------------------------------------------------

(defn- count-chars
    [pw c-class]
    (if (nil? (re-find c-class pw))
        (throw+ (ex-info (str "PW must contain at least one of " c-class) {:cause :password :type :db}))
        pw))

(defn- verify-passwd
    [pw]
    (if (not= (str/trim pw) pw)
        (throw+ (ex-info "PW can't begin or end with space" {:cause :password :type :db}))
        (if (< (count pw) 8)
            (throw+ (ex-info "PW must be 8 chars or more" {:cause :password :type :db}))
            (-> pw
                (count-chars #"[a-zåäö]")
                (count-chars #"[A-ZÅÄÖ]")
                (count-chars #"[0-9]")
                (count-chars #"[.*!@#$%^&()=+-]")
                ))))

(defn create-user
    [username passwd roles]
    {:pre [(utils/valid? :shop/username username)
           (utils/valid? :shop/password passwd)
           (utils/valid? :shop/roles roles)]
     :post [(utils/valid? :shop/user-db %)]}
    (when (some? (get-user username))
        (throw+ (ex-info "duplicate username" {:cause :username :type :db})))
    (let [user (merge {:username (str/trim username)
                       ;:password (creds/hash-bcrypt (verify-passwd passwd))
                       :password (verify-passwd passwd)
                       :roles    roles} (mk-std-field))]
        (mc-insert "create-user" users user)
        user))

(defn delete-user
    [userid]
    (mc-remove-by-id "delete-user" users userid))

;;-----------------------------------------------------------------------------

(defn set-user-name
    [uid name]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/string name)]}
    (mc-update-by-id "set-user-name" users uid {$set {:username name}}))

(defn set-user-password
    [uid passwd]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/password passwd)]}
    (mc-update-by-id "set-user-password" users uid
                     {$set {:password passwd}}))
; {$set {:password (creds/hash-bcrypt (verify-passwd passwd))}}))

;;-----------------------------------------------------------------------------

(defn set-user-roles
    [uid roles]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? :shop/roles roles)]}
    (mc-update-by-id "set-user-roles" users uid {$set {:roles roles}}))

;;-----------------------------------------------------------------------------

(defn set-user-property
    [uid prop-key prop-val]
    {:pre [(utils/valid? :shop/_id uid)
           (utils/valid? keyword? prop-key)
           (utils/valid? map? prop-val)]}
    (let [props (:properties (get-user-by-id uid))]
        (mc-update-by-id "set-user-property" users uid
                         {$set {:properties (assoc props prop-key prop-val)}})))

;;-----------------------------------------------------------------------------

(defn update-user
    [user]
    {:pre [(utils/valid? :shop/user user)]}
    (mc-replace-by-id "update-user" users user))
