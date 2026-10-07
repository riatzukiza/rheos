(ns rheos5.fulltip-receipts
  (:require [clojure.string :as str]
            [eta-mu.receipt-river.api :as api]
            ["node:child_process" :as cp]))
(let [[git-dir revision] *command-line-args*
      text (.execFileSync cp "git" #js [(str "--git-dir=" git-dir) "show" (str revision ":.ημ/receipts.edn")] #js {:encoding "utf8"})
      rows (str/split-lines text)
      results (mapv api/validate-line rows (range 1 (inc (count rows))))
      new-row (nth results 13)]
  (assert (= 14 (count rows)))
  (assert (:ok new-row))
  (assert (= :declared (get-in new-row [:source/schema :status])))
  (assert (:ok (nth results 12)))
  (println (js/JSON.stringify
             (clj->js {:physical-lines (count rows) :new-owned-line14 (select-keys new-row [:ok :line-number :errors :source/schema])
                       :parent-line13 (select-keys (nth results 12) [:ok :line-number :errors :source/schema])
                       :inherited-refusals (filterv (complement :ok) (mapv #(select-keys % [:ok :line-number :errors :source/schema]) (take 12 results)))}))))
