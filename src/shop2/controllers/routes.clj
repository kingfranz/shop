(ns shop2.controllers.routes
  	(:require (clojure 		[string   :as str])
              (ring.util 	[response :as ring])
              (compojure 	[core     :as cc])
              (shop2.views 	[home     :as home]
              				[recipe   :as recipe]
              				[items    :as items]
              				[menus    :as menus]
              				[lists    :as lists]
              				[projects :as projects])
              (shop2 		[db       :as db])))

(cc/defroutes routes
	(cc/GET "/" []
	    (home/home-page))

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
	    (projects/show-projects-page))
	(cc/GET "/finish-project/:id" [id]
	    (projects/finish-project id))
	(cc/GET "/unfinish-project/:id" [id]
	    (projects/unfinish-project id))
	(cc/POST "/update-projects" request
		(projects/update-projects! request))

	(cc/GET "/add-items/:id" [id]
	    (items/add-items-page id))
	(cc/GET "/add-to-list/:lid/:iid" [lid iid]
	    (items/add-item-page lid iid))
	(cc/POST "/new-item" request
	    (items/new-item! request))
	(cc/GET "/item/:id" [id]
	    (items/edit-item-page id))
	(cc/GET "/delete-item/:id" [id]
	    (items/delete-item id))
	(cc/POST "/update-item" request
	    (items/update-item request))

	(cc/GET "/new-list" []
	    (lists/new-list-page))
	(cc/POST "/new-list" request
		(lists/added-list! request))
	(cc/GET "/list/:id" [id]
		(lists/show-list-page id))
	(cc/GET "/item-done/:listid/:itemid" [listid itemid]
		(lists/item-done listid itemid))
	(cc/GET "/item-undo/:listid/:itemid" [listid itemid]
		(lists/item-undo listid itemid))
	)
