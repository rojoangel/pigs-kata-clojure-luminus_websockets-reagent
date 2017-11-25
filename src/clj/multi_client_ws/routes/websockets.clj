(ns multi-client-ws.routes.websockets
  (:require [compojure.core :refer [GET defroutes wrap-routes]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            [pigs.core :as pigs]))

(defonce channels (atom #{}))
(defonce game (atom (pigs/new-game)))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

(defn- decode [msg]
  (-> msg
      (.getBytes "UTF-8")
      (java.io.ByteArrayInputStream.)
      (transit/reader :json)
      (transit/read)))

(defn- encode [msg]
  (let [out (java.io.ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer msg)
    (.toString out)))

(defn notify-clients! [channel msg]
  (let [message (encode msg)]
    (log/info "notify clients:" msg)
    (doseq [channel @channels]
      (async/send! channel message))))

(defn dispatch-message! [channel msg]
  (let [message (:message (decode msg))]
    (case (:type message)
      :join
      (do
        (swap! game pigs/add-player)
        (notify-clients! channel {:message (str (:player message) " joined the game")}))

      :roll
      (let [rolled-value (inc (rand-int 6))]
        (notify-clients! channel {:message (str (:player message) " rolled a " rolled-value)})
        (swap! game #(pigs/roll % rolled-value))
        (notify-clients! channel {:message (str (:player message) "'s current rolls are " (:current-player-rolls @game))}))

      ;default
      (log/error "received unknown message" message))))

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open    connect!
   :on-close   disconnect!
   :on-message dispatch-message!})

(defn ws-handler [request]
  (async/as-channel request websocket-callbacks))

(defroutes websocket-routes
           (GET "/ws" [] ws-handler))
