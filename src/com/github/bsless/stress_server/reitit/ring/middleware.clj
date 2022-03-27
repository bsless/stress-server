(ns com.github.bsless.stress-server.reitit.ring.middleware
  (:require
   [reitit.coercion.malli :as rcm]
   [reitit.swagger :as swagger]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [muuntaja.core :as m]
   [malli.util :as mu]))

(defn options
  []
  {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
   ;;:validate spec/validate ;; enable spec validation for route data
   ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
   ;; :exception pretty/exception
   :data
   {:coercion
    (rcm/create
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
    :muuntaja (m/create (merge m/default-options {:return :bytes}))
    :middleware
    [;; swagger feature
     swagger/swagger-feature
     ;; query-params & form-params
     parameters/parameters-middleware
     ;; content-negotiation
     muuntaja/format-negotiate-middleware
     ;; encoding response body
     muuntaja/format-response-middleware
     ;; exception handling
     exception/exception-middleware
     ;; decoding request body
     muuntaja/format-request-middleware
     ;; coercing response bodys
     coercion/coerce-response-middleware
     ;; coercing request parameters
     coercion/coerce-request-middleware
     ;; multipart
     multipart/multipart-middleware]}})
