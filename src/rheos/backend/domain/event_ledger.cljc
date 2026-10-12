(ns rheos.backend.domain.event-ledger
  "Read-only identity admission over all physical rows, not full event-schema admission.")

(defn check-text [text]
  ;; RED: there is no whole-row/uniqueness checker in the qualified predecessor.
  {:ok false :record-count (count text) :errors [{:row 1 :code :not-implemented}]})
