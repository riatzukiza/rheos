(ns rheos.backend.infra.task-edit
  "Ledger-backed edits to task frontmatter and comments — the write path for the
   decisions in [[rheos.backend.domain.task-edit]].

   These are the non-status writes. Like
   [[rheos.backend.infra.transition/move-task!]], every successful mutation is
   recorded in the project's event ledger.

   Callers (HTTP handlers and CLI commands) supply the resolved project and task;
   this namespace owns the file write + event emission chokepoint."
  (:require ["node:fs/promises" :as fsp]
            [rheos.backend.domain.events :as events]
            [rheos.backend.domain.task-edit :as task-edit]
            [rheos.backend.domain.relationships :as relationships]
            [rheos.backend.law.relationships :as relationship-law]
            [rheos.backend.shape.content-parser :as content-shape]
            [rheos.backend.infra.content-parser :as content-parser]
            [rheos.backend.infra.ledger :as ledger]
            [rheos.backend.infra.task-store :as tasks]
            [rheos.backend.infra.publication :as publication]
            [rheos.backend.infra.watcher :as watcher]))

(defn- ^:async update-reserved!
  "Apply `updates` (a map of key -> value) to a task's YAML frontmatter, write the
   file back, and emit one ledger event per changed key. Returns `:ok true`,
   task reference data, and accepted `:frontmatter`. For ordinary edits, `:task`
   retains the caller's map; consumers use `:frontmatter` or a fresh native read.

   An empty `updates` is a no-op carrying `:noop true`; it writes nothing, so the
   file never changes without the ledger saying why."
  [{:keys [project task updates source]}]
  (if (empty? updates)
    ;; No updates is a no-op, not a write. Rewriting the file would stamp a fresh
    ;; write-id and wake the watcher while emitting no event — a file mutation the
    ;; ledger never saw. The HTTP handler already rejects this; CLI and MCP
    ;; callers reach here directly.
    {:ok true :task task :frontmatter (:frontmatter task) :noop true}
    (let [updates (into {} (map (fn [[k v]] [(keyword k) v])) (content-shape/checked-updates updates))
          loaded (await (tasks/load-tasks project))
          current (filterv #(and (= (:uuid task) (:uuid %))
                                (= (:source-path task) (:source-path %))) loaded)
          _ (when-not (= 1 (count current))
              (publication/conflict! "Task identity or selected source changed before edit"
                                     {:uuid (:uuid task) :source-path (:source-path task)}))
          decision (relationships/admit-update (tasks/relationship-snapshot loaded) (:uuid task) updates)
          _ (when-not (:ok? decision)
              (throw (ex-info "Relationship/frontmatter admission refused"
                              {:kind :refused :errors (:errors decision) :uuid (:uuid task)})))
          relationship-batch? (:relationship-change? decision)
          updates (if relationship-batch? (:updates decision) updates)
          task-path (:source-path task)
          raw (await (.readFile fsp task-path "utf8"))
          write-id (events/generate-write-id)
          plan (try (let [old-frontmatter (:frontmatter (content-parser/parse-task-content raw))
                          removals (set (keep (fn [[k v]] (when (and (relationship-law/fields k) (nil? v)) k)) updates))
                          rendered-updates (apply dissoc updates removals)
                          new-raw (-> raw
                                      (content-parser/remove-frontmatter-keys removals)
                                      (content-parser/update-frontmatter-keys rendered-updates)
                                      (content-parser/inject-write-id write-id))
                          new-frontmatter (:frontmatter (content-parser/parse-task-content new-raw))]
                      (cond-> (task-edit/plan-frontmatter-update old-frontmatter new-frontmatter new-raw updates)
                        relationship-batch?
                        (assoc :changes (mapv (fn [{:keys [field old-value new-value]}]
                                                {:key (name field) :old-value old-value :new-value new-value})
                                              (:changes decision)))))
                    (catch :default err
                      (if (and (= :refused (:kind (ex-data err)))
                               (contains? #{:uuid :slug :title :priority :status} (:field (ex-data err))))
                        (throw (ex-info (str "Refused card source " task-path ": " (.-message err))
                                        (assoc (ex-data err) :source-path task-path
                                               :diagnostic (.-message err))
                                        err))
                        (throw err))))
          ledger (ledger/get-ledger (:tasks-dir project))
          src (or source "cli")]
      (if (and relationship-batch? (:noop? decision))
        {:ok true :task (first current) :frontmatter (:frontmatter (first current)) :noop true}
        (do
          (when-not (= (tasks/source-revisions loaded)
                       (tasks/source-revisions (await (tasks/load-tasks project))))
            (publication/conflict! "Complete selected source changed before relationship edit"
                                   {:uuid (:uuid task) :source-path task-path}))
          (await (publication/file-and-event!
                  {:source-path task-path :write-id write-id}
                  (fn []
                    (watcher/register-cli-event! write-id (:uuid task))
                    (.writeFile fsp task-path (:raw plan) "utf8"))
                  (^:async fn []
                    (doseq [{:keys [key old-value new-value]} (:changes plan)]
                      (await (events/emit-frontmatter-change! ledger (:id project) (:uuid task)
                                                              key old-value new-value write-id src))))))
          {:ok true :task task :frontmatter (:frontmatter plan)})))))

(defn ^:async update-frontmatter!
  "The canonical edit boundary, including lower-level callers. Identity/status/
   provenance keys are refused; relationship batches use the complete graph.
   Empty requests and semantic relationship no-ops have no file/event effect."
  [{:keys [project updates] :as request}]
  (if (empty? updates)
    (await (update-reserved! request))
    (await (publication/with-reservation! project #(update-reserved! request)))))

(defn- ^:async append-comment-reserved!
  "Append `text` to a qualified task source and emit a comment event.
   Invalid decoded frontmatter is refused with source diagnostics before any
   write-id registration, file write or ledger event."
  [{:keys [project task text source]}]
  (let [task-path (:source-path task)
        raw (await (.readFile fsp task-path "utf8"))
        comment-raw (try
                      (task-edit/plan-comment raw (content-parser/parse-task-content raw) text)
                      (catch :default err
                        (if (= :refused (:kind (ex-data err)))
                          (throw (ex-info (str "Refused card source " task-path ": " (.-message err))
                                          (assoc (ex-data err) :source-path task-path
                                                 :diagnostic (.-message err))
                                          err))
                          (throw err))))
        write-id (events/generate-write-id)
        new-raw (content-parser/inject-write-id comment-raw write-id)
        ledger (ledger/get-ledger (:tasks-dir project))]
    (when-not (= raw (await (.readFile fsp task-path "utf8")))
      (publication/conflict! "Card source changed before comment publication"
                             {:uuid (:uuid task) :source-path task-path}))
    (await (publication/file-and-event!
            {:source-path task-path :write-id write-id}
            (fn []
              (watcher/register-cli-event! write-id (:uuid task))
              (.writeFile fsp task-path new-raw "utf8"))
            (fn [] (events/emit-comment! ledger (:id project) (:uuid task) text write-id (or source "cli")))))
    {:ok true :task task :text text}))

(defn ^:async append-comment!
  "Comments share the canonical reservation so they cannot overwrite a
   concurrent relationship/status write. Legacy source validation is retained;
   partial file/event effects use the same truthful publication diagnostics."
  [{:keys [project] :as request}]
  (await (publication/with-reservation! project #(append-comment-reserved! request))))
