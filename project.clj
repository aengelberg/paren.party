(defproject paren.party "0.1.0-SNAPSHOT"
  :description "PAREN PARTY"
  :url "http://paren.party"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.238"]
   [org.clojure/core.async "0.4.474"]
   [manifold-cljs "0.1.7-1"]
   [reagent "0.7.0"]]

  :plugins
  [[lein-figwheel "0.5.16"]
   [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :figwheel {:on-jsload "paren.party/on-js-reload"}
     :compiler {:main paren.party
                :asset-path "js/compiled/out"
                :output-to "resources/public/js/compiled/paren.party.js"
                :output-dir "resources/public/js/compiled/out"
                :source-map-timestamp true
                :preloads [devtools.preload]}}
    {:id "min"
     :source-paths ["src"]
     :compiler {:output-to "resources/public/js/compiled/paren.party.js"
                :main paren.party
                :optimizations :advanced
                :pretty-print false}}]}

  :figwheel
  { ;; :http-server-root "public" ;; default and assumes "resources"
   ;; :server-port 3449 ;; default
   ;; :server-ip "127.0.0.1"

   :css-dirs ["resources/public/css"] ;; watch and update CSS

   ;; Start an nREPL server into the running figwheel process
   ;; :nrepl-port 7888

   ;; Server Ring Handler (optional)
   ;; if you want to embed a ring handler into the figwheel http-kit
   ;; server, this is for simple ring servers, if this

   ;; doesn't work for you just run your own server :) (see lein-ring)

   ;; :ring-handler hello_world.server/handler

   ;; To be able to open files in your editor from the heads up display
   ;; you will need to put a script on your path.
   ;; that script will have to take a file path and a line number
   ;; ie. in  ~/bin/myfile-opener
   ;; #! /bin/sh
   ;; emacsclient -n +$2 $1
   ;;
   ;; :open-file-command "myfile-opener"

   ;; if you are using emacsclient you can just use
   ;; :open-file-command "emacsclient"

   ;; if you want to disable the REPL
   ;; :repl false

   ;; to configure a different figwheel logfile path
   ;; :server-logfile "tmp/logs/figwheel-logfile.log"

   ;; to pipe all the output to the repl
   ;; :server-logfile false
   }
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles
  {:dev
   {:dependencies
    [[binaryage/devtools "0.9.9"]
     [figwheel-sidecar "0.5.16"]
     [cider/piggieback "0.3.1"]]
    :source-paths ["dev"]
    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
    :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                      :target-path]}})
