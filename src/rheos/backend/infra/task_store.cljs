(ns rheos.backend.infra.task-store
  "Task loading from projected Markdown files with YAML frontmatter parsing."
  (:require ["node:fs/promises" :as fsp]
            ["node:crypto" :as crypto]
            ["node:path" :as path]
            [clojure.string :as str]
            [rheos.backend.infra.projects :as projects]
            [rheos.backend.law.frontmatter :as law-frontmatter]
            [rheos.backend.infra.content-parser :as content-parser]
            [rheos.backend.domain.relationships :as relationships]
            [rheos.backend.law.relationships :as relationship-law]
            [rheos.backend.shape.kanban :as shape]))

(def status-index
  (into {} (map-indexed (fn [i s] [s i]) shape/StatusOrder)))

(defn- normalize-labels [labels tags]
  (let [raw (or labels tags [])
        items (cond
                (string? raw) (str/split raw #",")
                (vector? raw) raw
                :else [])]
    (vec (distinct (filter seq (mapv #(str/trim (str %)) items))))))

(defn- normalize-status [status]
  (case (-> (or status "incoming") str/lower-case str/trim)
    "pending" "incoming"
    "completed" "done"
    (-> (or status "incoming") str/lower-case str/trim)))

(defn- ^:async parse-task-file [file-path _tasks-dir]
  (try
    (let [raw (await (.readFile fsp file-path "utf8"))
          {:keys [frontmatter content]} (content-parser/parse-frontmatter raw)
          _ (law-frontmatter/assert-task-frontmatter-shape frontmatter)
          title (or (:title frontmatter) (path/basename file-path ".md"))
          priority (-> (or (:priority frontmatter) "P3") str/upper-case str/trim)
          labels (normalize-labels (:labels frontmatter) (:tags frontmatter))
          uuid (or (:uuid frontmatter)
                   (:slug frontmatter)
                   (-> title str/lower-case (str/replace #"[^a-z0-9]+" "-")))
          status (normalize-status (:status frontmatter))
          created-at (or (:created_at frontmatter)
                         (:createdAt frontmatter)
                         (.toISOString (new js/Date)))
          relationship-input (select-keys frontmatter relationship-law/fields)
          normalized (relationships/normalize relationship-input)]
      (merge {:uuid uuid
       :title title
       :slug (or (:slug frontmatter) uuid)
       :status status
       :priority priority
       :labels labels
       :created-at created-at
       :content content
       :source-path file-path
       :source-revision (.digest (.update (.createHash crypto "sha256") raw "utf8") "hex")
       :frontmatter frontmatter}
             (select-keys frontmatter [:type])
             (if (:ok? normalized)
               (:value normalized)
               (assoc relationship-input :relationship-errors (:errors normalized)))))
    (catch :default err
      (let [diagnostic (or (.-message err) (str err))]
        (throw (ex-info (str "Refused card source " file-path ": " diagnostic)
                        {:kind :refused :source-path file-path :diagnostic diagnostic}
                        err))))))

(defn- ^:async entry-kind
  "`:file`, `:dir`, `:link`, or nil — read with `lstat`, so a symlink reports as
   a link rather than as whatever it points at."
  [full-path]
  (try
    ;; ^js: `isSymbolicLink` is not in the externs shadow infers from, so the
    ;; call compiles to a munged name under :advanced without the hint.
    (let [^js st (await (.lstat fsp full-path))]
      (cond
        (.isSymbolicLink st) :link
        (.isDirectory st) :dir
        (.isFile st) :file
        :else nil))
    (catch :default error
      (throw (ex-info "Unavailable selected card entry"
                      {:kind :refused :cause :incomplete-projection
                       :source-path full-path :diagnostic (.-message error)} error)))))

(declare collect-entry)

(defn- ^:async collect-below
  "Markdown files below `dir`, one level at a time."
  [dir]
  (try
    (let [names (await (.readdir fsp dir))
          nested (await
                  (js/Promise.all
                   (clj->js
                    (mapv #(collect-entry (path/join dir %)) names))))]
      (vec (apply concat nested)))
    (catch :default err
      (throw (ex-info "Unavailable selected card directory"
                      {:kind :refused :cause :incomplete-projection
                       :source-path dir :diagnostic (.-message err)} err)))))

(defn- ^:async collect-entry
  "One discovered entry, classified with `lstat`.

   Symlinks are **skipped, not followed**. The walk enforces no containment of
   its own — that check happens in `shape.config` against the *configured*
   projection roots — so a link planted underneath a projected root would
   otherwise pull cards in from anywhere the process can read, and a link back
   to an ancestor would recurse until the process died. Neither is hypothetical:
   `stat` follows links, and this walk used it."
  [entry-path]
  (let [kind (await (entry-kind entry-path))]
    (case kind
      :file (if (str/ends-with? entry-path ".md") [entry-path] [])
      :dir (await (collect-below entry-path))
      [])))

(defn- ^:async collect-markdown-files
  "Markdown files at or under a configured projection root.

   The root itself is resolved with `stat`, so it may legitimately be a symlink —
   `shape.config` has already resolved it and checked it stays inside the task
   root. Everything discovered *below* it goes through [[collect-entry]], which
   does not follow links."
  [entry-path]
  (try
    (let [^js stat (await (.stat fsp entry-path))]
      (cond
      (.isFile stat)
      (if (str/ends-with? entry-path ".md") [entry-path] [])

      (.isDirectory stat)
      (await (collect-below entry-path))

      :else (throw (ex-info "Unsupported selected projection root"
                            {:kind :refused :cause :incomplete-projection
                             :source-path entry-path}))))
    (catch :default err
      (throw (ex-info "Unavailable selected projection root"
                      {:kind :refused :cause :incomplete-projection
                       :source-path entry-path :diagnostic (.-message err)} err)))))

(defn relationship-snapshot
  "Use stored identity/relationships, not display fallback UUIDs. Loader aliases
   for status remain the canonical read spelling; raw frontmatter stays intact."
  [loaded]
  (mapv #(merge (:frontmatter %)
                {:status (:status %) :source-path (:source-path %)}) loaded))

(defn source-revisions [loaded]
  (into (sorted-map) (map (juxt :source-path :source-revision)) loaded))

(defn- task-sort-key [task]
  [(get status-index (:status task) 99)
   (case (:priority task) "P0" 0 "P1" 1 "P2" 2 "P3" 3 4)
   (str/lower-case (:title task))])

(defn- source->project
  "Normalize `load-tasks`' argument. A project map passes through; a tasks-dir
   string is resolved through the shared registry so a configured project keeps
   its `:card-projection`."
  [source]
  (if (map? source)
    source
    (or (projects/find-project-by-tasks-dir source)
        {:tasks-dir source})))

(defn ^:async load-tasks
  "Load materialized card projections for a project.

   `source` is either a project map or a bare tasks-dir string. A project with
   `:card-projection {:paths [...]}` scans only those resolved paths; a bare
   tasks-dir preserves recursive legacy discovery.

   A refused candidate rejects the load with its source path and diagnostic.
   Never report a successful partial board or invent frontmatter for that file;
   the caller can repair the source and retry. Files outside the configured
   projection and non-Markdown entries retain their discovery exclusions.

   The resolved tasks-dir is checked because getting it wrong used to be
   invisible: `readdir` throws on a bad argument, [[collect-markdown-files]]
   catches everything and returns `[]`, and the caller reads that as \"the board
   has no cards\". A CLI verb shipped with exactly that mistake and reported
   `unknown task` for cards that existed. Fail where the mistake is, not five
   frames later — accepting a project map here does not make an unusable one
   silent."
  [source]
  (when-not (or (map? source) (and (string? source) (seq source)))
    (throw (ex-info (str "load-tasks expects a project map or a tasks directory path, got: " (pr-str source))
                    {:kind :usage :source source})))
  (let [project (source->project source)
        tasks-dir (:tasks-dir project)
        _ (when-not (and (string? tasks-dir) (seq tasks-dir))
            (throw (ex-info (str "load-tasks resolved no tasks directory from: " (pr-str source))
                            {:kind :usage :source source :tasks-dir tasks-dir})))
        projection (:card-projection project)
        roots (if (and projection (contains? projection :paths))
                (:paths projection)
                [tasks-dir])
        nested (await (js/Promise.all
                       (clj->js (mapv collect-markdown-files roots))))
        files (vec (distinct (apply concat nested)))
        tasks (vec (await (js/Promise.all
                           (clj->js
                            (mapv #(parse-task-file % tasks-dir) files)))))]
    (vec (sort-by task-sort-key tasks))))
