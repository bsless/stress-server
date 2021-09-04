(ns com.github.bsless.stress-server.ring-interceptors
  (:require
   [reitit.ring :as ring]
   [reitit.http :as http]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.interceptor.sieppari :as sieppari]
   [com.github.bsless.stress-server.prof :as prof]
   [com.github.bsless.stress-server.reitit.ring :as rr]
   [com.github.bsless.stress-server.routes :as routes]
   [com.github.bsless.stress-server.reitit.ring.interceptors :as ri]))

(defn app
  []
  (http/ring-handler
    (http/router
      [(routes/swagger) prof/route (routes/math)]
      (ri/options))
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))
    (merge
     rr/options
     {:executor sieppari/executor})))
