(ns rheos.backend.domain.relationships
  "Pure relationship normalization and complete-snapshot admission.

   A snapshot is the caller's complete selected-project projection, retaining
   exact :uuid, :type and raw relationship values. This namespace performs no
   loading, writing, status admission, revision reservation or event effects.
   The writer must acquire its publication reservation and re-read that complete
   projection before using a decision; a previously valid result is no lock."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [rheos.backend.law.frontmatter :as frontmatter]
            [rheos.backend.law.relationships :as law]
            [rheos.backend.shape.relationships :as shape]))

(defn- failure [errors]
  {:ok? false :errors (vec errors)})

(defn- malformed [field value]
  {:kind :malformed-relationship :field field :value value})

(defn- singular [field value]
  (cond
    (law/blank? value) {:ok? true}
    (law/reference? value) {:ok? true :value value}
    :else (failure [(malformed field value)])))

(defn- dependencies [value]
  (cond
    (law/blank? value) {:ok? true}
    (not (or (string? value) (vector? value)))
    (failure [(malformed :dependency value)])
    :else
    (let [items (if (string? value)
                  (if (str/includes? value ",")
                    ;; Whitespace around CSV separators is representation syntax.
                    ;; A singular string or vector member is an exact identity.
                    (mapv law/trim-csv-member (str/split value #"," -1))
                    [value])
                  value)
          duplicates (->> (frequencies items)
                          (keep (fn [[id n]] (when (> n 1) id))))]
      (cond
        (not-every? law/reference? items)
        (failure [(malformed :dependency value)])
        (seq duplicates)
        (failure [{:kind :duplicate-dependency :field :dependency
                   :targets (vec (sort duplicates))}])
        (empty? items) {:ok? true}
        :else {:ok? true :value (vec (sort items))}))))

(defn normalize
  "Normalize a relationship-only map. Missing keys remain missing; nil/blank
   singular values and nil/blank/empty-vector dependencies remove that field.
   Repeated or malformed dependency members are errors, never silently dropped."
  [input]
  (cond
    (not (map? input)) (failure [{:kind :malformed-relationship-input}])
    (seq (set/difference (set (keys input)) law/fields))
    (failure [{:kind :unknown-relationship-field}])
    :else
    (let [results (for [field [:parent :epic :dependency]
                        :when (contains? input field)]
                    [field (if (= field :dependency)
                             (dependencies (get input field))
                             (singular field (get input field)))])
          errors (vec (mapcat (comp :errors second) results))
          value (into {} (keep (fn [[field result]]
                                (when (contains? result :value)
                                  [field (:value result)]))) results)]
      (if (seq errors)
        (failure errors)
        {:ok? true :value value}))))

(defn- error-order [error]
  ;; Keep distinct diagnostic payloads ordered when the leading keys tie.
  [(str (:uuid error)) (str (:kind error)) (str (:field error))
   (str (:target error)) (str (:graph error)) (pr-str error)])

(defn- ordered-failure [errors]
  (failure (sort-by error-order errors)))

(defn- normalize-card [card]
  (let [result (normalize (select-keys card law/fields))]
    (if (:ok? result)
      {:ok? true :value (merge (apply dissoc card law/fields) (:value result))}
      (failure (map #(assoc % :uuid (:uuid card)) (:errors result))))))

(defn- card-edges [card]
  (concat (when-let [target (:parent card)] [[:parent target]])
          (when-let [target (:epic card)] [[:epic target]])
          (map (fn [target] [:dependency target]) (:dependency card))))

(defn- reference-errors [cards by-uuid]
  (for [card cards
        [field target] (card-edges card)
        :let [target-card (get by-uuid target)
              kind (cond
                     (= target (:uuid card)) :self-reference
                     (nil? target-card) :missing-reference
                     (and (= field :epic) (not= "epic" (:type target-card)))
                     :invalid-epic-target)]
        :when kind]
    {:kind kind :uuid (:uuid card) :field field :target target}))

(defn- cyclic-remainder
  "Kahn elimination, without recursive traversal. Remaining nodes prove a
   cycle exists; the remainder may also include nodes downstream of the cycle
   and is deliberately not labeled as its exact membership or a path."
  [adjacency]
  (let [incoming (reduce-kv
                  (fn [counts _ targets]
                    (reduce #(update %1 %2 inc) counts targets))
                  (zipmap (keys adjacency) (repeat 0)) adjacency)]
    (loop [counts incoming
           ready (into (sorted-set) (keep (fn [[id n]] (when (zero? n) id))) incoming)]
      (if-let [id (first ready)]
        (let [remaining (dissoc counts id)
              next-counts (reduce #(update %1 %2 dec) remaining (get adjacency id))
              newly-ready (filter #(zero? (get next-counts %)) (get adjacency id))]
          (recur next-counts (into (disj ready id) newly-ready)))
        (vec (sort (keys counts)))))))

(defn- cycle-error [cards field graph]
  (let [adjacency (into {} (map (fn [card]
                                [(:uuid card)
                                 (if (= field :parent)
                                   (if-let [id (:parent card)] [id] [])
                                   (or (:dependency card) []))])) cards)
        remaining (cyclic-remainder adjacency)]
    (when (seq remaining)
      {:kind :cycle :graph graph :remaining remaining})))

(defn- nearest-parent-epic [card by-uuid]
  (loop [id (:parent card) seen #{}]
    (when (and id (not (contains? seen id)))
      (let [parent (get by-uuid id)]
        (cond
          (= "epic" (:type parent)) id
          (:epic parent) (:epic parent)
          :else (recur (:parent parent) (conj seen id)))))))

(defn- membership-errors [cards by-uuid]
  (for [card cards
        :let [epic (:epic card)
              inherited (nearest-parent-epic card by-uuid)]
        :when (and epic inherited (not= epic inherited))]
    {:kind :conflicting-epic :uuid (:uuid card) :field :epic
     :target epic :parent-epic inherited}))

(defn inspect-graph
  "Classify a complete raw snapshot without repairing it. Successful inspection
   returns a normalized data projection; a failure returns no admitted tasks.
   Parent edges and dependency edges each point from card to referenced card
   and are independently acyclic. Epic membership is a separate constraint.
   Missing :type means a legacy ordinary task, never an inferred epic."
  [snapshot]
  (if-not (and (vector? snapshot) (every? map? snapshot))
    (failure [{:kind :malformed-snapshot}])
    (let [invalid (for [card snapshot :when (not (law/reference? (:uuid card)))]
                    {:kind :malformed-uuid :uuid (:uuid card)})
          type-errors (for [card snapshot
                            :when (and (contains? card :type)
                                       (not (contains? #{"task" "epic"} (:type card))))]
                        {:kind :malformed-card-type :uuid (:uuid card)})
          duplicate-errors (for [[id n] (frequencies (map :uuid snapshot))
                                 :when (and (law/reference? id) (> n 1))]
                             {:kind :ambiguous-uuid :uuid id})
          normalized (mapv normalize-card snapshot)
          initial-errors (concat invalid type-errors duplicate-errors
                                 (mapcat :errors normalized))]
      (if (seq initial-errors)
        (ordered-failure initial-errors)
        (let [cards (vec (sort-by :uuid (mapv :value normalized)))
              by-uuid (into {} (map (juxt :uuid identity)) cards)
              ref-errors (reference-errors cards by-uuid)]
          (if (seq ref-errors)
            (ordered-failure ref-errors)
            (let [errors (concat (keep identity [(cycle-error cards :parent :parent)
                                                 (cycle-error cards :dependency :dependency)])
                                 (membership-errors cards by-uuid))]
              (if (seq errors)
                (ordered-failure errors)
                {:ok? true :tasks cards}))))))))

(defn relationship-update? [updates]
  (boolean (seq (set/intersection (set (keys updates)) law/fields))))

(defn- key-errors [updates]
  (let [allowed (set/union frontmatter/mutable-keys law/fields)]
    (for [field (sort (keys updates)) :when (not (contains? allowed field))]
      {:kind (if (contains? frontmatter/forbidden-keys field)
               :protected-field :unknown-field)
       :field field})))

(defn disallowed-update-keys
  "The same closed authority used by the writer, exposed for public diagnostics."
  [updates]
  (mapv :field (key-errors updates)))

(defn admit-update
  "Admit one complete mixed batch against a complete post-update graph.

   Descriptive-only updates retain the existing closed-key authority and do
   not reinterpret malformed inherited relationships. Relationship batches
   validate the whole proposed graph, including unchanged cards. A true semantic
   no-op has empty :changes/:updates and leaves the original task untouched.
   Refusals never return a task, updates or a partially admitted subset."
  [snapshot uuid updates]
  (cond
    (not (shape/valid-mutation? {:uuid uuid :updates updates}))
    (failure [{:kind :malformed-mutation}])
    (law/blank? uuid) (failure [{:kind :malformed-uuid :uuid uuid}])
    (empty? updates) (failure [{:kind :empty-update}])
    (not (and (vector? snapshot) (every? map? snapshot)))
    (failure [{:kind :malformed-snapshot}])
    :else
    (let [bad-keys (key-errors updates)
          matches (filterv #(= uuid (:uuid %)) snapshot)]
      (cond
        (seq bad-keys) (ordered-failure bad-keys)
        (empty? matches) (failure [{:kind :task-not-found :uuid uuid}])
        (> (count matches) 1) (failure [{:kind :ambiguous-uuid :uuid uuid}])
        (not (relationship-update? updates))
        {:ok? true :relationship-change? false
         :task (merge (first matches) updates) :updates updates}
        :else
        (let [before (first matches)
              proposed (merge before updates)
              snapshot-after (mapv #(if (= uuid (:uuid %)) proposed %) snapshot)
              graph (inspect-graph snapshot-after)]
          (if-not (:ok? graph)
            graph
            (let [after (first (filter #(= uuid (:uuid %)) (:tasks graph)))
                  prior-relations (normalize (select-keys before law/fields))
                  old-value (fn [field]
                              (if (and (:ok? prior-relations) (law/fields field))
                                (get (:value prior-relations) field)
                                (get before field)))
                  changes (->> (sort (keys updates))
                               (keep (fn [field]
                                       (let [old (old-value field) new (get after field)]
                                         (when (not= old new)
                                           {:field field :old-value old :new-value new}))))
                               vec)
                  noop? (and (:ok? prior-relations) (empty? changes))]
              {:ok? true :relationship-change? true :noop? noop?
               :task (if noop? before after)
               :relationships (select-keys after law/fields)
               :changes changes
               :updates (into {} (map (juxt :field :new-value)) changes)})))))))
