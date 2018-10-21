(ns paren.party
  (:require
    [cljsjs.soundjs]
    [manifold-cljs.deferred :as d]
    [manifold-cljs.executor :as ex]
    [manifold-cljs.time :as mt]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defn millis
  []
  (.getTime (js/Date.)))


(defonce parens (atom {}))


(defonce bg-color (atom "#000000"))


(defn page
  []
  [:div.main
   {:style {:background-color @bg-color}}
   (for [[id {:keys [text color size posn rotate]}] @parens
         :when posn
         :let [{:keys [x y]} posn]]
     ^{:key id}
     [:span
      {:style {:position "fixed"
               :left (str (* 100 x) "%")
               :top (str (* 100 y) "%")
               :color color
               :transform (str "rotate(" rotate "deg)")
               :font-size (str size "pt")}}
      text])])


(defn between
  [lo hi frac]
  (+ lo (* frac (- hi lo))))


(defn rand-between
  [lo hi]
  (+ lo (* (rand) (- hi lo))))


(defn rand-color
  []
  (apply str "#" (shuffle [(rand-nth ["CC" "FF"])
                           (rand-nth ["00" "22" "88" "EE"])
                           (rand-nth ["33" "77" "DD"])])))


(def measure 2000)


(defonce music-started (atom 0))


(defn update-parens!
  []
  (swap!
    parens
    (fn [parens]
      (into {}
            (keep
              (fn [[id {:keys [started total size dir start end]
                        :as paren}]]
                (let [now (millis)
                      progress (- now started)
                      rhythm-progress (- now @music-started)
                      %complete (/ progress total)
                      base-x (between (:x start) (:x end) %complete)
                      base-y (between (:y start) (:y end) %complete)
                      horizontal-jump (* dir (Math/cos (* rhythm-progress
                                                          (/ measure)
                                                          Math/PI 4)))
                      jump-x (* 0.015 horizontal-jump)
                      jump-y (* -0.03
                                (/ size 30)
                                (Math/abs (Math/sin (* rhythm-progress (/ measure) Math/PI 4))))
                      x (+ base-x jump-x)
                      y (+ base-y jump-y)
                      rotate (* horizontal-jump 10)]
                  (when (< %complete 1)
                    [id (assoc paren
                               :posn {:x x, :y y}
                               :rotate rotate)]))))
            parens)))
  (let [now (millis)
        n (* (- now @music-started) (/ measure) 2 Math/PI)
        r (max 0 (* 30 (Math/sin n)))
        g (max 0 (* 30 (Math/sin (+ n (* 0.66 Math/PI)))))
        b (max 0 (* 30 (Math/sin (+ n (* 1.33 Math/PI)))))]
    (reset! bg-color (str "rgb(" r "," g "," b ")"))))


(defn add-parens!
  []
  (swap! parens merge
         (let [now (millis)
               size (rand-nth (range 14 30))
               total (* 10000 (/ 30 size))
               color (rand-color)
               [left right] (rand-nth [["(" ")"]
                                       ["[" "]"]
                                       ["{" "}"]])
               start-y (rand-between 0.1 0.9)
               end-y (max 0.1 (min 0.9 (+ start-y (rand-between -0.2 0.2))))]
           {(str "paren-left-" now)
            {:started now
             :total total
             :text left
             :size size
             :color color
             :dir 1
             :start {:x -0.01, :y start-y}
             :end {:x 0.47, :y end-y}}
            (str "paren-right-" now)
            {:started now
             :total total
             :text right
             :size size
             :color color
             :dir -1
             :start {:x 1, :y start-y}
             :end {:x 0.53, :y end-y}}})))


(createjs.Sound.on
  "fileload"
  (fn start-daemons
    []
    (reset! music-started (millis))
    (mt/every (/ 1000 60) #(update-parens!))
    (mt/every (/ measure 1.5) #(add-parens!))
    (mt/every 64000 #(createjs.Sound.play "music"))))


(defonce start
  (createjs.Sound.registerSound
    #js{:src "/paren-party.mp3"
        :id "music"}))


(reagent/render-component
  [page]
  (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
