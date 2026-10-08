(ns rheos.backend.domain.compose-test
  (:require [cljs.test :refer [deftest testing is]]
            ["node:fs/promises" :as fsp]
            ["node:os" :as os]
            ["node:path" :as path]
            [clojure.string :as str]
            [rheos.backend.domain.compose :as compose]
            [rheos.backend.domain.events :as events]
            [rheos.backend.infra.ledger :as ledger]))

(deftest parse-where-clause-eq
  (is (= ["meta.domain" := "infra"] (compose/parse-where-clause "meta.domain = infra"))))

(deftest parse-where-clause-in
  (is (= ["meta.org" :in ["open-hax" "octave-commons"]] (compose/parse-where-clause "meta.org in open-hax,octave-commons"))))

(deftest parse-where-clause-contains
  (is (= ["meta.tags" :contains "proxy"] (compose/parse-where-clause "meta.tags contains proxy"))))

(deftest parse-where-clause-regex
  (is (= ["title" :regex "infra.*"] (compose/parse-where-clause "title ~ infra.*")))
  (is (= ["title" :regex "infra.*"] (compose/parse-where-clause "title ~ /infra.*/")))
  (is (= ["title" :regex ".*in progress.*"] (compose/parse-where-clause "title ~ .*in progress.*")))
  (is (= ["title" :regex ".*contains.*"] (compose/parse-where-clause "title ~ /.*contains.*/"))))

(deftest parse-compose-query-basic
  (let [flags {:status "todo,in_progress" :priority "P0,P1" :projects "proxx,eta-mu"}
        query (compose/parse-compose-query flags)]
    (is (= ["todo" "in_progress"] (:status query)))
    (is (= ["proxx" "eta-mu"] (:across query)))))

(defn- ^:async write-task [dir uuid title status priority labels]
  (let [file-path (path/join dir (str uuid ".md"))
        labels-str (str/join ", " labels)
        frontmatter (str "---\n"
                         "uuid: \"" uuid "\"\n"
                         "title: \"" title "\"\n"
                         "status: \"" status "\"\n"
                         "priority: \"" priority "\"\n"
                         "labels: \"" labels-str "\"\n"
                         "---\n\n# " title)]
    (await (.writeFile fsp file-path frontmatter "utf8"))))

