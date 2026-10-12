(ns rheos.backend.shape.event-identity
  "Deterministic identity composition; runtime entropy stays at the producer edge.")

(defn generated-id [board-id task-id entropy]
  ;; RED: the reviewed constructor is not implemented yet.
  (throw (ex-info "Event ID constructor not implemented"
                  {:kind :refused :board-id board-id :task-id task-id :entropy entropy})))
