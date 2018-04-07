(ns shop2.views.notes
    (:require [shop2.extra :refer :all]
              [shop2.db :refer :all]
              [shop2.views.layout :refer :all]
              [shop2.views.common :refer :all]
              [shop2.views.css :refer :all]
              [shop2.db.notes :refer :all]
              [slingshot.slingshot :refer [throw+ try+]]
              [utils.core :as utils]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [hiccup.form :as hf]
              [ring.util.anti-forgery :as ruaf]
              [ring.util.response :as ring]
              [taoensso.timbre :as log]))

;;-----------------------------------------------------------------------------

(defn-spec ^:private show-note-page any?
           [request map?, note (s/nilable :shop/note)]
           (common request "Notering" [css-note]
                   (hf/form-to
                       [:post (if (nil? note) "/user/note/new" "/user/note/edit")]
                       (ruaf/anti-forgery-field)
                       (hf/hidden-field :note-id (:_id note))
                       [:div
                        (home-button)
                        (when note
                            [:a.link-flex {:href (str "/user/note/delete/" (:_id note))} "Ta bort!"])
                        (hf/submit-button {:class "button"}
                                          (if (nil? note) "Skapa" "Updatera!"))]
                       [:table
                        [:tr [:th "Namn"]]
                        [:tr
                         [:td.note-title-txt-td (hf/text-field {:class "note-title-txt"}
                                                              :note-name (:entryname note))]]
                        [:tr [:td.btn-spacer ""]]]
                       [:div.note-area-div (hf/text-area {:class "note-area"}
                                                        :note-area
                                                        (:text note))])))

;;-----------------------------------------------------------------------------

(defn-spec edit-note any?
           [request map?, note-id :shop/_id]
           (show-note-page request (get-note note-id)))

(defn-spec new-note any?
           [request map?]
           (show-note-page request nil))

;;-----------------------------------------------------------------------------

(defn edit-note!
    [{params :params}]
    (update-note (-> (get-note (:note-id params))
                     (set-name (:note-name params))
                     (assoc :text (or (:note-area params) ""))))
    (ring/redirect (str "/user/note/edit/" (:note-id params))))

(defn new-note!
    [{params :params}]
    (let [ret (add-note (create-note-obj (:note-name params) (:note-area params)))]
        (ring/redirect (str "/user/note/edit/" (:_id ret)))))

(defn delete-note!
    [_ id]
    (delete-note id)
    (ring/redirect "/user/home"))

;;-----------------------------------------------------------------------------

(st/instrument)
