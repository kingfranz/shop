(ns shop2.controllers.routes
    (:require [clojure.string :as str]
              [clojure.pprint :as pp]
              [ring.util.response :as ring]
              [slingshot.slingshot :refer [throw+ try+]]
              [compojure.core :as cc]
              [cemerick.friend :as friend]
              [shop2.views.home :refer :all]
              [shop2.views.recipe :refer :all]
              [shop2.views.items :refer :all]
              [shop2.views.menus :refer :all]
              [shop2.views.admin :refer :all]
              [shop2.views.lists :refer :all]
              [shop2.views.projects :refer :all]
              [shop2.views.admin.users :refer :all]
              [shop2.views.admin.lists :refer :all]
              [shop2.views.admin.items :refer :all]
              [shop2.views.admin.tags :refer :all]
              [shop2.db :refer :all]))

;;-----------------------------------------------------------------------------

(cc/defroutes user-routes
              ;---------------------------------------------
              ; home
              (cc/GET "/home" request
                  (home-page request))

              (cc/GET "/home/tree" request
                  (set-home-type request :tree))

              (cc/GET "/home/prio" request
                  (set-home-type request :prio))

              ;---------------------------------------------
              ; menus
              (cc/GET "/edit-menu" request
                  (edit-menu request))

              (cc/POST "/edit-menu" request
                  (edit-menu! request))

              (cc/GET "/choose-recipe/:menu-date" request
                  (choose-recipe request (-> request :params :menu-date)))

              (cc/GET "/recipe-to-menu/:recipe-id/:menu-date" request
                  (recipe->menu request
                                (-> request :params :recipe-id)
                                (-> request :params :menu-date)))

              (cc/GET "/recipe-from-recipe/:menu-date" request
                  (recipe<-menu request (-> request :params :menu-date)))

              ;---------------------------------------------
              ; recipes
              (cc/GET "/recipe/:id" request
                  (edit-recipe request (-> request :params :id)))

              (cc/GET "/new-recipe" request
                  (new-recipe request))

              (cc/POST "/update-recipe" request
                  (update-recipe! request))

              (cc/POST "/create-recipe" request
                  (create-recipe! request))

              ;---------------------------------------------
              ; projects
              (cc/GET "/projects" request
                  (edit-projects request :by-prio))

              (cc/GET "/projects/:by" request
                  (edit-projects request (-> request :params :by keyword)))

              (cc/POST "/update-projects" request
                  (edit-projects! request))

              (cc/GET "/finish-project/:id" request
                  (finish-proj request (-> request :params :id)))

              (cc/GET "/unfinish-project/:id" request
                  (unfinish-proj request (-> request :params :id)))

              (cc/GET "/clear-projects" request
                  (clear-projs request))

              ;---------------------------------------------
              ; items
              (cc/GET "/add-items/:listid" request
                  (add-items request (-> request :params :listid)))

              (cc/POST "/add-items" request
                  (add-items! request))

              (cc/GET "/add-items/:listid" request
                  (add-items request (-> request :params :listid)))

              (cc/GET "/new-item/:listid" request
                  (new-list-item request (-> request :params :listid)))

              (cc/POST "/new-item" request
                  (new-list-item! request))

              (cc/GET "/add-items/set-sort/:listid/:sort-type" request
                  (set-item-sort request (-> request :params :listid) (-> request :params :sort-type)))

              ;---------------------------------------------
              ; lists
              (cc/GET "/list/:id" request
                  (show-list-page request (-> request :params :id)))

              (cc/GET "/clean-list/:baselst/:targetlst" request
                  (clean-list request (-> request :params :baselst) (-> request :params :targetlst)))

              (cc/GET "/list-up/:listid/:itemid" request
                  (list-up request (-> request :params :listid)
                           (-> request :params :itemid)))

              (cc/GET "/list-down/:listid/:itemid" request
                  (list-down request
                             (-> request :params :listid)
                             (-> request :params :itemid)))

              (cc/GET "/item-done/:listid/:itemid" request
                  (item-done request
                             (-> request :params :listid)
                             (-> request :params :itemid)))

              (cc/GET "/item-undo/:listid/:itemid" request
                  (item-undo request
                             (-> request :params :listid)
                             (-> request :params :itemid)))
              )

;;---------------------------------------------------------------------------------------

(cc/defroutes admin-routes
              ;---------------------------------------------
              ; admin
              (cc/GET "/" request (admin-page request))

              ;---------------------------------------------
              ; users
              (cc/context "/user" []
                  (cc/GET "/new" request
                      (new-user request))

                  (cc/POST "/new" request
                      (new-user! request))

                  (cc/POST "/edit" request
                      (edit-user request (-> request :params :target)))

                  (cc/POST "/edited" request
                      (edit-user! request))

                  (cc/GET "/delete/:userid" request
                      (delete-user! request (-> request :params :userid)))
                  )

              ;---------------------------------------------
              ; lists
              (cc/GET "/new-list" request
                  (new-list request))

              (cc/POST "/new-list" request
                  (new-list! request))

              (cc/POST "/edit-list" request
                  (edit-list request (-> request :params :target)))

              (cc/POST "/edited-list" request
                  (edit-list! request))

              (cc/GET "/delete-list/:listid" request
                  (delete-list! request (-> request :params :listid)))

              ;---------------------------------------------
              ; items
              (cc/GET "/new-item" request
                  (new-item request))

              (cc/POST "/new-item" request
                  (new-item! request))

              (cc/GET "/edit-item/:id" request
                  (edit-item request (-> request :params :id)))

              (cc/POST "/edit-item" request
                  (edit-item request (-> request :params :target)))

              (cc/POST "/edited-item" request
                  (edit-item! request))

              (cc/GET "/bulk-edit-items" request
                  (bulk-edit-items request))

              (cc/POST "/bulk-edit-items" request
                  (bulk-edit-items! request))

              (cc/GET "/delete-item/:id" request
                  (delete-item! request (-> request :params :id)))

              ;---------------------------------------------
              ; tags
              (cc/GET "/new-tag" request
                  (new-tag request))

              (cc/POST "/new-tag" request
                  (new-tag! request))

              (cc/GET "/edit-tag/:tagid" request
                  (edit-tag request (-> request :params :tagid)))

              (cc/POST "/edit-tag" request
                  (edit-tag request (-> request :params :target)))

              (cc/POST "/edited-tag" request
                  (edit-tag! request))

              (cc/GET "/bulk-edit-tags" request
                  (bulk-edit-tags request))

              (cc/POST "/bulk-edit-tags" request
                  (bulk-edit-tags! request))

              (cc/GET "/delete-tag/:id" request
                  (delete-tag! request (-> request :params :id)))

              (cc/GET "/delete-tag-all/:id" request
                  (delete-tag-all! request (-> request :params :id)))
              )

;;-----------------------------------------------------------------------------

(cc/defroutes app-routes
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
                  (auth-page))
              (friend/logout
                  (cc/ANY "/logout" request
                      (ring/redirect "/user/home")))
              )

;;-----------------------------------------------------------------------------

