(ns rheos.backend.infra.relationship-admission-test
  "Behavioral RED for the reviewed writer story, through the owning read/write paths.
   Private fixture files are input; native Rheos events seed the retained ledger."
  (:require ["node:fs/promises" :as fsp]
            ["node:os" :as os]
            ["node:path" :as path]
            ["yaml" :as yaml]
            [cljs.test :refer [deftest is testing]]
            [rheos.backend.domain.events :as events]
            [rheos.backend.infra.ledger :as ledger]
            [rheos.backend.infra.task-create :as create]
            [rheos.backend.infra.task-edit :as edit]
            [rheos.backend.infra.content-parser :as parser]
            [rheos.backend.infra.agent-tools :as agent-tools]
            [rheos.backend.infra.projects :as projects]
            [rheos.backend.infra.publication :as publication]
            [rheos.backend.infra.task-store :as tasks]
            [rheos.backend.law.fsm :as fsm]
            [rheos.backend.infra.transition :as transition]))

(defn- fixture-card
  "Render controlled fixture data, without a second relationship interpretation."
  [card]
  (str "---\n" (.stringify yaml (clj->js (merge {:title (:uuid card) :priority "P3"} card)))
       "---\n\n# Fixture\n\nRetained body.\n\n---\nRetained comment.\n"))

