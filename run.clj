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
(def route 'ring)

(comment
  (curl/get
   "http://localhost:9999/profile"
   {:headers {"content-type" "application/json"}
    :query-params {"file" "bar.svg" "duration" (str 5)}}))

(defn -get
  [url m]
  (curl/get
   url
   {:query-params (walk/stringify-keys m)
    :headers {"content-type" "application/json"}}))

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


(defn output-file-name
  [name r t c d]
  (str name ".r" r ".t" t ".c" c ".d" d ".out"))

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
  [name rate t c d url]
  (let [wrk-cmd (wrk rate t c d url)
        out (str "results/" (output-file-name name rate t c d))]
    (println "running:" wrk-cmd)
    (let [proc (p/process wrk-cmd)
          report (slurp (:out (p/check proc)))]
      (spit out report)
      (println "wrk done")
      (process-histogram out))))

(def warmup (str "wrk -R10k -t12 -c400 -d240s " (pr-str url)))

(defn do-warmup
  []
  (println "warming up")
  (let [proc (p/check (p/process warmup {:out :inherit}))
        exit (:exit proc)]
    (println "warmup finished with status" exit)
    exit))

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

(def default-profile-duration 60)

(defn profile
  [server route]
  (let [proc (do-serve server route ["-Djdk.attach.allowAttachSelf"
                                     "-XX:+UnlockDiagnosticVMOptions"
                                     "-XX:+DebugNonSafepoints"])
        file (format "./results/%s.%s.svg" server route)]
    (do-warmup)
    (future
      (Thread/sleep 5000)
      (println (get! "localhost:9999/profile" {:file file :duration default-profile-duration}))
      (println "Profiling request sent"))
    (do-warmup)
    (p/destroy proc)))

(comment
  (profile "httpkit" "ring"))

(defn duration
  [minutes]
  (str (* minutes 60) "s"))

(def spec
  '{httpkit
    {rate ["60k" "75k"]}
    pohjavirta
    {rate ["60k" "75k" "90k"]}
    aleph
    {rate ["10k"]}
    jetty
    {rate ["50k" "60k" "70k"]}})

(defn -wrk
  [server route]
  (doseq [rate (get-in spec [server 'rate])
          t [16]
          c [400]
          d [10] ;; minutes
          :let [name (str server "." route)
                d (duration d)]]
    (wait 20)
    (do-wrk name rate t c d url)))

(defn -main
  []
  (doseq [server '[httpkit jetty aleph pohjavirta]
          route '[ring ring-middleware]
          :let [_ (profile server route)
                proc (do-serve server route)]
          :while (nil? (:exit proc))
          :let [exit (do-warmup)]
          :while (zero? exit)]

    (-wrk server route)

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
