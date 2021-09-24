(ns user
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [babashka.process :as p]
   [babashka.fs :as fs]
   [babashka.curl :as curl]
   [clojure.pprint :as pprint]))

(prefer-method pprint/simple-dispatch clojure.lang.IPersistentMap clojure.lang.IDeref)

(comment
  (curl/get
   "localhost:9999/profile"
   {:headers {"content-type" "application/json"}
    :query-params {"file" "bar.svg" "duration" (str 5)}}))

(defn -get
  [url m]
  (let [m (walk/stringify-keys m)]
    (println m)
    (curl/get
     url
     {:query-params m
      :headers {"content-type" "application/json"}})))

(comment
  (-get
   "localhost:9999/profile"
   {:file "./results/httpkit.ring-interceptors.java8.G1gc.svg", :duration "60", :async "true"}))

(defn get!
  ([url m]
   (get! url m false))
  ([url m async?]
   (if async?
     (future (-get url m))
     (-get url m))))

(comment
  (def p (get! "localhost:9999/profile" {:file "bar.svg" :duration 5} true)))

(defn process-histogram
  [file]
  (println "processing histogram")
  (let [base (str/replace file #"\.out" "")
        png (str base ".png")
        title (fs/file-name base)
        cmd ["hdr-plot" "--output" png "--title" title file]]
    (p/sh cmd)
    (println "wrote histogram to" png)))

(defn format-gc
  [gc]
  (-> gc str (str/replace  #"\-XX:\+Use" "") (str/replace  #"GC" "")))

(defn output-file-name
  [opts]
  (let [{:keys [name rate threads connections duration java gc async?]}
        opts
        gc (format-gc gc)]
    (str name
         (if async? ".async" ".sync")
         ".java" java
         "." gc "GC"
         ".r" rate
         ".t" threads
         ".c" connections
         ".d" duration
         ".out")))

(defn wrk-cmd
  [opts]
  (let [{:keys [rate threads connections duration url]} opts]
    ["wrk"
     "--latency"
     "-H"
     "content-type: application/text"
     (str "-R" rate)
     (str "-t" threads)
     (str "-c" connections)
     (str "-d" duration)
     url]))

(defn do-wrk
  [opts]
  (let [wrk-cmd (wrk-cmd opts)
        out (str "results/" (output-file-name opts))]
    (println "running:" wrk-cmd)
    (let [proc (p/check (p/process wrk-cmd))]
      (println "wrk done")
      (when-not (:skip-report opts)
        (let [report (slurp (:out proc))]
          (spit out report)
          (process-histogram out))))))

(def default-profile-duration 60)

(defn do-warmup
  [{:keys [d]
    :or {d "60"}
    :as opts}]
  (println "warming up")
  (let [proc (->
              opts
              (merge
               {:rate "10k"
                :threads "12"
                :connections "400"
                :skip-report true
                :duration (str d)})
              wrk-cmd
              (p/process {:out :inherit})
              p/check)
        exit (:exit proc)]
    (println "warmup finished with status" exit)
    exit))

(defn serve-cmd
  [opts]
  (let [{:keys [server route java-opts async? jar]} opts]
    (cond->
        (conj (into ["java"] java-opts) "-jar" jar server route)
      async? (conj "true"))))

(def startup-period 15)

(defn wait
  [s]
  (Thread/sleep (* s 1000)))

(defn serve
  [opts]
  (let [cmd (serve-cmd opts)]
    (println "serving" cmd "with env" opts)
    (let [proc (p/process cmd {:out :inherit})]
      (println "checking for abnormal exit:" (:exit proc))
      (println "waiting for server to stabilize")
      (wait startup-period)
      proc)))

(comment
  (def proc (p/process (serve {:server 'httpkit :route 'ring})))
  (println (slurp (:err proc))))

(defn profile
  [opts]
  (println "running profile")
  (let [{:keys [server route async? java gc java-opts]} opts
        java-opts' ["-Djdk.attach.allowAttachSelf"
                    "-XX:+UnlockExperimentalVMOptions"
                    "-XX:+UnlockDiagnosticVMOptions"
                    "-XX:+DebugNonSafepoints"
                    (str gc)]
        java-opts (into [] (concat (or java-opts []) java-opts'))]
    (println java-opts)
    (let [opts (assoc opts :java-opts java-opts)
          proc (serve opts)
          file (format "./results/%s.%s.%s.java%s.%sGC.svg" server route (if async? "async" "sync") java (format-gc gc))]
      (when (nil? (:exit proc))
        (when (zero? (do-warmup (assoc opts :d "120")))
          (future
            (wait 10)
            (println (get! "localhost:9999/profile"
                           {:file file
                            :duration (str default-profile-duration)
                            :async (str true)}))
            (println "Profiling request sent"))
          (do-warmup (assoc opts :d "120"))
          (p/destroy proc)
          true)))))

(defn duration
  [minutes]
  (str (* minutes 60) "s"))

(defn jenv
  [cmd]
  (p/sh (into ["jenv"] cmd)))

(defn jenv-version
  []
  (jenv ["version"]))

(comment
  (:out (jenv-version)))

(defn jenv-local
  [v]
  (println "Setting java version" v)
  (jenv ["local" (str v)])
  (println (:out (jenv-version))))

(comment
  (jenv-local 1.8))

(def spec
  '{httpkit {rate ["10k" "30k" "50k" "60k"]
             async? true}
    undertow {rate ["10k" "30k" "50k" "60k"]
              async? true}
    pohjavirta {rate ["10k" "30k" "50k" "60k"]}
    aleph {rate ["10k" "20k" "30k"] async? true}
    jetty {rate ["10k" "30k" "50k" "60k"] async? true}})

(def java-specs
  '{
    8 {gc [-XX:+UseG1GC -XX:+UseParallelGC] jenv-version 1.8}
    graal8 {gc [-XX:+UseG1GC -XX:+UseParallelGC] jenv-version graalvm64-1.8.0.302}
    11 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC]}
    graal11 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC] jenv-version graalvm64-11.0.12}
    15 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC -XX:+UseShenandoahGC]}
    16 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC -XX:+UseShenandoahGC]}
    17 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC -XX:+UseShenandoahGC]}
    graal16 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC] jenv-version graalvm64-16.0.2}
    })

