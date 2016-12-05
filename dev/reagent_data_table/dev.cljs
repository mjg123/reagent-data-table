(ns reagent-data-table.dev
  (:require [reagent.core :as r]
            [reagent-data-table.core :as rdt]))

(enable-console-print!)

(println "HELLO")

(def data1
  [{:id 1 :name "Alice"   :age 23}
   {:id 2 :name "Bob"     :age 28}
   {:id 3 :name "Charlie" :age 32}
   {:id 4 :name "David"   :age 41}
   {:id 5 :name "Everlasting Ermine"}])

(def app-state (r/atom {:table-data data1}))

(r/render-component

 [:div

  [:h2 "A very plain table"]
  [:div.table-container
   [rdt/data-table {:headers [[:id "ID"] [:name "Name"] [:age "Age"]]
                    :rows    (:table-data @app-state)}]]


  [:h2 "A snazzy table"]
  [:div.table-container
   [rdt/data-table {:table-id "table-1"

                    :headers [[:id "ID"] [:name "Name"] [:age "Age"]]
                    :rows    (:table-data @app-state)

                    :no-data-label [:span.info "~~unknowable~~"]

                    :filterable-columns [:age :name]
                    :filter-label "Search by age or name:"
                    :filter-string "e"

                    :sortable-columns [:id :name :age]
                    :sort-columns [[:age true]]

                    :table-state-change-fn #(.log js/console %)}]]]

 (.getElementById js/document "dt-app"))
