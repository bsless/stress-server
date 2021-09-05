(ns com.github.bsless.stress-server.reitit.ring.interceptors
  (:require
   [reitit.coercion.malli :as rcm]
   [malli.util :as mu]
   [reitit.swagger :as swagger]
   [reitit.http.coercion :as coercion]
   [reitit.http.interceptors.parameters :as parameters]
   [reitit.http.interceptors.muuntaja :as muuntaja]
   [reitit.http.interceptors.exception :as exception]
   [reitit.http.interceptors.multipart :as multipart]
   [muuntaja.core :as m]
   [sieppari.async.manifold]))

(defn options
  []
  {
   ;; :reitit.interceptor/transform dev/print-context-diffs ;; pretty context diffs
   ;; :validate spec/validate ;; enable spec validation for route data
   ;; :reitit.spec/wrap spell/closed ;; strict top-level validation
   ;; :exception pretty/exception
   :data {:coercion
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
          :interceptors
          [;; swagger feature
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