(def jar "target/uberjar/server.jar")
(def url "http://localhost:9999/math/plus?x=1&y=2")

(defn -wrk-flow
  [f opts]
  (doseq [:let [{:keys [server route]} opts]
          rate (get-in spec [server 'rate])
          t [16]
          c [400]
          d [10] ;; minutes
          :let [d (duration d)
                env {:threads t
                     :connections c
                     :duration d
                     :name (str server "." route)
                     :rate rate}]]
    (f (merge opts env))))

(defn run-flow
  [f opts]
  (doseq [server '[httpkit jetty aleph pohjavirta undertow]
          route '[ring-middleware ring-interceptors]
          java [8 11 15 17]
          :let [gcs (get-in java-specs [java 'gc])
                async? (if (get-in spec [server 'async?]) [true false] [false])]
          async async?
          gc gcs
          :let [env {:name (str server "." route)
                     :server server
                     :url url
                     :async? async
                     :route route
                     :java java
                     :gc gc}]
          :while (f (merge opts env))]
    (println 'done opts)))

(defn set-jenv
  [opts]
  (let [java (:java opts)
        version (get-in java-specs [java 'jenv-version] java)]
    (jenv-local version)))

(defn main-flow
  [opts]
  (set-jenv opts)
  (profile opts)
  (let [opts (assoc opts :java-opts ["-XX:+UnlockExperimentalVMOptions" "-Djdk.attach.allowAttachSelf" (:gc opts)])
        proc (serve opts)]
    (when (nil? (:exit proc))
      (when (zero? (do-warmup opts))
        (-wrk-flow do-wrk opts)
        (p/destroy proc)
        true))))

(defn -main
  [opts]
  (run-flow main-flow opts)
  (println "bye"))

(comment
  (-main {:url url :jar jar}))

(comment
  ;; spit results to file for easy viewing
  (->>
   (fs/list-dir "results")
   (filter #(str/ends-with? % ".png"))
   sort
   (map (fn [s] (format "![](%s)" s)))
   (str/join "\n\n")
   (spit "results.md"))

  )
