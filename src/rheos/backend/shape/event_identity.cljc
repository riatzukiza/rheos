(ns rheos.backend.shape.event-identity
  "Deterministic identity composition; runtime entropy stays at the producer edge."
  (:require [rheos.backend.law.event-identity :as law]))

(defn check-components!
  "Refuse known invalid emission components before an outer writer effect.
   Preserve nonblank opaque identities exactly; entropy stays producer-owned."
  [board-id task-id]
  (when-not (and (law/opaque-id? board-id) (law/opaque-id? task-id))
    (throw (ex-info "Invalid event identity components"
                    {:kind :refused :input {:board-id board-id :task-id task-id}}))))

(defn generated-id [board-id task-id entropy]
  (let [input {:board-id board-id :task-id task-id :entropy entropy}]
    (when-not (law/generation-input? input)
      (throw (ex-info "Invalid event identity components" {:kind :refused :input input})))
    (str board-id "-" task-id "-" entropy)))
