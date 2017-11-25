(ns multi-client-ws.core
  (:require [reagent.core :as reagent :refer [atom]]
            [multi-client-ws.websockets :as ws]))

(defonce messages (atom []))
(defonce player (atom nil))

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
                              {:message {:type :join :player @value}})
                            (reset! player @value)
                            (reset! value nil))}]])

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
       [:h3 (str "Let's play Pigs " @player)]]]
     [:div.row
      [:div.col-sm-6
       [message-list]]]
     (if (not @player)
       [:div.row
        [:div.col-sm-6
         [player-name-element player-name]]]
       [:div.container
        [:div.row
         [:div.col-sm-6
          [:div
           [:input {:id       "roll"
                    :name     "roll"
                    :class    "form-control"
                    :type     "submit"
                    :value    "roll"
                    :on-click #(ws/send-transit-msg!
                                 {:message {:type :roll :player @player}})}]
           [:input {:id    "hold"
                    :name  "hold"
                    :class "form-control"
                    :type  "submit"
                    :value "hold"
                    :on-click #(ws/send-transit-msg!
                                 {:message {:type :hold :player @player}})}]
           ]]]])]))

(defn update-messages! [{:keys [message]}]
  (println "Received message:" message)
  (swap! messages #(vec (take-last 10 (conj % message)))))

(defn mount-components []
  (reagent/render-component [#'home-page] (.getElementById js/document "app")))

(defn init! []
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (mount-components))