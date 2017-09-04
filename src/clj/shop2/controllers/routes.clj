(ns shop2.controllers.routes
  	(:require 	(clojure 		 	[string   		:as str]
  								 	[pprint   		:as pp])
            	(ring.util 		 	[response 		:as ring])
            	(compojure 		 	[core     		:as cc]
              					 	[route    		:as route])
            	(cemerick 		 	[friend     	:as friend])
            	(cemerick.friend 	[workflows 		:as workflows]
                             	 	[credentials	:as creds])
            	(shop2.views 	 	[home     		:as home]
              					 	[recipe   		:as recipe]
              					 	[items    		:as items]
              					 	[menus    		:as menus]
              					 	[admin         	:as admin]
              					 	[tags     		:as tags]
              					 	[lists    		:as lists]
              					 	[projects 		:as projects])
            	(shop2.views.admin	[items         	:as a-items]
            					   	[users         	:as a-users]
            					   	[lists         	:as a-lists])
            	(shop2 			 	[db       		:as db])))

;;-----------------------------------------------------------------------------

(cc/defroutes user-routes
	(cc/GET "/home" request
	    (home/home-page request))

	(cc/GET "/home/:list-type" request
		(home/set-home-type request (-> request :params :list-type)))

	(cc/GET "/menu" request
	    (menus/show-menu-page request))

	(cc/GET "/choose-recipe/:menu-date" request
	    (menus/choose-recipe request (-> request :params :menu-date)))

	(cc/GET "/add-recipe-to-menu/:recipe-id/:menu-date" request
	    (menus/add-recipe-to-menu request
	    						  (-> request :params :recipe-id)
	    						  (-> request :params :menu-date)))

	(cc/GET "/remove-recipe/:menu-date" request
	    (menus/remove-recipe-from-menu request (-> request :params :menu-date)))

	(cc/POST "/update-menu" request
	    (menus/update-menu! request))

	(cc/GET "/recipe/:id" request
	    (recipe/edit-recipe request (-> request :params :id)))

	(cc/GET "/new-recipe" request
	    (recipe/new-recipe request))

	(cc/POST "/update-recipe" request
	    (recipe/update-recipe! request))

	(cc/POST "/create-recipe" request
	    (recipe/create-recipe! request))

	(cc/GET "/projects" request
	    (projects/show-projects-page request :by-prio))

	(cc/GET "/projects/:by" request
	    (projects/show-projects-page request (-> request :params :by keyword)))

	(cc/GET "/finish-project/:id" request
	    (projects/finish-project request (-> request :params :id)))

	(cc/GET "/unfinish-project/:id" request
	    (projects/unfinish-project request (-> request :params :id)))

	(cc/POST "/update-projects" request
	    (projects/update-projects! request))

	(cc/GET "/clear-projects" request
	    (projects/clear-projects request))
	
	(cc/GET "/add-items/:id" request
	    (items/add-items-page request (-> request :params :id)))

	(cc/GET "/add-items/:id/:stype" request
	    (items/add-items-page request (-> request :params :id)))

	(cc/GET "/add-to-list/:lid/:iid" request
	    (items/add-item-page request
							  (-> request :params :lid)
							  (-> request :params :iid)))

	(cc/GET "/mk-new-item/:listid" request
	    (items/mk-new-item-page request (-> request :params :listid)))

	(cc/GET "/mk-new-item" request
	    (items/mk-new-item-page request nil))

	(cc/POST "/new-item" request
	    (items/new-item! request))

	(cc/POST "/update-item" request
	    (a-items/update-item request))

	(cc/GET "/list/:id" request
	    (lists/show-list-page request (-> request :params :id)))

	(cc/GET "/clean-list/:id" request
	    (lists/clean-list request (-> request :params :id)))

	(cc/GET "/list-up/:listid/:itemid" request
	    (lists/list-up request (-> request :params :listid)
							   (-> request :params :itemid)))

	(cc/GET "/list-down/:listid/:itemid" request
	    (lists/list-down request
						 (-> request :params :listid)
						 (-> request :params :itemid)))

	(cc/GET "/item-done/:listid/:itemid" request
	    (lists/item-done request
						 (-> request :params :listid)
						 (-> request :params :itemid)))

	(cc/GET "/item-undo/:listid/:itemid" request
	    (lists/item-undo request
						 (-> request :params :listid)
						 (-> request :params :itemid)))
	)

(cc/defroutes admin-routes
  	(cc/GET "/" request (admin/admin-page request))

  	(cc/GET "/new-user" request
  		(a-users/new-user-page request nil))

	(cc/POST "/new-user" request
	    (a-users/new-user! request))

  	(cc/GET "/edit-user/:userid" request
  		(a-users/edit-user-page request (-> request :params :userid) nil))

	(cc/POST "/edit-user" request
	    (a-users/edit-user! request))

  	(cc/GET "/edit-tag/:tagid" request
  		(tags/edit-tag-page request (-> request :params :tagid)))

	(cc/GET "/delete-tag/:id" request
	    (tags/delete-tag! request (-> request :params :id)))

	(cc/GET "/delete-tag-all/:id" request
	    (tags/delete-tag-all! request (-> request :params :id)))

	(cc/POST "/update-tag" request
	    (tags/update-tag! request))

	(cc/GET "/delete-item/:id" request
	    (a-items/delete-item request (-> request :params :id)))

  	(cc/GET "/edit-item/:itemid" request
  		(a-items/edit-item request (-> request :params :itemid)))

  	(cc/POST "/edit-item" request
  		(a-items/update-item request))

	(cc/GET "/new-list" request
	    (a-lists/new-list-page request))

	(cc/POST "/new-list" request
	    (a-lists/added-list! request))

	(cc/GET "/delete-list/:listid" request
	    (a-lists/delete-list! request (-> request :params :listid)))

	(cc/GET "/edit-list/:listid" request
	    (a-lists/edit-list-page request (-> request :params :listid)))

	(cc/POST "/edit-list" request
	    (a-lists/edit-list! request))
	)

;;-----------------------------------------------------------------------------

(cc/defroutes routes
	;; requires user role
  	(cc/context "/user" request
    	(friend/wrap-authorize user-routes #{:user}))

  	;; requires admin role
  	(cc/context "/admin" request
    	(friend/wrap-authorize admin-routes #{:admin}))

  	;; anonymous
  	(cc/GET "/" request
  		(ring/redirect "/user/home"))
	(cc/GET "/login" []
		(admin/auth-page))
  	(friend/logout
  		(cc/ANY "/logout" request
  			(ring/redirect "/user/home")))
	
	;(route/not-found "Page not found")
  	)

;;-----------------------------------------------------------------------------

