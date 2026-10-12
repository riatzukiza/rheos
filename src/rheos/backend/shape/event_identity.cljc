(ns rheos.backend.shape.event-identity
  "Deterministic identity composition; runtime entropy stays at the producer edge."
  (:require [rheos.backend.law.event-identity :as law]))

(defn generated-id [board-id task-id entropy]
  (let [input {:board-id board-id :task-id task-id :entropy entropy}]
    (when-not (law/generation-input? input)
      (throw (ex-info "Invalid event identity components" {:kind :refused :input input})))
    (str board-id "-" task-id "-" entropy)))
