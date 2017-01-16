(defproject reagent-data-table "2.1.0"
  :description "Sortable/filterable tables for reagent people"
  :url "http://github.com/mjg123/reagent-data-table"
  :license {:name "Apache License Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [reagent "0.6.0-rc"]]

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :cljsbuild {:builds {:dev    {:source-paths ["src" "dev"]

                                :figwheel {:open-urls ["http://localhost:3449/"]}

                                :compiler {:main reagent-data-table.dev
                                           :asset-path "/js/compiled/out"
                                           :output-to "resources/public/js/compiled/reagent_data_table.js"
                                           :output-dir "resources/public/js/compiled/out"
                                           :source-map-timestamp true
                                           :preloads [devtools.preload]}}

                       :demo    {:source-paths ["src" "dev"]
                                :compiler {:output-to "resources/public/js/compiled/reagent_data_table.js"
                                           :main reagent-data-table.dev
                                           :optimizations :advanced
                                           :pretty-print false}}

                       :min    {:source-paths ["src"]
                                :compiler {:output-to "resources/public/js/compiled/reagent_data_table.js"
                                           :main reagent-data-table.core
                                           :optimizations :advanced
                                           :pretty-print false}}}}

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.7.2"]
                                  [figwheel-sidecar "0.5.4-7"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "dev"]
                   :repl-options {:init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
