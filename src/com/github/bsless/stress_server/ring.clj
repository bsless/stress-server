(ns com.github.bsless.stress-server.ring
  (:require
   [reitit.ring :as ring]
   [reitit.http :as http]
   [reitit.coercion.malli]
   [reitit.ring.malli]
   [malli.util :as mu]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.http.coercion :as coercion]
   [reitit.dev.pretty :as pretty]
   [reitit.interceptor.sieppari :as sieppari]
   [reitit.http.interceptors.parameters :as parameters]
   [reitit.http.interceptors.muuntaja :as muuntaja]
   [reitit.http.interceptors.exception :as exception]
   [reitit.http.interceptors.multipart :as multipart]
   [muuntaja.core :as m]
   [com.github.bsless.stress-server.prof :as prof]
   [sieppari.async.manifold]))

(defn app
  []
  (http/ring-handler
    (http/router
      [["/swagger.json"
        {:get {:no-doc true
               :swagger {:info {:title "my-api"
                                :description "with reitit-http"}}
               :handler (swagger/create-swagger-handler)}}]
       prof/route
       ["/math"
        {:swagger {:tags ["math"]}}
        ["/plus"
         {:get {:summary "plus with malli query parameters"
                :parameters {:query [:map [:x int?] [:y int?]]}
                :responses {200 {:body [:map [:total int?]]}}
                :handler (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body {:total (+ x y)}})}
          :post {:summary "plus with malli body parameters"
                 :parameters {:body [:map [:x int?] [:y int?]]}
                 :responses {200 {:body [:map [:total int?]]}}
                 :handler (fn [{{{:keys [x y]} :body} :parameters}]
                            {:status 200
                             :body {:total (+ x y)}})}}]]]

      {;:reitit.interceptor/transform dev/print-context-diffs ;; pretty context diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
       :exception pretty/exception
       :data {:coercion (reitit.coercion.malli/create
                         {;; set of keys to include in error messages
                          :error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                          ;; schema identity function (default: close all map schemas)
                          :compile mu/closed-schema
                          ;; strip-extra-keys (effects only predefined transformers)
                          :strip-extra-keys true
                          ;; add/set default values
                          :default-values true
                          ;; malli options
                          :options nil})
              :muuntaja m/instance
              :interceptors [;; swagger feature
                             swagger/swagger-feature
                             ;; query-params & form-params
                             (parameters/parameters-interceptor)
                             ;; content-negotiation
                             (muuntaja/format-negotiate-interceptor)
                             ;; encoding response body
                             (muuntaja/format-response-interceptor)
                             ;; exception handling
                             (exception/exception-interceptor)
                             ;; decoding request body
                             (muuntaja/format-request-interceptor)
                             ;; coercing response bodys
                             (coercion/coerce-response-interceptor)
                             ;; coercing request parameters
                             (coercion/coerce-request-interceptor)
                             ;; multipart
                             (multipart/multipart-interceptor)]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))
    {:executor sieppari/executor}))
