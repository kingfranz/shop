(ns shop2.rest.items
  	(:require 	(shop2 			[db         	:as db]
  								[utils      	:as utils])
            	(shop2.db 		[tags 			:as dbtags]
  								[items			:as dbitems]
  								[lists 			:as dblists]
  								[menus 			:as dbmenus]
  								[projects 		:as dbprojects]
  								[recipes 		:as dbrecipes])
            	(hiccup			[core       	:as h]
            					[def        	:as hd]
            					[element    	:as he]
            					[form       	:as hf]
            					[page       	:as hp]
            					[util       	:as hu])
            	(ring.util 		[response   	:as ring]
              					[anti-forgery 	:as ruaf])
            	(clojure.spec 	[alpha          :as s])
             	(clojure 		[string       	:as str]
            					[set          	:as set])))

;;-----------------------------------------------------------------------------

(defn get-parents
	[a-list]
	(loop [parent (:parent a-list)
		   acc    #{(:_id a-list)}]
		(if (nil? parent)
			acc
			(recur (:parent parent) (conj acc (:_id parent))))))

(defn get-items
	[a-list]
	(let [items        (dbitems/get-items)
		  id-parents   (get-parents a-list)
    	  active-items (->> a-list
                            :items
                            (remove #(some? (:finished %)))
                            (map :_id)
                            set)]
		(->> items
       		 (filter #(contains? id-parents (:parent %)))
          	 (remove #(contains? active-items (:_id %))))))

(defn rest-items
  	[list-id]
   	{:body {:body "<p>body text</p>"
	        :items (str (get-items (dblists/get-list list-id)))}
	 :headers {"Cache-Control" "no-transform,public,max-age=10"}})

