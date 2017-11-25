(ns multi-client-ws.core
  (:require [reagent.core :as reagent :refer [atom]]
            [multi-client-ws.websockets :as ws]))

(defonce messages (atom []))
(defonce joined (atom false))

(defn message-list []
  [:ul
   (for [[i message] (map-indexed vector @messages)]
     ^{:key i}
     [:li message])])

(defn input-element
  "An input element which updates its value on change"
  [label id name type value]
  [:div
   [:label label]
   [:input {:id          id
            :name        name
            :class       "form-control"
            :type        type
            :required    ""
            :value       @value
            :on-change   #(reset! value (-> % .-target .-value))
            :on-key-down #(when (= (.-keyCode %) 13)
                            (ws/send-transit-msg!
                              {:message {:type :join :name @value}})
                            (reset! value nil)
                            (swap! joined not))}]])

(defn player-name-element
  [player-name-atom]
  (input-element
    "What's your name?"
    "player-name"
    "player-name"
    "text"
    player-name-atom))

(defn home-page []
  (let [player-name (atom nil)]
    [:div.container
     [:div.row
      [:div.col-md-12
       [:h2 "Let's play Pigs"]]]
     [:div.row
      [:div.col-sm-6
       [message-list]]]
     (if (not @joined)
       [:div.row
        [:div.col-sm-6
         [player-name-element player-name]]])]))

(defn update-messages! [{:keys [message]}]
  (println "Received message:" message)
  (swap! messages #(vec (take 10 (conj % message)))))

(defn mount-components []
  (reagent/render-component [#'home-page] (.getElementById js/document "app")))

(defn init! []
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (mount-components))