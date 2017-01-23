(ns reagent-data-table.core
  (:require [reagent.core :as reagent]
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
  (every? identity                                          ;; every filter must match
          (for [filter (filter-tokens s)]
            (some identity                                  ;; some column
                  (for [col ((apply juxt filter-cols) row-map)]
                    (s/index-of (s/upper-case (str col)) (s/upper-case (str filter))))))))

(defn- sort-indicator

  "Generates the image component for the little arrows next to the column name indicating the sort orders"

  [id {sc :sort-columns} sort-image-base]

  (letfn [(h [url] (str sort-image-base url))]

    [:img {:style {:margin-left :8px}
           :src   (cond
                    (= id (-> sc first first)) (if (-> sc first second) (h "sort_asc.png") (h "sort_desc.png"))
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

(defn- row-expanded?
  [table-state row-data table-id]
  (get-in @table-state [:child-rows [row-data table-id] :expanded?]))

(defn- toggle-child-row-fn
  "Returns a fn that toggles `:expanded?` for a specific row.
   fn will return args so that it may be composed with a pre-existing on-click handler"
  [table-state row-data table-id]
  (fn [& args]
    (swap! table-state update-in [:child-rows [row-data table-id] :expanded?] not)
    args))

(defn expand-button
  [{:keys [expanded-class collapsed-class] :or {expanded-class "expanded" collapsed-class "collapsed"}}
   table-state row-data table-id]
  (with-meta [:td {:class    (if (row-expanded? table-state row-data table-id)
                               expanded-class collapsed-class)
                   :on-click (toggle-child-row-fn table-state row-data table-id)}]
             {:key [row-data "expand-button" table-id]}))

(defn add-expand-button
  [{:keys [expand-button-alignment] :or {expand-button-alignment :right} :as child-row-opts}
   tr table-state row-data table-id]
  (let [expand-button (expand-button child-row-opts table-state row-data table-id)]
    (if (= :left expand-button-alignment)
      (with-meta (into [(first tr) expand-button] (rest tr)) (meta tr))
      (conj tr expand-button))))

(defn render-child-row
  [{:keys [child-row-render-fn expanded-class collapsed-class] :or {expanded-class "expanded" collapsed-class "collapsed"}}
   table-state row-data table-id]
  (with-meta
    [:tr {:class (if (row-expanded? table-state row-data table-id) expanded-class collapsed-class)}
     (child-row-render-fn row-data)]
    {:key [row-data "child-row" table-id]}))

(defn- row-with-child-row
  [{:keys [child-row-render-fn] :as child-row-opts} table-id table-state tr row-data]
  (let [child-row (render-child-row child-row-opts table-state row-data table-id)]
    [(add-expand-button child-row-opts tr table-state row-data table-id) child-row]))

(defn render-td
  [td-render-fn table-id headers row k]
  (with-meta (let [cell (td-render-fn row k)]
               (if (and (vector? cell) (= :td (first cell)))
                 cell
                 [:td cell]))
             {:key [row k table-id]}))

(defn render-thead
  [{:keys [child-row-opts headers sortable-columns sort-image-base table-id]} table-state]
  [:thead>tr
   (let [headers (if child-row-opts
                   (if (= :left (:expand-button-alignment child-row-opts))
                     (into [["expand-buttons" ""]] headers)
                     (conj headers ["expand-buttons" ""]))
                   headers)]   ;; Add extra column for expand button if there are child rows
     (doall
      (for [[col-id title] headers]
        (with-meta
          (if (some #{col-id} sortable-columns)

            [:th {:style    {:cursor "pointer"}
                  :on-click #(update-sort! col-id table-state)}
             title [sort-indicator col-id @table-state sort-image-base]]

            [:th title])
          {:key [col-id table-id]}))))])

(defn render-tbody
  [{:keys [headers rows filterable-columns td-render-fn table-id child-row-opts]
    :or   {td-render-fn (fn [row k]
                          (get row k))}}
   table-state]
  [:tbody
   (doall
    (->>
     (for [row (cond->> rows
                 (seq filterable-columns) (filter #(filter-row (:filter-string @table-state) filterable-columns %))
                 (:sort-columns @table-state) (do-sort (:sort-columns @table-state)))]
       (let [tr (with-meta (into [:tr]
                                 (for [[k _] headers]
                                   (render-td td-render-fn table-id headers row k)))
                  {:key [row table-id]})]
         (if (:child-row-render-fn child-row-opts)
           (row-with-child-row child-row-opts table-id table-state tr row)
           [tr])))
     (mapcat identity)))])

(defn data-table

  "Reagent component for a sortable/filterable table.
   The basic model is that the rows are defined by a seq of maps. Each map contains keys whose values correspond
   to the data which will be shown in the table.

   Takes a single arg which is a map, uses the following keys from that map:

   `:headers`            - A seq of `[col-id text]` where `col-id` is the key looked up in the row-maps, and `text` is the column heading
   `:rows`               - A seq of maps which make provide the table's data

   `:td-render-fn`       - A fn of two args, row and col-id which can return a reagent td element or just the content of it.
   `:child-row-render-fn`- A fn of one arg, a row which, if supplied, can be used to emit a child row after
   `:child-row-opts`     - A map with keys:
       `:child-row-render-fn`     - A fn of one arg, a row which, if supplied, can be used to emit a child row after each row.
                                  - The top level element should be a valid child of `<tr>` i.e `<th>` or `<td>`
                                  - fn should return falsey if a child row shouldn't be rendered this row
       `:expand-button-alignment` - `:left` or `:right` (default: `:right`)
       `:expanded-class`      - (optional) The CSS class to assign to expanded rows. Defaults to \"expanded\"
       `:collapsed-class`     - (optional) The CSS class to assign to collapsed rows. Defaults to \"collapsed\"

   `:sortable-columns`   - A seq of `col-id` which dictates which columns will be sortable
   `:filterable-columns` - A seq of `col-id` which dictates which columns will be filterable
   `:filter-label`       - A string used as a label for the filter input. Defaults to: \"Filter by: <col-1-name>, <col-2-name>...\"

   `:filter-string`      - A string to pre-populate the filter input
   `:sort-columns`       - A seq of `[col-id reverse-order?]` pairs which can specify the inital filtering
   `:sort-image-base`    - Where to find the files `sort_asc.png` &c. Default is `/img/`

   `:table-id`           - The value to use as the HTML `id` attribute for the table.  Must be unique if there are multiple tables shown
   `:table-class`        - The value used for the `class` attribute of the table
                           Defaults to `table table-striped table-bordered` which is OK for Bootstrap

   `:table-state-change-fn` - Optionally provide a one-arg fn which is called whenever the state of the table (sorting/filtering) changes
                              This is useful if some other part of your app needs to know about the sorting/filtering (saving user prefs, etc)"


  [{:keys [sortable-columns filter-string sort-columns table-state-change-fn table-class table-id sort-image-base child-row-opts]
    :or   {table-class     "table table-striped table-bordered"
           table-id        ""
           sort-image-base "/img/"}}]

  (let [table-state (reagent/atom {:filter-string (or filter-string "")
                                   :sort-columns  (or sort-columns
                                                      (map (fn [col] [col false]) sortable-columns))})]

    (when table-state-change-fn
      (add-watch table-state :blah
                 (fn [_ _ _ new]
                   (table-state-change-fn new))))

    (fn [{:keys [headers filterable-columns filter-label] :as opts}]
      (let [opts-with-defaults (merge opts {:sort-image-base sort-image-base})]
        [:div
         (when (seq filterable-columns)
           [:label (or filter-label
                       (str "Filter by " (s/join ", " (map (into {} headers) filterable-columns)) ":"))
            [:input {:style         {:margin-left :8px}
                     :default-value (:filter-string @table-state)
                     :on-change     #(swap! table-state assoc :filter-string (-> % .-target .-value))}]])

         [:table
          {:id table-id :class table-class :cell-spacing "0" :width "100%"}

          (render-thead opts-with-defaults table-state)
          (render-tbody opts-with-defaults table-state)]]))))
