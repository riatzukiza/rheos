(ns rheos.backend.infra.event-ledger-cli-test
  "Actual compiled CLI exits and no-write controls; no board bootstrap is allowed."
  (:require [cljs.test :refer [deftest is]]
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
        broken-config (path/join dir "unrelated-config.edn")
        inaccessible-config (path/join dir "unreadable-config.edn")
        files [[unique "{:event/id \"legacy-1791744352193\"}\n{:event/id \"literal, id-雪\"}\n"]
               [duplicate "{:event/id \"same\"}\n{:event/id \"same\" :payload {:changed true}}\n"]
               [empty-path ""] [malformed "{:event/id\n\"split\"}\n"]
               [unreadable "{:event/id \"inaccessible\"}\n"]
               [broken-config "{:this-is-not-valid"]
               [inaccessible-config "{:this-is-not-readable true}"]]]
    (try
      (doseq [[file content] files] (.writeFileSync fs file content "utf8"))
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
                                   [["check-event-ledger" missing] 2]
                                   [["check-event-ledger" dir] 4]
                                   [["check-event-ledger" unreadable] 4]
                                   [["check-event-ledger"] 1]]]
              (let [result (.spawnSync child (.-execPath js/process)
                                       (clj->js (into [cli-path] args))
                                       #js {:cwd dir :env env :encoding "utf8"})]
                (is (= expected (.-status result))
                    (str args " config=" config-path " stderr=" (.-stderr result)))
                (when (and (seq (.-stdout result)) (second args))
                  (let [report (js->clj (js/JSON.parse (.-stdout result)) :keywordize-keys true)]
                    (is (= (zero? expected) (:ok report)))
                    (is (= (second args) (:path report)))
                    (when (= (second args) duplicate)
                      (is (= [{:row 2 :code "duplicate-event-id" :event-id "same" :first-row 1}]
                             (:errors report))))))))))
        (is (= names (vec (.readdirSync fs dir))) "No board, view store, config or missing ledger was created")
        (.chmodSync fs unreadable 384)
        (.chmodSync fs inaccessible-config 384)
        (doseq [[file content] files]
          (is (= content (.readFileSync fs file "utf8")) "Every fixture remains byte-identical")))
      (finally
        (.rmSync fs dir #js {:recursive true :force true})))))
