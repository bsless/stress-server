(ns com.github.bsless.httpkit
  (:require
   [org.httpkit.server :as http]))

(defn respond
  [channel]
  (fn -respond [response]
    (http/send! channel response)))

(defn raise
  [channel]
  (fn -raise [?error]
    (http/send! channel ?error)
    (http/close channel)))

(defn ring->httpkit
  [handler]
  (fn
    ([request]
     (when-let [ch (request :async-channel)]
       (handler request (respond ch) (raise ch))
       {:body ch}))))
