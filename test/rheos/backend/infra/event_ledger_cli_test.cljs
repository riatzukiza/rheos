(ns rheos.backend.infra.event-ledger-cli-test
  "Actual compiled CLI exits and no-write controls; no board bootstrap is allowed."
  (:require [cljs.test :refer [deftest is]]
            [cljs.reader :as reader]
            [clojure.string :as str]
            ["node:child_process" :as child]
            ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]))

(deftest explicit-ledger-check-bypasses-config-and-preserves-fixtures
  (let [dir (.mkdtempSync fs (path/join (.tmpdir os) "rheos-ledger-check-"))
        cli-path (path/resolve "dist/cli.cjs")
        unique (path/join dir "unique.edn")
        duplicate (path/join dir "duplicate.edn")
        empty-path (path/join dir "empty.edn")
        malformed (path/join dir "malformed.edn")
        missing (path/join dir "missing.edn")
        unreadable (path/join dir "unreadable.edn")
        invalid-utf8 (path/join dir "invalid-utf8.edn")
        two-forms (path/join dir "two-forms.edn")
        blank-row (path/join dir "blank-row.edn")
        bom-path (path/join dir "bom.edn")
        broken-config (path/join dir "unrelated-config.edn")
        inaccessible-config (path/join dir "unreadable-config.edn")
        denial-preload (path/join dir "deny-owned-read.cjs")
        root? (and (some? (.-getuid js/process)) (zero? (.getuid js/process)))
        files [[unique "{:event/id \"legacy-1791744352193\"}\n{:event/id \"literal, id-雪\"}\n"]
               [duplicate "{:event/id \"same\"}\n{:event/id \"same\" :payload {:changed true}}\n"]
               [empty-path ""] [malformed "{:event/id\n\"split\"}\n"]
               [two-forms "{:event/id \"first\"} {:event/id \"second\"}\n"]
               [blank-row "{:event/id \"first\"}\n\n"]
               [bom-path "\uFEFF{:event/id \"bom\"}\n"]
               [unreadable "{:event/id \"inaccessible\"}\n"]
               [broken-config "{:this-is-not-valid"]
               [inaccessible-config "{:this-is-not-readable true}"]
               [denial-preload
                (str "const fs = require('node:fs/promises'); const read = fs.readFile;\n"
                     "fs.readFile = function(path, ...args) {\n"
                     " if (String(path) === process.env.RHEOS_TEST_UNREADABLE_PATH)\n"
                     "  return Promise.reject(Object.assign(new Error('owned fixture denial'), {code: 'EACCES'}));\n"
                     " return read.call(this, path, ...args); };\n")]]]
    (try
      (doseq [[file content] files] (.writeFileSync fs file content "utf8"))
      (let [bytes (js/Buffer.from #js [123 255 125])]
        (.writeFileSync fs invalid-utf8 bytes))
      (.chmodSync fs unreadable 0)
      (.chmodSync fs inaccessible-config 0)
      (let [names (vec (.readdirSync fs dir))]
        (doseq [config-path [nil broken-config inaccessible-config]]
          (let [env (doto (js/Object.assign #js {} js/process.env)
                      (js-delete "KANBAN_CONFIG"))]
            (when config-path (aset env "KANBAN_CONFIG" config-path))
            (doseq [[args expected] [[["check-event-ledger" unique] 0]
                                   [["check-event-ledger" duplicate] 3]
                                   [["check-event-ledger" malformed] 3]
                                   [["check-event-ledger" empty-path] 3]
                                   [["check-event-ledger" two-forms] 3]
                                   [["check-event-ledger" blank-row] 3]
                                   [["check-event-ledger" bom-path] 0]
                                   [["check-event-ledger" invalid-utf8] 3]
                                   [["check-event-ledger" unique "--config" broken-config
                                     "--tasks-dir" (path/join dir "never-created")] 0]
                                   [["check-event-ledger" missing] 2]
                                   [["check-event-ledger" dir] 4]
                                   [["check-event-ledger" unreadable] 4]
                                   [["check-event-ledger"] 1]
                                   [["check-event-ledger" "--json"] 1]]]
              (let [denied? (and root? (= unreadable (second args)))
                    child-env (js/Object.assign #js {} env)
                    _ (js-delete child-env "RHEOS_TEST_UNREADABLE_PATH")
                    _ (when denied? (aset child-env "RHEOS_TEST_UNREADABLE_PATH" unreadable))
                    result (.spawnSync child (.-execPath js/process)
                                       (clj->js (into (if denied?
                                                       ["--require" denial-preload cli-path]
                                                       [cli-path]) args))
                                       #js {:cwd dir :env child-env :encoding "utf8"})]
                (is (= expected (.-status result))
                    (str args " config=" config-path " stderr=" (.-stderr result)))
                (is (seq (.-stdout result)) "Every checker outcome emits JSON, including missing path")
                (when (seq (.-stdout result))
                  (let [report (js->clj (js/JSON.parse (.-stdout result)) :keywordize-keys true)]
                    (is (= (zero? expected) (:ok report)))
                    (is (= (when-not (str/starts-with? (or (second args) "") "--") (second args)) (:path report)))
                    (when (= unreadable (second args))
                      ;; Non-root observes mode-000 denial. Root uses a scoped
                      ;; readFile fault: root permissions cannot prove EACCES.
                      (is (= "EACCES" (get-in report [:errors 0 :io-code]))))
                    (when (or (= 1 (count args)) (= "--json" (second args)))
                      (is (= 0 (:record-count report)))
                      (is (= "usage" (get-in report [:errors 0 :code]))))
                    (when (= (second args) duplicate)
                      (is (= [{:row 2 :code "duplicate-event-id" :event-id "same" :first-row 1}]
                             (:errors report))))))))))
        (is (= names (vec (.readdirSync fs dir))) "No board, view store, config or missing ledger was created")
        (.chmodSync fs unreadable 384)
        (.chmodSync fs inaccessible-config 384)
        ;; The production EDN admission adapter uses cljs.reader/read-string.
        ;; Verify the actual compiled reader admits the same BOM-prefixed map;
        ;; NBB observations alone do not prove compiled-reader parity.
        (is (= {:event/id "bom"} (reader/read-string (.readFileSync fs bom-path "utf8"))))
        (doseq [[file content] files]
          (is (= content (.readFileSync fs file "utf8")) "Every fixture remains byte-identical"))
        (is (.equals (js/Buffer.from #js [123 255 125]) (.readFileSync fs invalid-utf8))))
      (finally
        (.rmSync fs dir #js {:recursive true :force true})))))
