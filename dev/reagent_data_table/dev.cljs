(ns reagent-data-table.dev
  (:require [reagent.core :as r]
            [reagent-data-table.core :as rdt]))

(enable-console-print!)

(def data1
  [{:id 1 :name "Alice"   :age 23}
   {:id 2 :name "Bob"     :age 28}
   {:id 3 :name "Charlie" :age 32}
   {:id 4 :name "David"   :age 41}
   {:id 5 :name "Everlasting Ermine"}])

(def data2
  [{:id 6  :name "Freddy Five Fingers"  :age 23}
   {:id 7  :name "Grateful Gertie"      :age 28}
   {:id 8  :name "Harmful Humphrey"     :age 32}
   {:id 9  :name "Ignacius In An Igloo" :age 41}
   {:id 10 :name "Jovial Joliet"        :age 54}])

(def app-state (r/atom {:table-data data1}))

(defn page [app]
  [:div

   [:h2 "Data"]
   [:input {:type "Button" :default-value "Dataset #1" :on-click #(swap! app-state assoc :table-data data1)}]
   [:input {:type "Button" :default-value "Dataset #2" :on-click #(swap! app-state assoc :table-data data2)}]

   [:h2 "A very plain table"]
   [:div.table-container
    [rdt/data-table {:headers [[:id "ID"] [:name "Name"] [:age "Age"]]
                     :rows    (:table-data @app)}]]


   [:h2 "A snazzy table"]
   [:div.table-container
    [rdt/data-table {:table-id "table-1"

                     :headers [[:id "ID"] [:name "Name"] [:age "Age"]]
                     :rows    (:table-data @app-state)

                     :td-anchor-attributes-fn (fn [row col-id]
                                                (when (and (= :name col-id)
                                                           (even? (:id row)))
                                                  {:href (str "http://example.com/people/" (:name row))}))

                     :no-data-label [:span.info "~~unknowable~~"]

                     :filterable-columns [:age :name]
                     :filter-label "Search by age or name:"
                     :filter-string "a"

                     :sortable-columns [:id :name :age]
                     :sort-columns [[:age true]]
                     :sort-image-base "img/"

                     :table-state-change-fn #(.log js/console %)}]]]  )

(r/render-component [page app-state]
 (.getElementById js/document "dt-app"))
