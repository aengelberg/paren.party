(ns paren.party
  (:require
    [cljsjs.soundjs]
    [goog.userAgent :refer [MOBILE]]
    [manifold-cljs.deferred :as d]
    [manifold-cljs.executor :as ex]
    [manifold-cljs.time :as mt]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defn millis
  []
  (.getTime (js/Date.)))


(defonce started? (atom false))


(defonce ready? (atom false))


(defonce parens (atom {}))


(defonce bg-color (atom "#000000"))


(defn between
  [lo hi frac]
  (if (>= frac 1)
    hi
    (+ lo (* frac (- hi lo)))))


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


(defn render-parens
  []
  [:div.main
   {:style {:background-color @bg-color}}
   (for [[id {:keys [text color size posn rotate opacity dir]}] @parens
         :when posn
         :let [{:keys [x y]} posn]]
     ^{:key id}
     [:span
      {:style {:position "fixed"
               :left (str (* 100 x) "%")
               :top (str (* 100 y) "%")
               :color color
               :transform (str "rotate(" rotate "deg) "
                               "translate("
                               (if (pos? dir) "-" "")
                               "50%, 0)")
               :font-size (str size "pt")
               :opacity opacity}}
      text])])


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
                      jump-suppression (->> (- %complete 0.9)
                                            (* 10)
                                            (min 1)
                                            (max 0)
                                            (- 1))
                      horizontal-jump (* dir (Math/cos (* rhythm-progress
                                                          (/ measure)
                                                          Math/PI 4))
                                         jump-suppression)
                      jump-x (* 0.015 horizontal-jump)
                      jump-y (* -0.03
                                (/ size 30)
                                (Math/abs (Math/sin (* rhythm-progress (/ measure) Math/PI 4)))
                                jump-suppression)
                      x (+ base-x jump-x)
                      y (+ base-y jump-y)
                      rotate (* horizontal-jump 10)]
                  (cond
                    (< %complete 1)
                    [id (assoc paren
                               :posn {:x x, :y y}
                               :rotate rotate)]

                    (< progress (+ total 1000))
                    [id (assoc paren
                               :posn {:x base-x, :y base-y}
                               :rotate 0
                               :opacity (- 1 (/ (- progress total) 1000)))]))))
            parens)))
  (let [now (millis)
        n (* (- now @music-started) (/ measure) 2 Math/PI)
        r (max 0 (* 20 (Math/sin n)))
        g (max 0 (* 20 (Math/sin (+ n (* 0.66 Math/PI)))))
        b (max 0 (* 20 (Math/sin (+ n (* 1.33 Math/PI)))))]
    (reset! bg-color (str "rgb(" r "," g "," b ")"))))


(defn add-parens!
  []
  (swap! parens merge
         (let [now (millis)
               size (rand-nth (range 14 30))
               total (* 10000 (/ 30 size))
               color (rand-color)
               [left right] (rand-nth [["(" ")"]
                                       ["(" ")"]
                                       ["(" ")"]
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
             :end {:x 0.5, :y end-y}}
            (str "paren-right-" now)
            {:started now
             :total total
             :text right
             :size size
             :color color
             :dir -1
             :start {:x 1, :y start-y}
             :end {:x 0.5, :y end-y}}})))


(defn start-the-party!
  []
  (reset! music-started (millis))
  (mt/every (/ 1000 60) #(update-parens!))
  (mt/every (/ measure 1.5) #(add-parens!))
  (mt/every 64000 #(createjs.Sound.play "music")))


(createjs.Sound.on
  "fileload"
  (fn maybe-start
    []
    (reset! ready? true)
    (when-not MOBILE
      (start-the-party!))))


(defn page
  []
  (if (or @started? (not MOBILE))
    [:div
     {:on-click #(add-parens!)}
     [render-parens]]
    [:div.main
     {:style {:background-color "black"}
      :on-click #(when @ready?
                   (reset! started? true)
                   (start-the-party!))}
     [:p.touch-to-start "Touch to start"]]))


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