(defn- ^:async with-cards
  "Allocate and remove one private board; seed its history using Rheos itself."
  [cards f]
  (let [dir (await (.mkdtemp fsp (path/join (.tmpdir os) "rheos-relationship-admission-")))
        project {:id "relationship-admission-test" :tasks-dir dir :fsm "promethean"}
        ledger-path (path/join dir ".events" "ledger.edn")]
    (try
      (doseq [card cards]
        (await (.writeFile fsp (path/join dir (str (:uuid card) ".md")) (fixture-card card) "utf8")))
      (await (events/emit-comment! (ledger/get-ledger dir) (:id project) "subject"
                                   "Retained native fixture event" "fixture-seed" "test"))
      (await (f {:project project :dir dir :ledger-path ledger-path}))
      (finally
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(defn- ^:async assert-refused-move!
  "A refusal must preserve the complete card and the already seeded ledger bytes."
  [{:keys [project dir ledger-path]} from to]
  (let [source-path (path/join dir "subject.md")
        before (await (.readFile fsp source-path "utf8"))
        history (await (.readFile fsp ledger-path "utf8"))
        result (await (transition/move-task! {:project project
                                              :task {:uuid "subject" :status from :source-path source-path}
                                              :new-status to :source "test"}))]
    (is (false? (:ok result)) (str "Refuse guarded admission: " (:reason result)))
    (is (= before (await (.readFile fsp source-path "utf8"))) "Refusal preserves body, comments and frontmatter")
    (is (= history (await (.readFile fsp ledger-path "utf8"))) "Refusal appends no success event")))

(deftest ^:async canonical-loader-retains-accepted-relationship-facts
  (await
   (with-cards [{:uuid "epic" :type "epic" :status "done"}
                {:uuid "parent" :type "task" :status "done" :epic "epic"}
                {:uuid "subject" :type "task" :status "breakdown"
                 :parent "parent" :epic "epic" :dependency "parent"}]
     (^:async fn [{:keys [project]}]
       (let [subject (first (filter #(= "subject" (:uuid %)) (await (tasks/load-tasks project))))]
         (is (= "task" (:type subject)))
         (is (= "parent" (:parent subject)))
         (is (= "epic" (:epic subject)))
         (is (= ["parent"] (:dependency subject))))))))

(deftest ^:async incomplete-selected-projection-is-not-a-successful-partial-read
  (await
   (with-cards [{:uuid "subject" :status "breakdown"}]
     (^:async fn [{:keys [project dir]}]
       (let [selected (assoc project :card-projection
                             {:paths [(path/join dir "subject.md") (path/join dir "unavailable")]})
             outcome (try
                       {:tasks (await (tasks/load-tasks selected))}
                       (catch :default error {:error error}))]
         (is (some? (:error outcome)) "A missing selected root must not produce an apparently complete board"))))))

(deftest ^:async each-reviewed-implementation-boundary-refuses-unfinished-predecessors
  (doseq [[from to] [["breakdown" "ready"] ["ready" "todo"] ["todo" "in_progress"]]
          status ["incoming" "ready" "in_progress" "rejected" "archived" "unknown"]]
    (testing (str from " -> " to ", predecessor=" status)
      (await (with-cards [{:uuid "subject" :status from :dependency "predecessor"}
                         {:uuid "predecessor" :status status}]
               (^:async fn [fixture] (await (assert-refused-move! fixture from to))))))))

(deftest ^:async guarded-admission-checks-complete-reachable-graph
  (doseq [[label cards]
          [["missing predecessor" [{:uuid "subject" :status "breakdown" :dependency "missing"}]]
           ["malformed relationship" [{:uuid "subject" :status "breakdown" :dependency 42}]]
           ["dependency cycle" [{:uuid "subject" :status "breakdown" :dependency "predecessor"}
                                {:uuid "predecessor" :status "done" :dependency "subject"}]]
           ["unfinished transitive predecessor" [{:uuid "subject" :status "breakdown" :dependency "predecessor"}
                                                {:uuid "predecessor" :status "done" :dependency "ancestor"}
                                                {:uuid "ancestor" :status "in_progress"}]]]]
    (testing label
      (await (with-cards cards
               (^:async fn [fixture] (await (assert-refused-move! fixture "breakdown" "ready"))))))))

(deftest ^:async completed-predecessor-admission-remains-a-real-success-case
  (await
   (with-cards [{:uuid "subject" :status "breakdown" :dependency "predecessor"}
                {:uuid "predecessor" :status "done"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [source-path (path/join dir "subject.md")
             history (await (.readFile fsp ledger-path "utf8"))
             result (await (transition/move-task! {:project project
                                                  :task {:uuid "subject" :status "breakdown" :source-path source-path}
                                                  :new-status "ready" :source "test"}))
             after (await (.readFile fsp ledger-path "utf8"))]
         (is (true? (:ok result)))
         (is (= "ready" (get-in result [:task :frontmatter :status]))
             "The returned retained frontmatter must describe the accepted write")
         (is (.startsWith after history) "Successful admission retains the native event prefix")
         (is (> (count after) (count history)) "Success appends its native event"))))))

(deftest ^:async original-wip-and-command-gates-remain-obligations
  (await
   (with-cards [{:uuid "subject" :status "todo" :dependency "predecessor"}
                {:uuid "predecessor" :status "done"}]
     (^:async fn [{:keys [project] :as fixture}]
       (with-redefs [fsm/promethean-fsm (assoc-in fsm/promethean-fsm [:wip-limits "in_progress"] 0)]
         (await (assert-refused-move! fixture "todo" "in_progress")))
       (with-redefs [fsm/run-gate (fn [_ _] (js/Promise.resolve {:allowed? false :reason "fixture command failed"}))]
         (await (assert-refused-move! (assoc fixture :project project) "todo" "in_progress")))))))

(deftest ^:async changed-predecessor-after-executable-gate-is-refused
  (await
   (with-cards [{:uuid "subject" :status "todo" :dependency "predecessor"}
                {:uuid "predecessor" :status "done"}]
     (^:async fn [{:keys [dir] :as fixture}]
       (with-redefs [fsm/run-gate (^:async fn [_ _]
                                   ;; A nonparticipating editor is observed, not serialized.
                                   (await (.writeFile fsp (path/join dir "predecessor.md")
                                                      (fixture-card {:uuid "predecessor" :status "in_progress"}) "utf8"))
                                   {:allowed? true})]
         (await (assert-refused-move! fixture "todo" "in_progress")))))))

(deftest ^:async caller-stale-status-cannot-authorize-a-new-move
  (await
   (with-cards [{:uuid "subject" :status "incoming"}]
     (^:async fn [fixture]
       (await (assert-refused-move! fixture "breakdown" "ready"))))))

(deftest ^:async concurrent-owning-move-cannot-enter-the-held-publication-boundary
  (await
   (with-cards [{:uuid "subject" :status "todo"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [task {:uuid "subject" :status "todo" :source-path (path/join dir "subject.md")}
             nested (atom nil)
             entered (atom false)
             original fsm/run-gate
             before (await (.readFile fsp (:source-path task) "utf8"))
             history (await (.readFile fsp ledger-path "utf8"))]
         (with-redefs [fsm/run-gate (^:async fn [decision cwd]
                                     (if (compare-and-set! entered false true)
                                       (do
                                         (reset! nested (await (transition/move-task!
                                                               {:project project :task task
                                                                :new-status "in_progress" :source "test"})))
                                         {:allowed? false :reason "fixture parent stops before effects"})
                                       (await (original decision cwd))))]
           (let [outer (await (transition/move-task! {:project project :task task
                                                     :new-status "in_progress" :source "test"}))]
             (is (false? (:ok outer)))
             (is (false? (:ok @nested)) "Independent attempt cannot publish inside the owner's gate")
             (is (= :conflict (:kind @nested)))
             (is (= before (await (.readFile fsp (:source-path task) "utf8"))))
             (is (= history (await (.readFile fsp ledger-path "utf8")))))))))))

(deftest ^:async native-tool-creation-and-edit-use-the-same-reviewed-writer
  (await
   (with-cards [{:uuid "subject" :type "task" :status "incoming"}
                {:uuid "epic" :type "epic" :status "done"}
                {:uuid "parent" :type "task" :status "done" :epic "epic"}]
     (^:async fn [{:keys [project]}]
       (let [saved {:projects (projects/all) :default-project-id (projects/default-id)}]
         (projects/set-projects! {:projects [project] :default-project-id (:id project)})
         (try
           (let [created (await (agent-tools/dispatch "kanban_create_task"
                                  {:title "Public child" :uuid "public-child" :parent "parent"
                                   :epic "epic" :dependency "parent" :project (:id project)}))
                 child (first (filter #(= "public-child" (:uuid %)) (await (tasks/load-tasks project))))]
             (is (:ok created))
             (is (= "epic" (:epic child)))
             (is (= ["parent"] (:dependency child))))
           (let [result (try
                          (await (agent-tools/dispatch "kanban_update_frontmatter"
                                   {:uuid "subject" :project (:id project)
                                    :updates {:dependency "parent" :epic "epic"}}))
                          (catch :default error {:error error}))
                 task (first (filter #(= "subject" (:uuid %)) (await (tasks/load-tasks project))))]
             (is (:ok result))
             (is (= ["parent"] (:dependency task)))
             (is (= "epic" (:epic task))))
           (finally (projects/set-projects! saved))))))))

(deftest ^:async relationship-create-edit-remove-and-replay-retain-accepted-facts
  (await
   (with-cards [{:uuid "subject" :type "task" :status "incoming"}
                {:uuid "epic" :type "epic" :status "done"}
                {:uuid "parent" :type "task" :status "done" :epic "epic"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [created (await (create/create-task! {:project project :title "Child" :uuid "child"
                                                 :parent "parent" :epic "epic" :dependency ["parent"]
                                                 :body "Authored body." :source "test"}))
             child (first (filter #(= "child" (:uuid %)) (await (tasks/load-tasks project))))]
         (is (:ok created))
         (is (= "epic" (:epic child)))
         (is (= ["parent"] (:dependency child)))
         (let [task {:uuid "subject" :source-path (path/join dir "subject.md")}
               updates {:parent "parent" :epic "epic" :dependency ["parent"] :priority "P0"}
               result (await (edit/update-frontmatter! {:project project :task task :updates updates :source "test"}))
               before (await (.readFile fsp (:source-path task) "utf8"))
               history (await (.readFile fsp ledger-path "utf8"))
               repeated (await (edit/update-frontmatter! {:project project :task task :updates updates :source "test"}))]
           (is (:ok result))
           (is (:noop repeated))
           (is (= before (await (.readFile fsp (:source-path task) "utf8"))))
           (is (= history (await (.readFile fsp ledger-path "utf8"))))
           (let [removed (await (edit/update-frontmatter! {:project project :task task
                                                          :updates {:parent nil :epic "" :dependency []}
                                                          :source "test"}))
                 after (await (.readFile fsp (:source-path task) "utf8"))
                 fm (:frontmatter (parser/parse-frontmatter after))
                 recorded (await (events/query-events (ledger/get-ledger dir) {}))
                 payloads (map :payload recorded)
                 creation (first (filter #(= "task-created" (:type %)) payloads))
                 edits (filter #(and (= "frontmatter" (:type %)) (= "subject" (:task-id %))) payloads)
                 folded (reduce (fn [state {:keys [key new-value]}]
                                  (if (nil? new-value) (dissoc state (keyword key))
                                    (assoc state (keyword key) new-value))) {} edits)]
             (is (:ok removed))
             (is (not-any? #(contains? fm %) [:parent :epic :dependency]))
             (is (.endsWith after "Retained comment.\n"))
             (is (= {:parent "parent" :epic "epic" :dependency ["parent"]}
                    (select-keys creation [:parent :epic :dependency])))
             (is (= "task" (:card-type creation)))
             (is (= "P0" (:priority folded)))
             (is (not-any? #(contains? folded %) [:parent :epic :dependency]))
             (is (= 1 (count (set (map :write-id (take 4 edits))))) "One batch has one correlation ID"))))))))

(deftest ^:async low-level-writer-refuses-protected-or-invalid-mixed-batches
  (await
   (with-cards [{:uuid "subject" :type "task" :status "incoming"}
                {:uuid "predecessor" :type "task" :status "done"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [task {:uuid "subject" :source-path (path/join dir "subject.md")}]
         (doseq [updates [{:dependency "missing" :priority "P0"}
                          {:parent "predecessor" :uuid "replacement"}
                          {:status "done"} {:write-id "forged"} {:type "epic"}
                          {:dependency ["predecessor" "predecessor"]}]]
           (let [before (await (.readFile fsp (:source-path task) "utf8"))
                 history (await (.readFile fsp ledger-path "utf8"))
                 error (try (await (edit/update-frontmatter! {:project project :task task :updates updates}))
                            nil (catch :default error error))]
             (is (= :refused (:kind (ex-data error))) (pr-str updates))
             (is (= before (await (.readFile fsp (:source-path task) "utf8"))))
             (is (= history (await (.readFile fsp ledger-path "utf8")))))))))))

(deftest ^:async native-comment-cannot-overwrite-a-reserved-relationship-publication
  (await
   (with-cards [{:uuid "subject" :type "task" :status "incoming"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [task {:uuid "subject" :source-path (path/join dir "subject.md")}
             before (await (.readFile fsp (:source-path task) "utf8"))
             history (await (.readFile fsp ledger-path "utf8"))
             failure (await
                      (publication/with-reservation! project
                        (^:async fn []
                          (try
                            (await (edit/append-comment! {:project project :task task :text "Concurrent comment"}))
                            nil
                            (catch :default error error)))))]
         (is (= :conflict (:kind (ex-data failure))))
         (is (= before (await (.readFile fsp (:source-path task) "utf8"))))
         (is (= history (await (.readFile fsp ledger-path "utf8")))))))))

(deftest ^:async event-append-failure-retains-file-readback-and-history
  (await
   (with-cards [{:uuid "subject" :type "task" :status "incoming"}
                {:uuid "predecessor" :type "task" :status "done"}]
     (^:async fn [{:keys [project dir ledger-path]}]
       (let [task {:uuid "subject" :source-path (path/join dir "subject.md")}
             history (await (.readFile fsp ledger-path "utf8"))
             failure (with-redefs [events/emit-frontmatter-change!
                                  (fn [& _] (js/Promise.reject (js/Error. "Controlled event append failure")))]
                       (try
                         (await (edit/update-frontmatter! {:project project :task task
                                                          :updates {:dependency "predecessor"}}))
                         nil
                         (catch :default error error)))
             after (await (.readFile fsp (:source-path task) "utf8"))
             data (ex-data failure)]
         (is (= :partial-effect (:kind data)))
         (is (= :event-append (:phase data)))
         (is (string? (:write-id data)))
         (is (= (.-byteLength (.from js/Buffer after "utf8")) (get-in data [:file-readback :bytes])))
         (is (= ["predecessor"] (:dependency (first (filter #(= "subject" (:uuid %))
                                                                    (await (tasks/load-tasks project)))))))
         (is (= history (await (.readFile fsp ledger-path "utf8")))))))))

(deftest ^:async cleanup-failure-cannot-hide-an-earlier-partial-effect
  (await
   (with-cards [{:uuid "subject" :status "incoming"}]
     (^:async fn [{:keys [project dir]}]
       (let [source-path (path/join dir "subject.md")
             owner-path (path/join dir ".rheos-writer-reservation" "owner.json")
             failure (try
                       (await
                        (publication/with-reservation! project
                          (^:async fn []
                            (await (publication/file-and-event!
                                    {:source-path source-path :write-id "controlled-partial"}
                                    (^:async fn []
                                      (await (.writeFile fsp source-path "Actual changed bytes" "utf8"))
                                      (await (.writeFile fsp owner-path "Changed ownership" "utf8")))
                                    (fn [] (js/Promise.reject (js/Error. "Controlled event failure"))))))))
                       nil
                       (catch :default error error))
             data (ex-data failure)]
         (is (= :partial-effect (:kind data)))
         (is (= :event-append (:phase data)))
         (is (= "controlled-partial" (:write-id data)))
         (is (= 20 (get-in data [:file-readback :bytes])))
         (is (= :conflict (get-in data [:reservation-release :kind])))
         (is (= "Changed ownership" (await (.readFile fsp owner-path "utf8")))
             "Never remove a changed or unknown owner's reservation"))))))

(deftest ^:async cleanup-failure-after-effects-cannot-report-an-unqualified-success
  (await
   (with-cards [{:uuid "subject" :status "incoming"}]
     (^:async fn [{:keys [project dir]}]
       (let [owner-path (path/join dir ".rheos-writer-reservation" "owner.json")
             failure (try
                       (await (publication/with-reservation! project
                                (^:async fn []
                                  (await (.writeFile fsp owner-path "Changed ownership" "utf8"))
                                  {:ok true :uuid "subject"})))
                       nil
                       (catch :default error error))
             data (ex-data failure)]
         (is (= :partial-effect (:kind data)))
         (is (= :reservation-release (:phase data)))
         (is (= {:ok true :uuid "subject"} (:operation-result data)))
         (is (= :conflict (get-in data [:reservation-release :kind])))
         (is (= "Changed ownership" (await (.readFile fsp owner-path "utf8")))))))))
