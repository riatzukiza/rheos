(ns rheos.backend.domain.dependency-admission
  "Pure validation boundary for resolved workflow-owned admission policy."
  (:require [rheos.backend.law.dependency-admission :as law]
            [rheos.backend.shape.dependency-admission :as shape]))

(defn policy-errors [fsm]
  (when (contains? fsm :dependency-admission)
    (if (shape/valid-policy? (:dependency-admission fsm))
      (law/policy-state-errors fsm)
      [{:kind :malformed-dependency-policy}])))
