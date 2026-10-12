(ns rheos.backend.infra.transition
  "The single, FSM-enforced, ledger-backed write path for status changes — the
   effects behind [[rheos.backend.domain.transition/decide-move]].

   Both the HTTP server and the CLI route status moves through [[move-task!]] so
   that no transition can bypass the FSM and every successful move is recorded in
   the event ledger."
  (:require [rheos.backend.domain.events :as events]
            [rheos.backend.domain.dependency-admission :as admission]
            [rheos.backend.domain.task-create :as task-create]
            [rheos.backend.domain.transition :as transition]
            [rheos.backend.infra.ledger :as ledger]
            [rheos.backend.infra.publication :as publication]
            [rheos.backend.infra.task-store :as tasks]
            [rheos.backend.infra.task-writeback :as writeback]
            [rheos.backend.infra.watcher :as watcher]
            [rheos.backend.law.fsm :as fsm]))

(defn- current-task! [loaded task]
  (let [matches (filterv #(= (:uuid task) (:uuid %)) loaded)]
    (when-not (and (= 1 (count matches))
                   (= (:source-path task) (:source-path (first matches)))
                   (= (:status task) (:status (first matches))))
      (publication/conflict! "Task identity, source or status changed before admission"
                             {:uuid (:uuid task) :source-path (:source-path task)}))
    (first matches)))

(defn- refusal [from to decision]
  {:ok false :kind :refused :from from :to to
   :reason (or (:reason decision) (str "Predecessor admission refused: " (pr-str (:errors decision))))
   :errors (:errors decision)})

(defn- decide [project task target loaded]
  (let [structural (transition/decide-move project (:status task) target loaded)
        predecessors (admission/decide (fsm/resolve-fsm {:fsm (:fsm project)})
                                      (tasks/relationship-snapshot loaded) (:uuid task) target
                                      {:card-types (task-create/card-types project)})]
    (if-not (:allowed? structural) structural
      (if-not (:allowed? predecessors) predecessors structural))))

(defn- ^:async move-reserved! [{:keys [project task new-status source]}]
  (let [from (:status task)
        loaded (await (tasks/load-tasks project))
        current (current-task! loaded task)]
    (if (= from new-status)
      {:ok true :task current :from from :to new-status :noop true}
      (let [decision (decide project current new-status loaded)]
        (if-not (:allowed? decision)
          (refusal from new-status decision)
          (let [gate (await (fsm/run-gate decision (or (:gate-cwd project) (js/process.cwd))))]
            (if-not (:allowed? gate)
              (refusal from new-status gate)
              (let [fresh (await (tasks/load-tasks project))
                    fresh-task (current-task! fresh current)
                    fresh-decision (decide project fresh-task new-status fresh)]
                (when-not (= (tasks/source-revisions loaded) (tasks/source-revisions fresh))
                  (publication/conflict! "Complete selected source revision changed during executable gate"
                                         {:uuid (:uuid task) :errors (:errors fresh-decision)}))
                (if-not (:allowed? fresh-decision)
                  (refusal from new-status fresh-decision)
                  (let [write-id (events/generate-write-id)
                        board-ledger (ledger/get-ledger (:tasks-dir project))
                        updated (await (publication/file-and-event!
                                        {:source-path (:source-path fresh-task) :write-id write-id}
                                        (fn []
                                          (watcher/register-cli-event! write-id (:uuid task))
                                          (writeback/write-task-status fresh-task (:tasks-dir project) new-status write-id))
                                        (fn [] (events/emit-status-change! board-ledger (:id project) (:uuid task)
                                                                          from new-status write-id (or source "cli")))))]
                    {:ok true :task updated :from from :to new-status}))))))))))

(defn ^:async move-task!
  "All public callers share the reservation, current scoped graph, original FSM
   and command gates, then a fresh source/graph check. Partial effects retain
   actual readback and never masquerade as successful admission."
  [{:keys [project task new-status] :as request}]
  (try
    (await (publication/with-reservation! project #(move-reserved! request)))
    (catch :default error
      (if (contains? #{:refused :conflict :partial-effect} (:kind (ex-data error)))
        (merge (ex-data error) {:ok false :reason (.-message error)
                               :from (:status task) :to new-status})
        (throw error)))))
