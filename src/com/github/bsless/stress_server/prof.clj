(ns com.github.bsless.stress-server.prof
  (:require
   [clj-async-profiler.core :as prof]))


(defn run
  [{:keys [duration] :as opts
    :or {duration 60}}]
  (println "Starting profiling with options" opts "for" duration "seconds")
  (prof/profile-for (Long/valueOf duration) opts)
  (println "done"))

(defn handler
  [{{opts :query} :parameters}]
  (future
    (try
      (run opts)
      (catch Exception e
        (println e))))
  {:status 200
   :body "ok"})

(def route
  ["/profile"
   {:get {:handler handler}}])
