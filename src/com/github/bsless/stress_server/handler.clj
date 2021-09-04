(ns com.github.bsless.stress-server.handler)

(defn get-add
  ([{{{:keys [x y]} :query} :parameters}]
   {:status 200
    :body {:total (+ x y)}})
  ([{{{:keys [x y]} :query} :parameters} respond _raise]
   (respond
    {:status 200
     :body {:total (+ x y)}})))

(defn post-add
  ([{{{:keys [x y]} :body} :parameters}]
   {:status 200
    :body {:total (+ x y)}})
  ([{{{:keys [x y]} :body} :parameters} respond _raise]
   (respond
    {:status 200
     :body {:total (+ x y)}})))

(defn stub
  ([_]
   {:status 200
    :body "ok"})
  ([_ respond _]
   (respond
    {:status 200
     :body "ok"})))
