(ns com.github.bsless.stress-server.ring-middleware
  (:require
   [reitit.ring :as ring]
   [reitit.swagger-ui :as swagger-ui]
   [com.github.bsless.stress-server.prof :as prof]
   [com.github.bsless.stress-server.routes :as routes]
   [com.github.bsless.stress-server.reitit.ring.middleware :as rmw]
   [com.github.bsless.stress-server.reitit.ring :as rr]))

(defn app
  []
  (ring/ring-handler
   (ring/router
    [(routes/swagger) prof/route (routes/math)]
    (rmw/options))
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler))
   rr/options))
