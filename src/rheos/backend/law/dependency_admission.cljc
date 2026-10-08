(ns rheos.backend.law.dependency-admission
  "Portable constraints for an already shape-validated workflow policy.")

(defn policy-state-errors
  "Known-state closure and uniqueness for a shape-validated declaration."
  [fsm]
  (when (contains? fsm :dependency-admission)
    (let [policy (:dependency-admission fsm)
          states (set (:states fsm))]
      (vec
         (for [field [:successful-states :guarded-targets]
               :let [values (get policy field)]
               error (concat
                      (when (not= (count values) (count (distinct values)))
                        [{:kind :duplicate-policy-state :field field}])
                      (for [state values :when (not (contains? states state))]
                        {:kind :unknown-policy-state :field field :state state}))]
           error)))))
