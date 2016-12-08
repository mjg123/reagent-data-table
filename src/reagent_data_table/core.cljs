(ns reagent-data-table.core
  (:require [reagent.core   :as reagent]
            [clojure.string :as s]))

(defn- filter-tokens

  "Splits a string of filter text into tokens"

  [s]
  (-> s
      (s/split #"\s+")
      (->>
       (remove empty?))))

(defn- filter-row

  "Predicate fn for deciding whether to show a row when filtering is being applied. All filters must match somewhere
   in a row for it to be shown"

  [s filter-cols row-map]
  (every? identity                           ;; every filter must match
        (for [filter (filter-tokens s)]
          (some identity                     ;; some column
                (for [col ((apply juxt filter-cols) row-map)]
                  (s/index-of (s/upper-case (str col)) (s/upper-case (str filter))))))))

(defn- sort-indicator

  "Generates the image component for the little arrows next to the column name indicating the sort orders"

  [id {sc :sort-columns} sort-image-base]

  (letfn [(h [url] (str sort-image-base url))]

    [:img {:style {:margin-left :8px}
           :src (cond
                  (= id (-> sc first  first)) (if (-> sc first  second) (h "sort_asc.png")     (h "sort_desc.png"))
                  (= id (-> sc second first)) (if (-> sc second second) (h "sort_asc_2nd.png") (h "sort_desc_2nd.png"))
                  :otherwise (h "sort_both.png"))}]))

(defn- update-sort-columns

  "If the first sort-column is already `id` then reverse its sort-order,
   Otherwise insert [id false] as the first and remove other instances of `id` from the list"

  [current-columns id]
  (if (= id (ffirst current-columns))
    (cons [id (-> current-columns first second not)]
          (rest current-columns))
    (cons [id false]
          (remove (fn [col] (= id (first col)))
                  current-columns))))

(defn- update-sort!

  "On-click handler for updating the sort-columns field in the table's state when"

  [id table-state]
  (swap! table-state update :sort-columns update-sort-columns id))

(defn- do-sort

  "Applies the sorting in `sort-columns` to the data in `rows`"

  [sort-columns rows]
  (reduce (fn [r [col reverse?]]
            (sort-by col (if reverse?
                           #(compare %2 %1)
                           #(compare %1 %2)) r))
          rows
          (reverse sort-columns)))

(defn data-table

  "Reagent component for a sortable/filterable table.
   The basic model is that the rows are defined by a seq of maps. Each map contains keys whose values correspond
   to the data which will be shown in the table.

   Takes a single arg which is a map, uses the following keys from that map:

   `:headers`            - A seq of `[col-id text]` where `col-id` is the key looked up in the row-maps, and `text` is the column heading
   `:rows`               - A seq of maps which make provide the table's data

   `:td-render-fn`       - A fn of two args, row and col-id which returns the content of td tags

   `:sortable-columns`   - A seq of `col-id` which dictates which columns will be sortable
   `:filterable-columns` - A seq of `col-id` which dictates which columns will be filterable
   `:filter-label`       - A string used as a label for the filter input. Defaults to: \"Filter by: <col-1-name>, <col-2-name>...\"

   `:filter-string`      - A string to pre-populate the filter input
   `:sort-columns`       - A seq of `[col-id reverse-order?]` pairs which can specify the inital filtering
   `:sort-image-base`      - Where to find the files `sort_asc.png` &c. Default is `/img/`

   `:table-id`           - The value to use as the HTML `id` attribute for the table.  Must be unique if there are multiple tables shown
   `:table-class`        - The value used for the `class` attribute of the table
                           Defaults to `table table-striped table-bordered` which is OK for Bootstrap

   `:table-state-change-fn` - Optionally provide a one-arg fn which is called whenever the state of the table (sorting/filtering) changes
                              This is useful if some other part of your app needs to know about the sorting/filtering (saving user prefs, etc)"


  [{:keys [sortable-columns filter-string sort-columns table-state-change-fn table-class table-id sort-image-base]
                 :or {table-class "table table-striped table-bordered"
                      table-id    ""
                      sort-image-base "/img/"}}]

  (let [table-state (reagent/atom {:filter-string (or filter-string "")
                                   :sort-columns (or sort-columns
                                                     (map (fn [col] [col false]) sortable-columns))})]

    (when table-state-change-fn
      (add-watch table-state :blah
                 (fn [_ _ _ new]
                   (table-state-change-fn new))))



    (fn [{:keys [headers rows sortable-columns filterable-columns filter-string sort-columns filter-label td-render-fn]
          :or {filterable-columns []
               sortable-columns   []
               td-render-fn       (fn [row k]
                                    (get row k))}}]

      [:div
       (when (seq filterable-columns)
         [:label (or filter-label
                     (str "Filter by " (s/join ", " (map (into {} headers) filterable-columns)) ":"))
          [:input {:style {:margin-left :8px}
                   :default-value (:filter-string @table-state)
                   :on-change #(swap! table-state assoc :filter-string (-> % .-target .-value))}]])

       [:table
        {:id table-id :class table-class :cell-spacing "0" :width "100%"}

        [:thead>tr
         (doall
          (for [[col-id title] headers]

            (with-meta
              (if (some #{col-id} sortable-columns)

                [:th {:style {:cursor "pointer"}
                      :on-click #(update-sort! col-id table-state)}
                 title [sort-indicator col-id @table-state sort-image-base]]

                [:th title])
              {:key [col-id table-id]})))]

        [:tbody
         (doall
          (for [row (cond->> rows
                      (seq filterable-columns)      (filter #(filter-row (:filter-string @table-state) filterable-columns %))
                      (:sort-columns @table-state)  (do-sort (:sort-columns @table-state)))]

            ^{:key [row table-id]}
            [:tr
             (for [[k _] headers]
               (let [cell (td-render-fn row k)]
                 (if (and (vector? cell) (= :td (first cell)))
                   cell
                   ^{:key [row k table-id]} [:td cell])))]))]]])))
