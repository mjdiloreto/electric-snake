(ns app.snake
  (:require #?(:clj [datascript.core :as d]) ; database on server
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [missionary.core :as m]
            [clojure.string :as str]
            [clojure.math :as math]
            [hyperfiddle.electric-ui4 :as ui]))

#?(:clj (defonce !grid-size (atom 10)))
#?(:clj (defn start-state [n]
          (vec (for [i (range n)]
                 (vec (for [j (range n)]
                        { :posn [i j] :state :empty }) )))))

#?(:clj (defn rand-pos [n] [(rand-int n) (rand-int n)]))
#?(:clj (defn grid-pos [] (rand-pos @!grid-size)))
#?(:clj (defn initial-state []
          {:direction :down
           :snake (list (grid-pos))
           :food (grid-pos)
           :grid (start-state @!grid-size)}))  ; TODO how to react to grid-size change in say, UI input? (missionary?) (watch + reset!?)


#?(:clj (defonce !running (atom false)))
#?(:clj (defonce !state
          (atom (initial-state))))
(comment (reset! !state (initial-state)))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

#?(:clj (defn move-direction [direction [x y]]
          [(case direction :left (mod (- x 1) @!grid-size) :right (mod (+ x 1) @!grid-size) x)
           (case direction :down (mod (+ y 1) @!grid-size) :up (mod (- y 1) @!grid-size) y)]))

#?(:clj (defn move-snake [state]
          (update-in state
                     [:snake]
                     (fn [snake]
                       (map (partial move-direction (:direction state)) snake)
                       ))))

#?(:clj (defn change-direction [state dir]
          (assoc-in state [:direction] dir)))

#?(:clj (defn opposite-direction  [dir]
          (case dir :left :right :right :left :up :down :down :up)))

#?(:clj (defn update-food [state]
          (if (not (:food state))
            (assoc-in state [:food] (grid-pos))
            state)))

#?(:clj (defn eat-food [state]
          (if (in? (:snake state) (:food state))
            (-> state
                (assoc-in [:food] nil)
                (update-in [:snake] conj (move-direction (opposite-direction (:direction state)) (:food state))))
            state)))

#?(:clj
   (def update-state
     (comp
      update-food
      eat-food
      move-snake)))

#?(:clj
   ;; TODO what if I wanted to try-catch this and js/console.log if there's an error in server-side update?
   (defn game-loop []
     (future
       (while true
         (when @!running
           (swap! !state update-state))
         (Thread/sleep 1000)))))

(comment (game-loop))

(e/defn App []
  (e/client
   (dom/h1 (dom/text "Snake"))
   (dom/button (dom/text "Play/Pause") (dom/on "click" (e/fn [_] (e/server (swap! !running not)))))
   (e/server
    (let [state (e/watch !state)
          grid (:grid state)
          snake (:snake state)]
      (e/for [row grid]
        (e/client
         (dom/on "keydown" (e/fn [e]
                             (let [key (.-key e)
                                   dirs {"ArrowUp" :up "ArrowDown" :down "ArrowRight" :right "ArrowLeft" :left}]
                               (e/server
                                (when-let [dir (dirs key)]
                                  (swap! !state change-direction dir))))))
         (dom/div
          (e/for [cell row]
            (dom/input (dom/props {:type "checkbox"
                                   :checked (= (:posn cell) (:food state))
                                   :indeterminate (in? snake (:posn cell))}))))))))))
