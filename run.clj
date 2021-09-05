(ns user
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [babashka.process :as p]
   [babashka.fs :as fs]
   [babashka.curl :as curl]
   [clojure.pprint :as pprint]))

(prefer-method pprint/simple-dispatch clojure.lang.IPersistentMap clojure.lang.IDeref)


(def server 'httpkit)
(def route 'ring-interceptors)
(def gc "-XX:+UseG1GC")
(def java 8)

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

(def jar "target/uberjar/server.jar")
(def url "http://localhost:9999/math/plus?x=1&y=2")

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
  [name r t c d java gc]
  (let [gc (format-gc gc)]
    (str name ".java" java "." gc "GC" ".r" r ".t" t ".c" c ".d" d ".out")))

(defn wrk
  [r t c d url]
  ["wrk"
   "--latency"
   "-H"
   "content-type: application/text"
   (str "-R" r)
   (str "-t" t)
   (str "-c" c)
   (str "-d" d)
   url])

(defn do-wrk
  [name rate t c d url java gc]
  (let [wrk-cmd (wrk rate t c d url)
        out (str "results/" (output-file-name name rate t c d java gc))]
    (println "running:" wrk-cmd)
    (let [proc (p/process wrk-cmd)
          report (slurp (:out (p/check proc)))]
      (spit out report)
      (println "wrk done")
      (process-histogram out))))

(def default-profile-duration 60)

(defn do-warmup
  ([]
   (do-warmup "60"))
  ([d]
   (println "warming up")
   (let [proc (p/check (p/process (wrk "10k" "12" "400" (str d) url) {:out :inherit}))
         exit (:exit proc)]
     (println "warmup finished with status" exit)
     exit)))

(defn serve
  ([server route]
   (serve server route []))
  ([server route opts]
   (conj (into ["java"] opts) "-jar" jar server route)))

(def startup-period 15)

(defn wait
  [s]
  (Thread/sleep (* s 1000)))

(defn do-serve
  ([server route]
   (do-serve server route []))
  ([server route opts]
   (let [cmd (serve server route opts)]
     (println "running" cmd)
     (let [proc (p/process cmd {:out :inherit})]
       (println "checking for abnormal exit:" (:exit proc))
       (println "waiting for server to stabilize")
       (wait startup-period)
       proc))))

(comment
  (def proc (p/process (serve 'httpkit 'ring)))
  (println (slurp (:err proc))))


(defn profile
  [server route java gc]
  (let [proc (do-serve server route ["-Djdk.attach.allowAttachSelf"
                                     "-XX:+UnlockExperimentalVMOptions"
                                     "-XX:+UnlockDiagnosticVMOptions"
                                     "-XX:+DebugNonSafepoints"
                                     (str gc)])
        file (format "./results/%s.%s.java%s.%sGC.svg" server route java (format-gc gc))]
    (do-warmup "70")
    (future
      (Thread/sleep 1000)
      (println (get! "localhost:9999/profile"
                     {:file file
                      :duration (str default-profile-duration)
                      :async (str true)}))
      (println "Profiling request sent"))
    (do-warmup "120")
    (p/destroy proc)))

(comment
  (profile "httpkit" "ring" "8" "G1"))

(defn duration
  [minutes]
  (str (* minutes 60) "s"))

(def spec
  '{httpkit {rate ["10k" "30k" "50k" "60k"]}
    undertow {rate ["10k" "30k" "50k" "60k"]}
    pohjavirta {rate ["10k" "30k" "50k" "60k"]}
    aleph {rate ["10k" #_#_#_"30k" "50k" "60k"]}
    jetty {rate ["10k" "30k" "50k" "60k"]}})

(def java-specs
  '{8 {gc [-XX:+UseG1GC -XX:+UseParallelGC]
       jenv-version 1.8}
    11 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC]}
    15 {gc [-XX:+UseG1GC -XX:+UseParallelGC -XX:+UseZGC -XX:+UseShenandoahGC]}})

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
  (jenv ["local" (str v)]))

(comment
  (jenv-local 1.8))

(defn -wrk
  [server route java gc]
  (doseq [rate (get-in spec [server 'rate])
          t [16]
          c [400]
          d [10] ;; minutes
          :let [name (str server "." route)
                d (duration d)]]
    (wait 20)
    (do-wrk name rate t c d url java gc)))

(defn -main
  []
  (doseq [server '[#_httpkit #_jetty #_aleph pohjavirta #_undertow]
          route '[ring-interceptors ring-middleware]
          java [8 11 15]
          :let [version (get-in java-specs [java 'jenv-version] java)
                gcs (get-in java-specs [java 'gc])
                _ (jenv-local version)]
          gc gcs
          :let [
                _ (profile server route java gc)
                proc (do-serve server route ["-XX:+UnlockExperimentalVMOptions" "-Djdk.attach.allowAttachSelf" gc])]
          :while (nil? (:exit proc))
          :let [exit (do-warmup)]
          :while (zero? exit)]

    (-wrk server route java gc)

    (p/destroy proc))
  (println "bye"))

(comment
  (-main))

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
