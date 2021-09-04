(ns com.github.bsless.stress-server.routes
  (:require
   [reitit.swagger :as swagger]
   [com.github.bsless.stress-server.handler :as handler]))

(defn swagger
  []
  ["/swagger.json"
   {:get {:no-doc true
          :swagger {:info {:title "my-api"
                           :description "with [malli](https://github.com/metosin/malli) and reitit-ring"}
                    :tags [{:name "files", :description "file api"}
                           {:name "math", :description "math api"}]}
          :handler (swagger/create-swagger-handler)}}])

(defn math
  []
  ["/math"
   {:swagger {:tags ["math"]}}
   ["/plus"
    {:get {:summary "plus with malli query parameters"
           :parameters {:query [:map [:x int?] [:y int?]]}
           :responses {200 {:body [:map [:total int?]]}}
           :handler handler/get-add}
     :post {:summary "plus with malli body parameters"
            :parameters {:body [:map [:x int?] [:y int?]]}
            :responses {200 {:body [:map [:total int?]]}}
            :handler handler/post-add}}]])