(deftest ^:async compose-snapshot-contains-matches-label
  (let [tmp-dir (path/join (js/process.cwd) "target" "compose-test-contains")
        _ (await (.mkdir fsp tmp-dir #js {:recursive true}))
        projects [{:id "p" :title "P" :tasks-dir tmp-dir :meta {}}]
        query {:status [] :priority [] :labels [] :across [] :where-clauses [[:labels :contains "proxy"]]}]
    (await (write-task tmp-dir "a" "Proxy task" "todo" "P1" ["proxy"]))
    (await (write-task tmp-dir "b" "Other task" "todo" "P1" ["ui"]))
    (let [snapshot (await (compose/compose-snapshot projects query))]
      (is (= 1 (:total-tasks snapshot)))
      (is (= "a" (:uuid (first (:tasks (first (filter #(= "todo" (:status %)) (:columns snapshot)))))))))
    (await (.rm fsp tmp-dir #js {:recursive true :force true}))))

(deftest ^:async compose-snapshot-regex-matches-title
  (let [tmp-dir (path/join (js/process.cwd) "target" "compose-test-regex")
        _ (await (.mkdir fsp tmp-dir #js {:recursive true}))
        projects [{:id "p" :title "P" :tasks-dir tmp-dir :meta {}}]
        query {:status [] :priority [] :labels [] :across [] :where-clauses [[:title :regex "infra-.*"]]}]
    (await (write-task tmp-dir "a" "infra-proxy" "todo" "P1" []))
    (await (write-task tmp-dir "b" "frontend-ui" "todo" "P1" []))
    (let [snapshot (await (compose/compose-snapshot projects query))]
      (is (= 1 (:total-tasks snapshot)))
      (is (= "a" (:uuid (first (:tasks (first (filter #(= "todo" (:status %)) (:columns snapshot)))))))))
    (await (.rm fsp tmp-dir #js {:recursive true :force true}))))

(deftest ^:async compose-snapshot-includes-domain-and-org-from-meta
  (testing "Tasks inherit domain and org from project meta"
    (let [tmp-dir (path/join (js/process.cwd) "target" "compose-test-meta-enrichment")
          _ (await (.mkdir fsp tmp-dir #js {:recursive true}))
          projects [{:id "p" :title "P" :tasks-dir tmp-dir :meta {:domain "proxx" :org "open-hax"}}]
          query {:status [] :priority [] :labels [] :across [] :where-clauses []}]
      (await (write-task tmp-dir "a" "Task A" "todo" "P1" []))
      (let [snapshot (await (compose/compose-snapshot projects query))
            tasks (->> snapshot :columns (filter #(= "todo" (:status %))) first :tasks)]
        (is (= 1 (count tasks)))
        (is (= "proxx" (:domain (first tasks))))
        (is (= "open-hax" (:org (first tasks)))))
      (await (.rm fsp tmp-dir #js {:recursive true :force true})))))

(deftest ^:async composed-board-propagates-refused-source-and-recovers
  (let [root (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-compose-refusal-")))
        good-dir (path/join root "good")
        bad-dir (path/join root "bad")
        bad-path (path/join bad-dir "broken.md")
        raw "---\nuuid: broken\nlabels: [unfinished\n---\n\n# Unchanged body\n"
        projects [{:id "good" :tasks-dir good-dir :meta {}}
                  {:id "bad" :tasks-dir bad-dir :meta {}}]
        query (compose/parse-compose-query {})]
    (try
      (await (.mkdir fsp good-dir))
      (await (.mkdir fsp bad-dir))
      (await (write-task good-dir "good" "Good" "todo" "P1" []))
      (await (.writeFile fsp bad-path raw "utf8"))
      (let [error (try (await (compose/compose-snapshot projects query))
                       nil (catch :default err err))]
        (is (some? error) "a composed board cannot silently omit a refused project's source")
        (is (= :refused (:kind (ex-data error))))
        (is (= bad-path (:source-path (ex-data error))))
        (is (and error (str/includes? (.-message error) bad-path))))
      (is (= raw (await (.readFile fsp bad-path "utf8"))))
      (await (write-task bad-dir "broken" "Repaired" "todo" "P1" []))
      (let [snapshot (await (compose/compose-snapshot projects query))]
        (is (= 2 (:total-tasks snapshot)))
        (is (= #{"good" "broken"} (set (mapcat #(map :uuid (:tasks %)) (:columns snapshot))))))
      (testing "unrelated composition errors keep their previous fallback"
        (is (= 1 (:total-tasks
                  (await (compose/compose-snapshot
                          [(first projects) {:id "unusable" :tasks-dir nil}] query))))
            "a non-refusal project usage error still skips that project")
        (is (= 0 (:total-tasks
                  (await (compose/compose-snapshot projects
                                                   {:where-clauses [[42 := "invalid"]]}))))
            "a non-refusal outer query error still returns an empty snapshot"))
      (finally
        (await (.rm fsp root #js {:recursive true :force true}))))))

(deftest ^:async composed-board-retains-each-supplied-project-projection
  (let [root (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-compose-projection-")))
        first-dir (path/join root "first")
        second-dir (path/join root "second")
        excluded-path (path/join root "unprojected.md")
        included-path (path/join first-dir "broken.md")
        unclosed "---\nuuid: broken\n# Missing closing delimiter\n"
        projects [{:id "first" :tasks-dir root :meta {}
                   :card-projection {:paths [first-dir]}}
                  {:id "second" :tasks-dir root :meta {}
                   :card-projection {:paths [second-dir]}}]
        query (compose/parse-compose-query {})]
    (try
      (await (.mkdir fsp first-dir))
      (await (.mkdir fsp second-dir))
      (await (write-task first-dir "one" "First" "todo" "P1" []))
      (await (write-task second-dir "two" "Second" "todo" "P1" []))
      (await (.writeFile fsp excluded-path "---\nlabels: [unfinished\n---\n" "utf8"))
      (await (.writeFile fsp (path/join first-dir "notes.txt") unclosed "utf8"))
      (let [snapshot (try (await (compose/compose-snapshot projects query))
                          (catch :default err err))]
        (is (= 2 (:total-tasks snapshot)) "ad hoc projects sharing a task root retain separate scopes")
        (is (= #{["one" "first"] ["two" "second"]}
               (set (mapcat #(map (juxt :uuid :source-board) (:tasks %)) (:columns snapshot)))))
        (is (= "---\nlabels: [unfinished\n---\n" (await (.readFile fsp excluded-path "utf8")))))
      (let [snapshot (try (await (compose/compose-snapshot
                                 [(assoc (first projects) :card-projection {:paths []})] query))
                          (catch :default err err))]
        (is (= 0 (:total-tasks snapshot)) "an explicit empty projection does not discover neighbors"))
      (await (.writeFile fsp included-path unclosed "utf8"))
      (let [error (try (await (compose/compose-snapshot projects query))
                       nil (catch :default err err))]
        (is (= :refused (:kind (ex-data error))))
        (is (= included-path (:source-path (ex-data error))))
        (is (= "Unterminated YAML frontmatter" (:diagnostic (ex-data error))))
        (is (= unclosed (await (.readFile fsp included-path "utf8")))))
      (await (write-task first-dir "broken" "Repaired" "todo" "P1" []))
      (let [snapshot (try (await (compose/compose-snapshot projects query))
                          (catch :default err err))]
        (is (= 3 (:total-tasks snapshot)))
        (is (= #{"one" "two" "broken"}
               (set (mapcat #(map :uuid (:tasks %)) (:columns snapshot))))))
      (finally
        (await (.rm fsp root #js {:recursive true :force true}))))))

(deftest ^:async compose-snapshot-includes-drift-flag
  (testing "Tasks with a drift-detected ledger event are marked drift=true"
    (let [tmp-dir (path/join (js/process.cwd) "target" "compose-test-drift")
          _ (await (.mkdir fsp tmp-dir #js {:recursive true}))
          projects [{:id "p" :title "P" :tasks-dir tmp-dir :meta {}}]
          query {:status [] :priority [] :labels [] :across [] :where-clauses []}
          ledger (ledger/get-ledger tmp-dir)]
      (await (write-task tmp-dir "drifty" "Drifty task" "todo" "P1" []))
      (await (write-task tmp-dir "clean" "Clean task" "todo" "P1" []))
      (await (events/emit-drift-detected! ledger "p" "drifty" (events/generate-write-id)))
      (let [snapshot (await (compose/compose-snapshot projects query))
            tasks (->> snapshot :columns (filter #(= "todo" (:status %))) first :tasks)
            by-uuid (into {} (map (juxt :uuid identity) tasks))]
        (is (= true (:drift (get by-uuid "drifty"))))
        (is (= false (:drift (get by-uuid "clean")))))
      (await (.rm fsp tmp-dir #js {:recursive true :force true})))))
