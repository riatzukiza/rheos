(ns rheos.backend.infra.event-identity-test
  "Real writer/ledger identity regression; historical fixture bytes stay immutable."
  (:require [cljs.test :refer [deftest is]]
            ["node:fs/promises" :as fsp]
            ["node:os" :as os]
            ["node:path" :as path]
            [open-hax.records.edn.event-admission :as edn-admission]
            [open-hax.openplanner-protocols :as protocols]
            [rheos.backend.infra.ledger :as ledger]
            [rheos.backend.domain.events :as events]
            [rheos.backend.law.event-identity :as identity-law]
            [rheos.backend.infra.task-edit :as edit]
            [rheos.backend.infra.task-store :as tasks]))

(deftest ^:async independent-native-admission-writers-have-distinct-frozen-time-ids
  (let [dir (await (.mkdtemp fsp (path/join (.tmpdir os) "rheos-two-writers-")))
        saved-now (.-now js/Date)
        saved-iso (.-toISOString (.-prototype js/Date))
        board "literal, id"
        task " legacy-雪 "
        timestamp "2026-10-11T18:45:52.193Z"]
    (try
      (set! (.-now js/Date) (fn [] 1791744352193))
      (set! (.-toISOString (.-prototype js/Date)) (fn [] timestamp))
      (let [first-writer (edn-admission/create-edn-event-admission dir)
            second-writer (edn-admission/create-edn-event-admission dir)]
        (await (events/emit-comment! first-writer board task "First" "shared" "test"))
        (await (events/emit-comment! second-writer board task "Second" "shared" "test"))
        (let [reader (edn-admission/create-edn-event-admission dir)
              all (await (protocols/query-events reader {}))]
          (is (= 2 (count all)))
          (is (= 2 (count (set (map :event/id all)))))
          (is (every? #(identity-law/generated-id? board task (:event/id %)) all))
          (is (= ["First" "Second"] (mapv #(get-in % [:payload :text]) all)))
          (doseq [event all]
            (is (= {:event/type "kanban.comment" :event/time timestamp :session/id board
                    :delivery/mode "tell" :payload {:task-id task :text (get-in event [:payload :text])
                                                   :write-id "shared" :source "test" :agent "eta-mu" :type "comment"}}
                   (dissoc event :event/id :causal/root))))))
      (finally
        (set! (.-now js/Date) saved-now)
        (set! (.-toISOString (.-prototype js/Date)) saved-iso)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async same-millisecond-native-batch-preserves-duplicate-history-and-new-identities
  (let [dir (await (.mkdtemp fsp (path/join (.tmpdir os) "rheos-event-identity-")))
        project {:id "board" :tasks-dir dir :fsm "promethean"}
        source-path (path/join dir "subject.md")
        events-dir (path/join dir ".events")
        ledger-path (path/join events-dir "ledger.edn")
        original "---\nuuid: subject\ntitle: Old\npriority: P3\nstatus: incoming\nlabels: old\n---\n\nRetained body.\n"
        historical [{:event/id "board-subject-1791744352193" :event/type "kanban.frontmatter"
                     :event/time "2026-10-11T18:45:52.193Z" :session/id "board" :delivery/mode "tell"
                     :causal/root "old-root-title" :payload {:task-id "subject" :type "frontmatter" :key "title"}}
                    {:event/id "board-subject-1791744352193" :event/type "kanban.frontmatter"
                     :event/time "2026-10-11T18:45:52.193Z" :session/id "board" :delivery/mode "tell"
                     :causal/root "old-root-labels" :payload {:task-id "subject" :type "frontmatter" :key "labels"}}]
        saved-now (.-now js/Date)]
    (try
      (await (.writeFile fsp source-path original "utf8"))
      (let [seed (edn-admission/create-edn-event-admission events-dir)]
        (await (protocols/append-events! seed historical)))
      (let [prefix (await (.readFile fsp ledger-path))
            task (first (await (tasks/load-tasks project)))]
        (set! (.-now js/Date) (fn [] 1791744352193))
        (let [result (await (edit/update-frontmatter!
                            {:project project :task task :updates {:title "New" :labels "new"} :source "test"}))
              ;; A new admission instance independently reads the actual persisted file.
              fresh (edn-admission/create-edn-event-admission events-dir)
              all (await (protocols/query-events fresh {}))
              appended (subvec all 2)
              bytes (await (.readFile fsp ledger-path))
              card (await (.readFile fsp source-path "utf8"))
              write-id (get-in result [:frontmatter :write-id])]
          (is (:ok result))
          (is (= 4 (count all)))
          (is (= historical (subvec all 0 2)))
          (is (.equals prefix (.subarray bytes 0 (.-length prefix)))
              "The historical duplicate-ID prefix is retained byte for byte")
          (is (= 2 (count (set (map :event/id appended))))
              "Multiple fields in one native writer batch get distinct IDs")
          (is (every? #(not= "board-subject-1791744352193" (:event/id %)) appended)
              "New emissions must not collide with the frozen-clock historical IDs")
          (is (= [{:key "title" :old-value "Old" :new-value "New"}
                  {:key "labels" :old-value "old" :new-value "new"}]
                 (mapv #(select-keys (:payload %) [:key :old-value :new-value]) appended)))
          (is (and (string? write-id) (seq write-id)))
          (is (every? #(= {:task-id "subject" :type "frontmatter" :source "test"
                          :agent "eta-mu" :write-id write-id}
                         (select-keys (:payload %) [:task-id :type :source :agent :write-id])) appended))
          (is (every? #(and (= "kanban.frontmatter" (:event/type %))
                           (= "board" (:session/id %))
                           (= "tell" (:delivery/mode %))
                           (string? (:event/time %))
                           (seq (:causal/root %))) appended))
          (is (.endsWith card "\n\nRetained body.\n"))))
      (finally
        (set! (.-now js/Date) saved-now)
        (swap! ledger/ledger-cache dissoc dir)
        (await (.rm fsp dir #js {:recursive true :force true}))))))
