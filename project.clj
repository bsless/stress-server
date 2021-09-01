(defproject com.github.bsless/stress-server "TEST"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]

                 [io.pedestal/pedestal.jetty "0.5.9"]
                 [io.pedestal/pedestal.service "0.5.9" :exclusions [ring/ring-core]]

                 [metosin/jsonista "0.3.3"]
                 [metosin/malli "0.7.0-20210826.064206-1"]
                 [metosin/muuntaja "0.6.8"
                  :exclusions
                  [com.cognitect/transit-java]]
                 [metosin/reitit "0.5.15"
                  :exclusions
                  [com.fasterxml.jackson.core/jackson-annotations
                   com.cognitect/transit-java]]

                 [metosin/reitit-pedestal "0.5.15"]
                 [metosin/sieppari "0.0.0-alpha13"]
                 [aleph "0.4.7-alpha5"]
                 [luminus/ring-undertow-adapter "1.2.3"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-codec "1.1.3"]
                 [http-kit "2.5.3"]
                 #_
                 [com.appsflyer/donkey "0.5.1" :exclusions [io.netty/netty-transport
                                                            io.netty/netty-codec-dns
                                                            io.netty/netty-codec-socks
                                                            io.netty/netty-buffer
                                                            io.netty/netty-transport-native-unix-common
                                                            io.netty/netty-codec]]
                 [metosin/pohjavirta "0.0.1-alpha7"]
                 [com.clojure-goes-fast/clj-async-profiler "0.5.1"]
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
