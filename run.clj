(ns user
  (:require
   [clojure.string :as str]
   [babashka.process :as p]
   [babashka.fs :as fs]
   [babashka.curl :as curl]))

(def jar "target/uberjar/server.jar")
(def url "http://localhost:9999/math/plus?x=1&y=2")

;;; hdr-plot --output myplot.png --title "My plot" ./results/httpkit.ring.r100k.t16.c400.d480s.out
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
  (let [proc (p/sh warmup)
        exit (:exit proc)]
    (println "warmup finished with status" exit)
    exit))

(defn serve
  [server route]
  ["java" "-jar" jar server route])

(def startup-period 15)

(defn wait
  [s]
  (Thread/sleep (* s 1000)))

(defn do-serve
  [server route]
  (let [cmd (serve server route)]
    (println "running" cmd)
    (let [proc (p/process cmd)]
      (println "checking for abnormal exit:" (:exit proc))
      (println "waiting for server to stabilize")
      (wait startup-period)
      proc)))

(comment
  (def proc (p/process (serve 'httpkit 'ring)))
  (println (slurp (:err proc))))

(defn duration
  [minutes]
  (str (* minutes 60) "s"))

(def server 'httpkit)
(def route 'ring)

(defn -wrk
  [server route]
  (doseq [rate ["50k" "60k" "75k" "90k"]
          t [#_4 16 #_24]
          c [#_200 400 #_800]
          d [10] ;; minutes
          :let [name (str server "." route)
                d (duration d)]]
    (wait 20)
    (do-wrk name rate t c d url)))

(defn -main
  []
  (doseq [server '[httpkit jetty aleph]
          route '[ring ring-middleware]
          :let [proc (do-serve server route)]
          :while (nil? (:exit proc))
          :let [exit (do-warmup)]
          :while (zero? exit)]

    (-wrk server route)

    (p/destroy proc))
  (println "bye"))

(comment
  (-main))



(comment
  (def res (babashka.process/process "sleep 10"))
  (println (babashka.process/process "sleep 10"))

  (p/check (p/destroy (babashka.process/process "sleep 10")))

  (slurp (:err res))

  (def resp (curl/get "localhost:9999/profile")))
