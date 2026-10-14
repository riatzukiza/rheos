#!/usr/bin/env bb
;; Fixture tests for scripts/verify_receipts.clj in throwaway git repositories.
(ns verify-receipts-test
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests testing]]))

(def script (str (fs/absolutize "scripts/verify_receipts.clj")))
(load-file script)
(alias 'vr 'verify-receipts)

(def receipt
  {:ts "2026-10-13T00:00:00Z" :kind :observation :origin "test" :owner "agent"
   :dod "d" :pi "p" :host "h" :manifest ["a"] :refs ["r"]})

(defn sh! [dir & args]
  (let [r (apply process/shell {:dir (str dir) :out :string :err :string :continue true} args)]
    (when-not (zero? (:exit r)) (throw (ex-info (str args (:err r)) r)))
    (str/trim (:out r))))

(defn write! [dir path text]
  (let [f (fs/file dir path)] (fs/create-dirs (fs/parent f)) (spit f text)))

(defn commit! [dir]
  (sh! dir "git" "add" "-A")
  (sh! dir "git" "-c" "user.name=t" "-c" "user.email=t@t" "commit" "-qm" "c" "--allow-empty")
  (sh! dir "git" "rev-parse" "HEAD"))

(defn check [dir base at]
  (binding [process/*defaults* (assoc process/*defaults* :dir (str dir))]
    (try (vr/verify! base at) :ok
         (catch clojure.lang.ExceptionInfo e (ex-message e)))))

(defn with-repo [f]
  (let [dir (fs/create-temp-dir)]
    (try (sh! dir "git" "init" "-q")
         (write! dir ".ημ/receipts.edn" (str (pr-str (assoc receipt :ts "2026-10-01T00:00:00Z")) "\n"))
         (write! dir ".ημ/receipts/README.md" "x\n")
         (f dir (commit! dir))
         (finally (fs/delete-tree dir)))))

(def seg ".ημ/receipts/2026-10-13T000000Z-abc.edn")

(deftest accepts-a-new-segment
  (with-repo (fn [dir base]
               (write! dir seg (str (pr-str receipt) "\n"))
               (is (= :ok (check dir base (commit! dir)))))))

(deftest rejects-legacy-append
  (with-repo (fn [dir base]
               (spit (fs/file dir ".ημ/receipts.edn") (str (pr-str receipt) "\n") :append true)
               (is (str/includes? (check dir base (commit! dir)) "frozen")))))

(deftest rejects-edited-and-removed-segments
  (with-repo (fn [dir _]
               (write! dir seg (str (pr-str receipt) "\n"))
               (let [base (commit! dir)]
                 (write! dir seg (str (pr-str (assoc receipt :note "x")) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "edited"))
                 (fs/delete (fs/file dir seg))
                 (is (str/includes? (check dir base (commit! dir)) "removed"))))))

(deftest rejects-bad-segments
  (with-repo (fn [dir base]
               (testing "bad name"
                 (write! dir ".ημ/receipts/2026-13-40T000000Z-a.edn" (str (pr-str receipt) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "name must match"))
                 (fs/delete (fs/file dir ".ημ/receipts/2026-13-40T000000Z-a.edn")))
               (testing "duplicate record"
                 (write! dir seg (str (pr-str receipt) "\n"))
                 (write! dir ".ημ/receipts/2026-10-13T000001Z-b.edn" (str (pr-str receipt) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "repeats")))
               (testing "invalid envelope"
                 (fs/delete (fs/file dir ".ημ/receipts/2026-10-13T000001Z-b.edn"))
                 (write! dir seg (str (pr-str (dissoc receipt :owner)) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "invalid envelopes")))
               (testing "missing newline and two records"
                 (write! dir seg (pr-str receipt))
                 (is (str/includes? (check dir base (commit! dir)) "newline"))
                 (write! dir seg (str (pr-str receipt) "\n" (pr-str (assoc receipt :ts "x")) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "exactly one"))))))

(deftest a-legacy-record-may-move-into-a-segment
  (with-repo (fn [dir base]
               (write! dir ".ημ/receipts.edn" "")
               (write! dir seg (str (pr-str (assoc receipt :ts "2026-10-01T00:00:00Z")) "\n"))
               (is (= :ok (check dir base (commit! dir)))))))

(let [{:keys [fail error]} (run-tests 'verify-receipts-test)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
