(ns com.github.bsless.stress-server.prof
  (:require
   [clojure.set :as set]
   [malli.util :as mu]
   [clj-async-profiler.core :as prof]
   [malli.core :as m]
   [clojure.java.io :as io]))

(defonce event-types (prof/list-event-types))

(def start-options
  [:map
   [:interval {:optional true} pos-int?]
   [:framebuf {:optional true} pos-int?]
   [:threads {:optional true} boolean?]
   [:event {:optional true} (into [:enum] event-types)]])

(def stop-options
  [:map
   [:min-width {:optional true} pos-int?]
   [:width {:optional true} pos-int?]
   [:height {:optional true} pos-int?]
   [:title {:optional true} string?]
   [:reverse {:optional true} boolean?]
   [:icicle {:optional true} boolean?]])

(def my-options
  [:map
   [:duration {:optional true :default 60} pos-int?]
   [:file {:optional true} string?]
   [:async {:optional true :default true} boolean?]])

(def my-keys (map first (m/children my-options)))

(def options
  (mu/merge start-options (mu/merge stop-options my-options)))

(defn rename-keys
  [m]
  (set/rename-keys m {:reverse :reverse?
                      :icicle :icicle?}))

(defn run
  [{:keys [duration] :as opts
    :or {duration 60}}]
  (println "Starting profiling with options" opts)
  (try
    (let [file (:file opts)
          opts (apply dissoc opts my-keys)
          ret (prof/profile-for duration opts)]
      (when file
        (let [in ret
              out (io/file file)]
          (try
            (io/copy in out)
            (catch Exception e
              (println e))))))
    (catch Exception e
      (prof/stop)
      (println e)))
  (println "done"))

(comment
  (run {:duration 5 :file "foo.png"}))

(defn handler
  ([{{opts :query} :parameters}]
   (let [opts (rename-keys opts)]
     (if (:async opts)
       (future
         (try
           (run opts)
           (catch Exception e
             (println e))))
       (run opts)))
   {:status 200
    :body "ok"})
  ([req respond raise]
   (respond (handler req))))

(def route
  ["/profile"
   {:get
    {:summary "Run clj-async-profiler"
     :parameters {:query options}
     :handler handler}}])
