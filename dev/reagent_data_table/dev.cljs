(ns reagent-data-table.dev
  (:require [reagent.core :as r]
            [reagent-data-table.core :as rdt]))

(enable-console-print!)

(def data1
  {:data [{:id 1 :name "Alice" :age 23 :info {:favourite-colour "Green" :pet "Cat"}}
          {:id 2 :name "Bob" :age 28 :info {:favourite-colour "Blue" :pet "Dog"}}
          {:id 3 :name "Charlie" :age 32 :info {:favourite-colour "White" :pet "rabbit"}}
          {:id 4 :name "David" :age 41 :info {:favourite-colour "Pink"}}
          {:id 5 :name "Everlasting Ermine"}]})

(def data2
  {:data [{:id 6 :name "Freddy Five Fingers" :age 23}
          {:id 7 :name "Grateful Gertie" :age 28 :info {:catchphrase "Gert Lush"}}
          {:id 8 :name "Harmful Humphrey" :age 32}
          {:id 9 :name "Ignacius In An Igloo" :age 41}
          {:id 10 :name "Jovial Joliet" :age 54}]})

(def child-row-opts
  {:child-row-render-fn (fn [row]
                          (when (not-empty (:info row))
                            [:td {:col-span "4"}
                             [:table.subtable
                              [:tbody
                               (for [[k v] (:info row)]
                                 ^{:key (str row "-info-" (name k))}
                                 [:tr [:td (name k)] [:td v]])]]]))
   :expand-on-click  (fn [row expanding] (.log js/console (str (:name row) " is " (if expanding "expanding" "collapsing"))))
   :expand-button-alignment :left})

(defn- toggle-child-rows
  [app]
  (assoc-in app [:table-data :child-rows]
            (if (-> app :table-data :child-rows)
              nil
              child-row-opts)))

(defn- toggle-left-right-alignment
  [app e]
  (swap! app
         assoc-in [:table-data :child-rows :expand-button-alignment]
         (-> e .-target .-value keyword)))

(def app-state (r/atom {:table-data data1}))

(defn page [app]
  [:div

   [:h2 "Data"]
   [:input {:type "Button" :default-value "Dataset #1" :on-click #(swap! app-state assoc :table-data data1)}]
   [:input {:type "Button" :default-value "Dataset #2" :on-click #(swap! app-state assoc :table-data data2)}]
   [:label {:for "child-row-toggle"} " Child rows?"]
   [:input {:type "Checkbox" :checked (-> @app :table-data :child-rows boolean) :id "child-row-toggle" :on-change #(swap! app-state toggle-child-rows)}]
   [:label {:for "left-right"} " Expand button alignment: "]
   [:select
    {:id "left-right"
     :on-change (partial toggle-left-right-alignment app)}
    [:option {:value :left} "Left"]
    [:option {:value :right} "Right"]]

   [:h2 "A very plain table"]
   [:div.table-container
    [rdt/data-table {:headers    [[:id "ID"] [:name "Name"] [:age "Age"]]
                     :rows       (:data (:table-data @app))
                     :table-id   "plain-table"}]]

   [:h2 "A snazzy table"]
   [:div.table-container
    [rdt/data-table {:table-id              "snazzy-table"
                     :sf-input-id           "search-field"
                     :headers               [[:id "ID"] [:name "Name"] [:age "Age"]]
                     :rows                  (-> @app :table-data :data)
                     :td-render-fn          (fn [row col-id]
                                              (cond (and (= :name col-id)
                                                         (even? (:id row))) [:td [:a {:href (str "http://example.com/pople/" (:name row))} (get row col-id)]]
                                                    :else (if (empty? (str (get row col-id)))
                                                            [:td {:style {:background :gold :display :block}} "~~unknowable~~"]
                                                            (get row col-id))))

                     :filterable-columns    [:age :name]
                     :filter-label          "Search by age or name:"
                     :filter-string         "a"
                     :child-row-opts        (-> @app :table-data :child-rows)
                     :sortable-columns      [:id :name :age]
                     :sort-columns          [[:age true]]
                     :table-state-change-fn #(.log js/console %)}]]])

(r/render-component [page app-state]
                    (.getElementById js/document "dt-app"))
