(ns multi-client-ws.core
  (:require [reagent.core :as reagent :refer [atom]]
            [multi-client-ws.websockets :as ws]))

(defonce messages (atom []))
(defonce player (atom nil))
(defonce my-turn? (atom false))
(defonce scores (atom nil))

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
      [:div.col-md-12
       [:h4 "Scores"]]
      [:div.container
       (for [[player score] @scores]
         [:div.col-sm-6 (str player ":" score)])]]
     [:div.row
      [:div.col-sm-12
       [message-list]]]
     (if (not @player)
       [:div.row
        [:div.col-sm-6
         [player-name-element player-name]]]
       (when @my-turn?
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
             [:input {:id       "hold"
                      :name     "hold"
                      :class    "form-control"
                      :type     "submit"
                      :value    "hold"
                      :on-click #(ws/send-transit-msg!
                                   {:message {:type :hold :player @player}})}]
             ]]]]))]))

(defn update-messages! [message]
  (println "Received message:" message)
  (swap! messages #(vec (take-last 10 (conj % message)))))

(defn update-turn! [value]
  (reset! my-turn? value))

(defn update-scores! [new-scores]
  (reset! scores new-scores))

(defn dispatch-message! [message]
  (cond
    (:message message) (update-messages! (:message message))
    (:scores message) (update-scores! (:scores message))
    (= :hold message) (update-turn! false)
    (= :your-turn message) (update-turn! true)))

(defn mount-components []
  (reagent/render-component [#'home-page] (.getElementById js/document "app")))

(defn init! []
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") dispatch-message!)
  (mount-components))