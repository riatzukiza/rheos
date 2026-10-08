(ns rheos.backend.infra.task-create
  "The single creation chokepoint for cards: place the file, write it exclusively,
   and record a `task-created` ledger event.

   Every decision made along the way belongs to
   [[rheos.backend.domain.task-create]] — identity, entry status, placement
   policy, the bytes of the file. What is left here is only effect: the directory
   probe, the exclusive write, the watcher registration that lets the resulting
   file event correlate back to this mutation, and the ledger append."
  (:require ["node:fs/promises" :as fsp]
            ["node:path" :as path]
            [clojure.string :as str]
            [rheos.backend.domain.events :as events]
            [rheos.backend.domain.task-create :as task-create]
            [rheos.backend.domain.relationships :as relationships]
            [rheos.backend.domain.dependency-admission :as admission]
            [rheos.backend.law.fsm :as fsm]
            [rheos.backend.law.frontmatter :as law-frontmatter]
            [rheos.backend.infra.content-parser :as content-parser]
            [rheos.backend.infra.ledger :as ledger]
            [rheos.backend.infra.publication :as publication]
            [rheos.backend.infra.task-store :as tasks]
            [rheos.backend.infra.watcher :as watcher]))

(defn- ^:async dir-exists? [dir-path]
  (try
    (.isDirectory (await (.stat fsp dir-path)))
    (catch :default _ false)))

(defn- within?
  "Is `candidate` inside `root` (or root itself)?"
  [root candidate]
  (or (= root candidate)
      (str/starts-with? candidate (str root path/sep))))

