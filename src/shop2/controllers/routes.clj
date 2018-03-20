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
              (cc/context "/home" []
                  (cc/GET "/" request
                      (home-page request))

                  (cc/GET "/tree" request
                      (set-home-type request :tree))

                  (cc/GET "/prio" request
                      (set-home-type request :prio))
                  )

              ;---------------------------------------------
              ; menus
              (cc/context "/menu" []
                  (cc/GET "/edit" request
                      (edit-menu request))

                  (cc/POST "/edit" request
                      (edit-menu! request))

                  (cc/GET "/choose-recipe/:menu-date" request
                      (choose-recipe request (-> request :params :menu-date)))

                  (cc/GET "/recipe+/:recipe-id/:menu-date" request
                      (recipe->menu request
                                    (-> request :params :recipe-id)
                                    (-> request :params :menu-date)))

                  (cc/GET "/recipe-/:menu-date" request
                      (recipe<-menu request (-> request :params :menu-date)))
                  )

              ;---------------------------------------------
              ; recipes
              (cc/context "/recipe" []
                  (cc/GET "/edit/:id" request
                      (edit-recipe request (-> request :params :id)))

                  (cc/GET "/new" request
                      (new-recipe request))

                  (cc/POST "/edit" request
                      (edit-recipe! request))

                  (cc/POST "/new" request
                      (new-recipe! request))
                  )

              ;---------------------------------------------
              ; projects
              (cc/context "/project" []
                  (cc/GET "/edit" request
                      (edit-projects request))

                  (cc/GET "/tag" request
                      (set-group-type request :by-tag))

                  (cc/GET "/prio" request
                      (set-group-type request :by-prio))

                  (cc/POST "/edit" request
                      (edit-projects! request))

                  (cc/GET "/finish/:id" request
                      (finish-proj request (-> request :params :id)))

                  (cc/GET "/unfinish/:id" request
                      (unfinish-proj request (-> request :params :id)))

                  (cc/GET "/clear" request
                      (clear-projs request))
                  )

              ;---------------------------------------------
              ; items
              (cc/context "/item" []
                  (cc/GET "/add/:listid" request
                      (add-items request (-> request :params :listid)))

                  (cc/POST "/add" request
                      (add-items! request))

                  (cc/GET "/new/:listid" request
                      (new-list-item request (-> request :params :listid)))

                  (cc/POST "/new" request
                      (new-list-item! request))

                  (cc/GET "/add/set-sort/:listid/:sort-type" request
                      (set-item-sort request (-> request :params :listid) (-> request :params :sort-type)))
                  )

              ;---------------------------------------------
              ; lists
              (cc/context "/list" []
                  (cc/GET "/get/:id" request
                      (show-list-page request (-> request :params :id)))

                  (cc/GET "/clean/:baselst/:targetlst" request
                      (clean-list request (-> request :params :baselst) (-> request :params :targetlst)))

                  (cc/GET "/up/:listid/:itemid" request
                      (list-up request (-> request :params :listid)
                               (-> request :params :itemid)))

                  (cc/GET "/down/:listid/:itemid" request
                      (list-down request
                                 (-> request :params :listid)
                                 (-> request :params :itemid)))

                  (cc/GET "/done/:listid/:itemid" request
                      (item-done request
                                 (-> request :params :listid)
                                 (-> request :params :itemid)))

                  (cc/GET "/undo/:listid/:itemid" request
                      (item-undo request
                                 (-> request :params :listid)
                                 (-> request :params :itemid)))
                  ))

;;---------------------------------------------------------------------------------------

(cc/defroutes admin-routes
              ;---------------------------------------------
              ; admin
              (cc/GET "/" request
                  (admin-page request))

              (cc/GET "/renew" request
                  (renew-password request))

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
              (cc/context "/list" []
                  (cc/GET "/new" request
                      (new-list request))

                  (cc/POST "/new" request
                      (new-list! request))

                  (cc/POST "/edit" request
                      (edit-list request (-> request :params :target)))

                  (cc/POST "/edited" request
                      (edit-list! request))

                  (cc/GET "/delete/:listid" request
                      (delete-list! request (-> request :params :listid)))
                  )

              ;---------------------------------------------
              ; items
              (cc/context "/item" []
                  (cc/GET "/new" request
                      (new-item request))

                  (cc/POST "/new" request
                      (new-item! request))

                  (cc/GET "/edit/:id" request
                      (edit-item request (-> request :params :id)))

                  (cc/POST "/edit" request
                      (edit-item request (-> request :params :target)))

                  (cc/POST "/edited" request
                      (edit-item! request))

                  (cc/GET "/bulk-edit" request
                      (bulk-edit-items request))

                  (cc/POST "/bulk-edit" request
                      (bulk-edit-items! request))

                  (cc/GET "/delete/:id" request
                      (delete-item! request (-> request :params :id)))
                  )

              ;---------------------------------------------
              ; tags
              (cc/context "/tag" []
                  (cc/GET "/new" request
                      (new-tag request))

                  (cc/POST "/new" request
                      (new-tag! request))

                  (cc/GET "/edit/:tagid" request
                      (edit-tag request (-> request :params :tagid)))

                  (cc/POST "/edit" request
                      (edit-tag request (-> request :params :target)))

                  (cc/POST "/edited" request
                      (edit-tag! request))

                  (cc/GET "/bulk-edit" request
                      (bulk-edit-tags request))

                  (cc/POST "/bulk-edit" request
                      (bulk-edit-tags! request))

                  (cc/GET "/delete/:id" request
                      (delete-tag! request (-> request :params :id)))

                  (cc/GET "/deleteall/:id" request
                      (delete-tag-all! request (-> request :params :id)))
                  ))

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

