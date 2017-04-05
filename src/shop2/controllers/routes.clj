(ns shop2.controllers.routes
  	(:require [clojure.string     :as str]
              [ring.util.response :as ring]
              [ring.middleware.anti-forgery :as rmaf]
              [compojure.core     :as cc]
              [shop2.views.home   :as home]
              [shop2.views.recipe :as recipe]
              [shop2.views.items  :as items]
              [shop2.views.menus  :as menus]
              [shop2.views.lists  :as lists]
              [shop2.views.projects :as projects]
              [shop2.db           :as db]))

(cc/defroutes routes
	(db/load-db)
	(cc/GET "/" []
	    (home/home-page))

	(cc/GET "/menu" []
	    (menus/show-menu-page))
	(cc/GET "/choose-recipe/:menu-date" [menu-date]
	    (menus/choose-recipe menu-date))
	(cc/GET "/add-recipe-to-menu/:recipe-id/:menu-date" [recipe-id menu-date]
	    (menus/add-recipe-to-menu recipe-id menu-date))
	(cc/POST "/update-menu" request
		(menus/update-menu! request)
		(ring/redirect "/menu"))

	(cc/GET "/recipe/:id" [id]
	    (recipe/edit-recipe id))
	(cc/GET "/new-recipe" []
	    (recipe/new-recipe))
	(cc/POST "/update-recipe" request
		(ring/redirect (str "/menu/" (recipe/update-recipe! request))))

	(cc/GET "/projects" []
	    (projects/show-projects-page))
	(cc/GET "/finish-project/:id" [id]
	    (projects/finish-project id)
		(ring/redirect "/projects"))
	(cc/GET "/unfinish-project/:id" [id]
	    (projects/unfinish-project id)
		(ring/redirect "/projects"))
	(cc/POST "/update-projects" request
		(projects/update-projects! request)
		(ring/redirect "/projects"))

	(cc/GET "/add-items/:id" [id]
	    (items/add-items-page (db/get-list id)))
	(cc/GET "/add-to-list/:lid/:iid" [lid iid]
	    (items/add-item-page lid iid))
	(cc/POST "/new-item" request
		(items/new-item! request))

	(cc/GET "/new-list" []
	    (lists/new-list-page))
	(cc/POST "/new-list" request
		(lists/added-list! request)
		(ring/redirect "/new-list"))
	(cc/GET "/list/:id" [id]
		(lists/show-list-page id)))
