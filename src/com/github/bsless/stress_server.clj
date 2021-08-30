(ns com.github.bsless.stress-server
  (:require
   [io.pedestal.http :as server]
   [reitit.pedestal :as pedestal]
   [org.httpkit.server :as httpkit]
   [aleph.http :as aleph]
   [ring.adapter.jetty :as jetty]

   [pohjavirta.server :as pohjavirta]

   [ring.adapter.undertow :as ut]

   [com.github.bsless.stress-server.ring-interceptors :as ring-interceptors]
   [com.github.bsless.stress-server.ring-middleware :as ring-middleware]
   [com.github.bsless.stress-server.pedestal :as p])
  (:gen-class))

(def java-version (System/getProperty "java.version"))
(def java8? (.startsWith java-version "1.8."))

(defmacro above-8
  [& body]
  (when-not java8?
    `(do ~@body)))

(def port 9999)

(above-8
 (println "above java 8, adding a donkey")
 (require
  '[com.appsflyer.donkey.core :as donkey-kong]
  '[com.appsflyer.donkey.server :as ds]
  '[com.appsflyer.donkey.result :as dr :refer [on-success]])

 (defn donkey
   [routes]
   (->
    (donkey-kong/create-donkey)
    (donkey-kong/create-server
     {:port port
      :routes [{:handler routes
                :handler-mode :non-blocking}]})
    (ds/start)
    (dr/on-success (fn [_] (println "Server started listening on port" port))))))

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
  (jetty/run-jetty app {:port port, :join? false :async? true}))

(defn aleph
  [app]
  (aleph/start-server (aleph/wrap-ring-async-handler app) {:port port}))

(defn pohjavirta
  [routes]
  (let [ut (-> routes (pohjavirta/create {:port port}))]
    (pohjavirta/start ut)
    ut))

(defn undertow
  [routes]
  (ut/run-undertow routes {:port port :async? true}))

(def servers
  (merge
   {"pedestal" pedestal
    "httpkit" httpkit
    "jetty" jetty
    "pohjavirta" pohjavirta
    "undertow" undertow
    "aleph" aleph}
   (above-8
    {"donkey" donkey})))

(defn -main
  [& [server router]]
  (let [router (case router
                 "ring-interceptors" (ring-interceptors/app)
                 "ring-middleware" (ring-middleware/app)
                 "pedestal" (p/router))
        go (get servers server)]
    (go router)
   (deref (promise))))
