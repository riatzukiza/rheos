(ns rheos.backend.domain.event-ledger
  "Read-only identity admission over all physical rows, not full event-schema admission."
  (:require [rheos.backend.law.event-identity :as law]
            [rheos.backend.shape.ledger-text :as shape]))

(defn- admit-row [{:keys [seen] :as result} {:keys [row record error]}]
  (let [event-id (:event/id record)
        issue (cond
                error {:row row :code error}
                (not (map? record)) {:row row :code :non-map-record}
                (not (law/opaque-id? event-id)) {:row row :code :invalid-event-id}
                (contains? seen event-id) {:row row :code :duplicate-event-id
                                          :event-id event-id :first-row (get seen event-id)})]
    (if issue
      (update result :errors conj issue)
      (assoc-in result [:seen event-id] row))))

(defn check-text [text]
  (if-not (string? text)
    {:ok false :record-count 0 :errors [{:code :invalid-ledger-text}]}
    (let [rows (shape/decode-text text)
          errors (if (empty? rows)
                   [{:code :empty-ledger}]
                   (:errors (reduce admit-row {:seen {} :errors []} rows)))]
      {:ok (empty? errors) :record-count (count rows) :errors errors})))
