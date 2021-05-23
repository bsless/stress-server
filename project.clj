(defproject com.github.bsless/stress-server "TEST"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]

                 [io.pedestal/pedestal.jetty "0.5.9"]
                 [io.pedestal/pedestal.service "0.5.9"]

                 [metosin/jsonista "0.3.3"]
                 [metosin/malli "0.5.1"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.13"]
                 [metosin/reitit-pedestal "0.5.13"]
                 [metosin/sieppari "0.0.0-alpha13"]
                 [aleph "0.4.7-alpha5"]
                 [ring/ring-jetty-adapter "1.9.3"]
                 [http-kit "2.5.3"]
                 [com.appsflyer/donkey "0.5.1"]
                 [metosin/pohjavirta "0.0.1-alpha7"]
                 [com.clojure-goes-fast/clj-async-profiler "0.5.0"]
                 ]
  :main com.github.bsless.stress-server
  :target-path "target/%s"
  :uberjar-name "server.jar"
  :profiles
  {:uberjar {:aot :all
             :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                        "-Dclojure.compiler.elide-meta=[:doc :file :line :added]"
                        "-Djdk.attach.allowAttachSelf"
                        "-XX:+UnlockDiagnosticVMOptions"
                        "-XX:+DebugNonSafepoints"]}
   :dev {:jvm-opts ["-Djdk.attach.allowAttachSelf"
                    "-XX:+UnlockDiagnosticVMOptions"
                    "-XX:+DebugNonSafepoints"]
         :plugins [[lein-ancient "0.6.15"]]}})
