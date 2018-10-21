(ns paren.party
  (:require
    [manifold-cljs.deferred :as d]
    [manifold-cljs.executor :as ex]
    [manifold-cljs.time :as mt]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Hi")

;; define your app data so that it doesn't get over-written on reload

(def app-state
  (atom
    {:title ""
     :subtitle ""}))


(defn animate-text!
  [data-key title]
  (d/loop [chars title]
    (when-not (empty? chars)
      (d/chain
        (mt/in (+ 30 (rand-int 30)) (constantly nil))
        (fn next-letter
          []
          (swap! app-state update-in data-key str (first chars))
          (d/recur (rest chars)))))))


(defn page
  []
  [:div "sup"])


(reagent/render-component
  [page]
  (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