(defn ^:async resolve-card-dir
  "Where a new card of `card-type` belongs, in precedence order:

   1. an explicit `dir` (resolved against the project's tasks-dir);
   2. the project's `:card-dirs` config for this type;
   3. the conventional `<tasks-dir>/epics` or `<tasks-dir>/tasks`, when it exists;
   4. the tasks-dir itself.

   The result is put to
   [[rheos.backend.domain.task-create/check-card-dir!]], which refuses anything
   that escapes the task root or falls outside the project's card projection."
  [project card-type dir]
  (let [tasks-dir (:tasks-dir project)
        configured (get-in project [:card-dirs (keyword card-type)])
        conventional (get task-create/conventional-dirs card-type)
        resolved (cond
                   dir (path/resolve tasks-dir dir)
                   configured (path/resolve tasks-dir configured)
                   (and conventional
                        (await (dir-exists? (path/join tasks-dir conventional))))
                   (path/join tasks-dir conventional)
                   :else tasks-dir)]
    (task-create/check-card-dir! project resolved dir within?)))

(defn- resolve-card-path
  "The absolute path a new card is written to, refused unless it is a direct
   child of the already-checked `card-dir`.

   [[rheos.backend.domain.task-create/check-uuid!]] is what stops a crafted uuid
   from reaching the file name in the first place; this re-checks the path the
   name actually resolves to, so no future change to naming can quietly reopen
   the escape that `path/join` normalization would otherwise permit."
  [card-dir file-name]
  (let [file-path (path/resolve card-dir file-name)]
    (when-not (= card-dir (path/dirname file-path))
      (task-create/refuse! :refused
                           (str "card file name escapes its directory: " file-name)
                           {:dir card-dir :file-name file-name :path file-path}))
    file-path))

(defn- ^:async write-card-exclusive!
  [file-path raw]
  (try
    (await (.writeFile fsp file-path raw #js {:encoding "utf8" :flag "wx"}))
    (catch :default e
      (if (= "EEXIST" (.-code e))
        (task-create/refuse! :refused
                             (str "a card file already exists at " file-path)
                             {:path file-path :cause :create-conflict})
        (throw e)))))

(defn- ^:async create-reserved!
  "Create a card and record a `task-created` ledger event.

   Refuses, rather than guessing, when: the title is blank; the card type is
   unknown; the uuid is already taken; a named `:parent` does not exist; an
   explicit `:status` is not the FSM's initial state (pass `:force-status?` to
   override deliberately); or the target file already exists.

   Returns `{:ok true :uuid … :title … :status … :source-path … :card-type …}`."
  [{:keys [project title card-type parent epic dependency status priority points labels body
           dir uuid source force-status?]}]
  (when-not project
    (task-create/refuse! :not-found "unknown project" {}))
  (let [card-type (task-create/check-request! {:title title :card-type card-type})
        existing (await (tasks/load-tasks project))
        normalized (relationships/normalize {:parent parent :epic epic :dependency dependency})
        _ (when-not (:ok? normalized)
            (task-create/refuse! :refused "Malformed creation relationships" {:errors (:errors normalized)}))
        decision (task-create/decide-card {:project project :title title
                                           :card-type card-type :parent parent
                                           :status status :uuid uuid
                                           :force-status? force-status?
                                           :existing existing})
        card-uuid (:uuid decision)
        card-status (:status decision)
        proposed (conj (tasks/relationship-snapshot existing)
                       (merge {:uuid card-uuid :type card-type :status card-status} (:value normalized)))
        card-dir (await (resolve-card-dir project card-type dir))
        file-path (resolve-card-path
                   card-dir (task-create/card-file-name (:slug decision) card-uuid))
        _ (try
            (await (.lstat fsp file-path))
            (task-create/refuse! :refused (str "a card file already exists at " file-path)
                                {:path file-path :cause :create-conflict})
            (catch :default error
              (when-not (= "ENOENT" (.-code error)) (throw error))))
        graph (relationships/inspect-graph proposed)
        _ (when-not (:ok? graph)
            (task-create/refuse! :refused "Creation relationship graph refused" {:errors (:errors graph)}))
        predecessor-decision (admission/decide (fsm/resolve-fsm {:fsm (:fsm project)}) proposed card-uuid card-status)
        _ (when-not (:allowed? predecessor-decision)
            (task-create/refuse! :refused "Creation predecessor admission refused" {:errors (:errors predecessor-decision)}))
        write-id (events/generate-write-id)
        card (task-create/render-card {:uuid card-uuid
                                       :title title
                                       :status card-status
                                       :card-type card-type
                                       :priority priority
                                       :points points
                                       :labels (vec (or labels []))
                                       :parent (:parent (:value normalized))
                                       :epic (:epic (:value normalized))
                                       :dependency (:dependency (:value normalized))
                                       :category (path/basename card-dir)
                                       :write-id write-id
                                       :created-at (.toISOString (new js/Date))
                                       :body body})
        _ (try
            (law-frontmatter/assert-task-frontmatter-shape
              (:frontmatter (content-parser/parse-frontmatter (:raw card))))
            (catch :default error
              (throw (ex-info (str "Refused card source " file-path ": " (.-message error))
                              (assoc (ex-data error) :kind :refused :source-path file-path
                                     :diagnostic (.-message error))
                              error))))]
    (await (.mkdir fsp card-dir #js {:recursive true}))
    (when-not (= (tasks/source-revisions existing)
                 (tasks/source-revisions (await (tasks/load-tasks project))))
      (publication/conflict! "Complete selected source changed before creation" {:uuid card-uuid}))
    (await (publication/file-and-event!
            {:source-path file-path :write-id write-id}
            (fn []
              (watcher/register-cli-event! write-id card-uuid)
              (write-card-exclusive! file-path (:raw card)))
            (fn []
              (events/emit-task-created!
               (ledger/get-ledger (:tasks-dir project))
               (:id project) card-uuid
               (merge {:title title :card-type card-type :status card-status
                       :source-path file-path :body (:body card)} (:value normalized))
               write-id source))))
    {:ok true :uuid card-uuid :title title :status card-status
     :card-type card-type :parent (:parent (:value normalized))
     :epic (:epic (:value normalized)) :dependency (:dependency (:value normalized))
     :source-path file-path}))

(defn ^:async create-task!
  "All creation callers share the graph publication reservation and accepted
   relationships. Explicit force-status retains its existing initial-state
   override; it never waives the configured predecessor admission policy."
  [{:keys [project] :as request}]
  (when-not project (task-create/refuse! :not-found "unknown project" {}))
  (await (publication/with-reservation! project #(create-reserved! request))))
