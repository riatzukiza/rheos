(ns rheos.backend.domain.dependency-admission
  "Pure validation boundary for resolved workflow-owned admission policy."
  (:require [rheos.backend.law.dependency-admission :as law]
            [rheos.backend.shape.dependency-admission :as shape]
            [rheos.backend.domain.relationships :as relationships]))

(defn policy-errors [fsm]
  (when (contains? fsm :dependency-admission)
    (if (shape/valid-policy? (:dependency-admission fsm))
      (law/policy-state-errors fsm)
      [{:kind :malformed-dependency-policy}])))

(defn decide
  "Judge the complete selected graph and every reachable dependency predecessor
   at a workflow-owned guarded target. Parent/epic edges are graph constraints,
   never substitutes for predecessor completion. No effects or caller status list."
  ([fsm snapshot uuid target] (decide fsm snapshot uuid target {}))
  ([fsm snapshot uuid target opts]
  (let [errors (policy-errors fsm)
        policy (:dependency-admission fsm)]
    (cond
      (seq errors) {:allowed? false :errors errors}
      (not (some #{target} (:guarded-targets policy))) {:allowed? true}
      :else
      (let [graph (relationships/inspect-graph snapshot opts)]
        (if-not (:ok? graph)
          {:allowed? false :errors (:errors graph)}
          (let [by-id (into {} (map (juxt :uuid identity)) (:tasks graph))
                subject (get by-id uuid)]
            (if-not subject
              {:allowed? false :errors [{:kind :task-not-found :uuid uuid}]}
              (loop [pending (into (sorted-set) (:dependency subject)) seen #{} blockers []]
                (if-let [id (first pending)]
                  (let [card (get by-id id)
                        unfinished? (not (some #{(:status card)} (:successful-states policy)))
                        next-seen (conj seen id)]
                    (recur (into (disj pending id)
                                 (remove next-seen (:dependency card)))
                           next-seen
                           (cond-> blockers unfinished?
                             (conj {:kind :unfinished-predecessor :uuid uuid
                                    :target id :status (:status card)}))))
                  (if (seq blockers)
                    {:allowed? false :errors (vec (sort-by :target blockers))}
                    {:allowed? true})))))))))))
