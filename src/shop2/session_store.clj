(ns shop2.session-store
    (:require [utils.core :as utils]
              [slingshot.slingshot :refer [throw+ try+]]
              [environ.core :refer [env]]
              [ring.middleware.session.store :as store]
              [clojure.pprint :refer [pprint]]
              [mongolib.core :as db]
              ))

;;-----------------------------------------------------------------------------

(defonce key-store (atom {}))

(defn- read-store-data
    [key]
    ;(println "read-store-data:" key)
    (get @key-store key))

(defn- save-store-data
    [key data]
    ;(println "save-store-data:" key data)
    (swap! key-store assoc key data))

(defn- delete-store-data
    [key]
    ;(println "delete-store-data:" key)
    (swap! key-store dissoc key))

(deftype ShopStore []
    store/SessionStore
    (store/read-session [_ key]
        (read-store-data key))
    (store/write-session [_ key data]
        (let [key (or key (db/mk-id))]
            (save-store-data key data)
            key))
    (store/delete-session [_ key]
        (delete-store-data key)
        nil))

;;-----------------------------------------------------------------------------
