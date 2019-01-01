(ns paren.party
  (:require
    [cljsjs.soundjs]
    [goog.userAgent :refer [MOBILE WINDOWS]]
    [manifold-cljs.deferred :as d]
    [manifold-cljs.executor :as ex]
    [manifold-cljs.time :as mt]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload

(defn millis
  "Get the current time in milliseconds."
  []
  (.getTime (js/Date.)))


(def manual-start?
  "Certain devices don't allow auto-playing sound, so we have to introduce a
  manual start so the music plays."
  (or MOBILE WINDOWS))


(defonce started? (atom false))


(defonce ready? (atom false))


(defonce paren-config (atom {}))


(defonce latest-tick (atom 0))


(defonce music-started (atom 0))


(defn current-tick
  []
  (- (millis) @music-started))


(def measure 2000)


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


(defn render-paren
  [id tick {:keys [started total size dir start end text color]}]
  (let [progress (- tick started)
        %complete (/ progress total)
        base-x (between (:x start) (:x end) %complete)
        base-y (between (:y start) (:y end) %complete)
        jump-suppression (->> (- %complete 0.9)
                              (* 10)
                              (min 1)
                              (max 0)
                              (- 1))
        horizontal-jump (* dir (Math/cos (* tick
                                            (/ measure)
                                            Math/PI 4))
                           jump-suppression)
        jump-x (* 0.015 horizontal-jump)
        jump-y (* -0.03
                  (/ size 30)
                  (Math/abs (Math/sin (* tick (/ measure) Math/PI 4)))
                  jump-suppression)
        x (+ base-x jump-x)
        y (+ base-y jump-y)
        rotate (* horizontal-jump 10)
        opacity (min 1 (- 1 (/ (- progress total) 1000)))]
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
     text]))


(defn render-parens
  []
  (let [tick @latest-tick
        config @paren-config
        n (* (- tick @music-started) (/ measure) 2 Math/PI)
        r (max 0 (* 20 (Math/sin n)))
        g (max 0 (* 20 (Math/sin (+ n (* 0.66 Math/PI)))))
        b (max 0 (* 20 (Math/sin (+ n (* 1.33 Math/PI)))))]
    [:div.main
     {:style {:background-color (str "rgb(" r "," g "," b ")")}}
     (for [[id paren] config]
       ^{:key id}
       [render-paren id tick paren])]))


(defn gen-pair
  []
  (if (re-find #"egalitarian" (or window.location.href ""))
    (rand-nth [["(" ")"]
               ["[" "]"]
               ["{" "}"]
               ["<" ">"]
               ["“" "”"]
               ["«" "»"]
               ["⦃" "⦄"]
               ["⦅" "⦆"]
               ["〚" "〛"]
               ["⸨" "⸩"]])
    (rand-nth [["(" ")"]
               ["(" ")"]
               ["(" ")"]
               ["[" "]"]
               ["{" "}"]])))


(defn spawn-parens!
  []
  (swap!
    paren-config
    (fn [config]
      (-> config
          (merge
            (let [tick (current-tick)
                  size (rand-nth (range 14 30))
                  total (* 10000 (/ 30 size))
                  color (rand-color)
                  [left right] (gen-pair)
                  start-y (rand-between 0.1 0.9)
                  end-y (max 0.1 (min 0.9 (+ start-y (rand-between -0.2 0.2))))]
              {(str "paren-left-" tick)
               {:started tick
                :total total
                :text left
                :size size
                :color color
                :dir 1
                :start {:x -0.01, :y start-y}
                :end {:x 0.5, :y end-y}}
               (str "paren-right-" tick)
               {:started tick
                :total total
                :text right
                :size size
                :color color
                :dir -1
                :start {:x 1, :y start-y}
                :end {:x 0.5, :y end-y}}}))
          (->>
            (remove
              (fn [[id paren]]
                (< (+ (:started paren) (:total paren) 2000) (current-tick))))
            (into {}))))))


(defn start-the-party!
  []
  (reset! music-started (millis))
  (mt/every (/ 1000 60) #(reset! latest-tick (current-tick)))
  (mt/every (/ measure 1.5) #(spawn-parens!))
  (mt/every 64000 #(createjs.Sound.play "music")))


(createjs.Sound.on
  "fileload"
  (fn maybe-start
    []
    (reset! ready? true)
    (when-not manual-start?
      (start-the-party!))))


(defn page
  []
  [:div
   (if (or @started? (not manual-start?))
     [:div
      {:on-click #(spawn-parens!)}
      [render-parens]]
     [:div.main
      {:style {:background-color "black"}
       :on-click #(when @ready?
                    (reset! started? true)
                    (start-the-party!))}
      [:p.touch-to-start
       (if @ready?
         "Touch to start"
         "Loading...")]])
   [:div.credits
    "Made with <3 by "
    [:a {:href "https://twitter.com/aengelbro"
         :target "_blank"}
     "@aengelbro"]
    " and "
    [:a {:href "https://twitter.com/arrdem"
         :target "_blank"}
     "@arrdem"]]])


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
