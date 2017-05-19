(ns shop2.controllers.routes
  	(:require (clojure 		[string   :as str])
              (ring.util 	[response :as ring])
              ;[ring.middleware.proxy-headers :as proxy]
              (compojure 	[core     :as cc])
              (shop2.views 	[home     :as home]
              				[recipe   :as recipe]
              				[items    :as items]
              				[menus    :as menus]
              				[tags     :as tags]
              				[lists    :as lists]
              				[projects :as projects])
              (shop2 		[db       :as db])))

(defn get-user
	[headers]
	;(db/get-user-data (get headers "x-forwarded-user")))
	(db/get-user-data "soren"))

(cc/defroutes routes
	(cc/GET "/" {:keys [headers params body] :as request}
	    (home/home-page (get-user headers)))
	(cc/GET "/home/:list-type" {:keys [headers params body] :as request}
	    (home/set-home-type (get-user headers) (:list-type params)))

	(cc/GET "/menu" []
	    (menus/show-menu-page))
	(cc/GET "/choose-recipe/:menu-date" [menu-date]
	    (menus/choose-recipe menu-date))
	(cc/GET "/add-recipe-to-menu/:recipe-id/:menu-date" [recipe-id menu-date]
	    (menus/add-recipe-to-menu recipe-id menu-date))
	(cc/GET "/remove-recipe/:menu-date" [menu-date]
	    (menus/remove-recipe-from-menu menu-date))
	(cc/POST "/update-menu" request
		(menus/update-menu! request))

	(cc/GET "/recipe/:id" [id]
	    (recipe/edit-recipe id))
	(cc/GET "/new-recipe" []
	    (recipe/new-recipe))
	(cc/POST "/update-recipe" request
		(recipe/update-recipe! request))
	(cc/POST "/create-recipe" request
		(recipe/create-recipe! request))

	(cc/GET "/projects" []
	    (projects/show-projects-page :by-prio))
	(cc/GET "/projects/:by" [by]
	    (projects/show-projects-page (keyword by)))
	(cc/GET "/finish-project/:id" [id]
	    (projects/finish-project id))
	(cc/GET "/unfinish-project/:id" [id]
	    (projects/unfinish-project id))
	(cc/POST "/update-projects" request
		(projects/update-projects! request))
	(cc/GET "/clear-projects" []
	    (projects/clear-projects))
	
	(cc/GET "/add-items/:id" [id]
	    (items/add-items-page id :a-z))
	(cc/GET "/add-items/:id/:stype" [id stype]
	    (items/add-items-page id (keyword stype)))
	(cc/GET "/add-to-list/:lid/:iid" [lid iid]
	    (items/add-item-page lid iid))
	(cc/GET "/mk-new-item/:listid" [listid]
	    (items/mk-new-item-page listid))
	(cc/POST "/new-item" request
	    (items/new-item! request))
	(cc/GET "/item/:id" [id]
	    (items/edit-item-page id))
	(cc/GET "/delete-item/:id" [id]
	    (items/delete-item id))
	(cc/POST "/update-item" request
	    (items/update-item request))

	(cc/GET "/pick-list" []
	    (home/pick-list))
	(cc/GET "/new-list" []
	    (lists/new-list-page))
	(cc/POST "/new-list" request
		(lists/added-list! request))
	(cc/GET "/delete-list/:listid" [listid]
	    (lists/delete-list! listid))
	(cc/GET "/edit-list/:listid" [listid]
	    (lists/edit-list-page listid))
	(cc/POST "/edit-list" request
		(lists/edit-list! request))
	(cc/GET "/list/:id" [id]
		(lists/show-list-page id))
	(cc/GET "/clean-list/:id" [id]
		(lists/clean-list id))
	(cc/GET "/list-up/:listid/:itemid" [listid itemid]
		(lists/list-up listid itemid))
	(cc/GET "/list-down/:listid/:itemid" [listid itemid]
		(lists/list-down listid itemid))
	(cc/GET "/item-done/:listid/:itemid" [listid itemid]
		(lists/item-done listid itemid))
	(cc/GET "/item-undo/:listid/:itemid" [listid itemid]
		(lists/item-undo listid itemid))

	(cc/GET "/tag/:id" [id]
	    (tags/edit-tag-page id))
	(cc/GET "/delete-tag/:id" [id]
	    (tags/delete-tag! id))
	(cc/GET "/delete-tag-all/:id" [id]
	    (tags/delete-tag-all! id))
	(cc/POST "/update-tag" request
		(tags/update-tag! request))
	)
