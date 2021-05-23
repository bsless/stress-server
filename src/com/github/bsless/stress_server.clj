(ns com.github.bsless.stress-server
  (:require
   [io.pedestal.http :as server]
   [reitit.pedestal :as pedestal]
   [org.httpkit.server :as httpkit]
   [aleph.http :as aleph]
   [ring.adapter.jetty :as jetty]
   [com.github.bsless.stress-server.ring :as ring]
   [com.github.bsless.stress-server.ring-middleware :as ring-middleware]
   [com.github.bsless.stress-server.pedestal :as p])
  (:gen-class))

(def port 9999)

(defn pedestal
  [router]
  (-> {:env :dev
       ::server/type :jetty
       ::server/port port
       ::server/join? false
       ;; no pedestal routes
       ::server/routes []
       ;; allow serving the swagger-ui styles & scripts from self
       ::server/secure-headers {:content-security-policy-settings
                                {:default-src "'self'"
                                 :style-src "'self' 'unsafe-inline'"
                                 :script-src "'self' 'unsafe-inline'"}}}
      (server/default-interceptors)
      ;; use the reitit router
      (pedestal/replace-last-interceptor router)
      (server/dev-interceptors)
      (server/create-server)
      (server/start)))

(defn httpkit
  [app]
  (httpkit/run-server app {:port port}))

(defn jetty
  [app]
  (jetty/run-jetty app {:port port, :join? false}))

(defn aleph
  [app]
  (aleph/start-server app {:port port}))

(defn -main
  [& [server router]]
  (let [router (case router
                 "ring" (ring/app)
                 "ring-middleware" (ring-middleware/app)
                 "pedestal" (p/router))]
    (case server
      "pedestal" (pedestal router)
      "httpkit" (httpkit router)
      "jetty" (jetty router)
      "aleph" (aleph router))
   (deref (promise))))
