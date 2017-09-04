(ns shop2.add-items
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]))


(def items [
        {:id 0 :entryname "milk" :tags "liquid"}
        {:id 1 :entryname "cream" :tags "liquid"}
        {:id 2 :entryname "toast" :tags "bakery"}
        {:id 3 :entryname "pork" :tags "meat"}
        {:id 4 :entryname "cheese" :tags "dairy"}])

(rf/reg-event-db              ;; sets up initial application state
  	:initialize                 ;; usage:  (dispatch [:initialize])
  	(fn [_ _]                   ;; the two parameters are not important here, so use _
    	{:items items
      	 :visible items
         :search-str ""}))    ;; so the application state will initially be a map with two keys

(defn mk-add-item
	[a-list item]
	[:tr
		[:td.item-txt-td
			[:a.item-txt-a
				{:href (str "/user/add-to-list/" (:_id a-list) "/" (:_id item))}
				[:div.item-txt (:entryname item)]]]
		[:td.item-tags-td
			(hf/label {:class "item-tags"}
				:x (some->> item :tags common/frmt-tags))]])

(defn old-items
	[a-list]
	[:div.item-div
		[:table.item-table
			(map #(mk-add-item a-list %) (->> @(rf/subscribe [:visible-items])
						 			  (sort-by #(str/lower-case (:entryname %))))))

(defn search-input
  	[]
  	[:div "Search: "
   		[:input {:type "text"
            	 :value @(rf/subscribe [:search-str])
            	 :on-change #(rf/dispatch [:search-change (-> % .-target .-value)])}]])  ;; <---

(defn add-items-ui
	[list-id]
	(let [a-list (dblists/get-list list-id)]
	    [:div
			(common/homeback-button (str "/user/list/" list-id))
    		[:a.link-flex {:href (str "/user/mk-new-item/" list-id)} "Ny"]]
        [:div
      		[search-input]
    		[old-items a-list]]))

(defn ^:export run
  	[list-id]
  	(rf/dispatch-sync [:initialize list-id])     ;; puts a value into application state
  	(reagent/render [add-items-ui]              ;; mount the application's ui into '<div id="app" />'
                    (js/document.getElementById "app")))

