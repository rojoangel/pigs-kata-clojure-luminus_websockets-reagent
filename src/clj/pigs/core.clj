(ns pigs.core)

(defn new-game []
  (-> (hash-map)
      (assoc :scores [])
      (assoc :player-turn 1)))

(defn add-player [game]
  (update game :scores conj 0))

(defn- add-rolls-to-current-player-score [game-state]
  (let [current-player-score-idx (dec (:player-turn game-state))
        rolls-total (apply + (:current-player-rolls game-state))]
    (update-in game-state [:scores current-player-score-idx] + rolls-total)))

(defn- next-turn-player [n max]
  (let [next-player (inc n)]
    (if (> next-player max)
      1
      next-player)))

(defn- change-player-turn [game-state]
  (let [players (count (:scores game-state))]
    (update game-state :player-turn next-turn-player players)))

(defn- reset-rolls [game-state]
  (update game-state :current-player-rolls empty))

(defn hold [game-state]
  (-> game-state
      add-rolls-to-current-player-score
      reset-rolls
      change-player-turn))

(defn- add-to-rolls [game-state dice-value]
  (update game-state :current-player-rolls conj dice-value))

(defn roll [game-state dice-value]
  (if (= 1 dice-value)
    (-> game-state
        reset-rolls
        change-player-turn)
    (add-to-rolls game-state dice-value)))

(defn end-game? [game-state]
  (not-every? #(< % 100) (:scores game-state)))

(defn winner [game-state]
  (->> game-state
       :scores
       (map-indexed (fn [idx score] {:player (inc idx) :score score}))
       (filter #(<= 100 (:score %)))
       first
       :player))