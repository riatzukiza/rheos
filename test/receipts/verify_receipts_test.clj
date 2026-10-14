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
  {:ts "2026-10-13T00:00:00Z" :kind :observation :repo "riatzukiza/rheos" :origin "test" :owner "agent"
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

(defn run-check [dir opts]
  (binding [process/*defaults* (assoc process/*defaults* :dir (str dir))]
    (try (vr/run opts) :ok
         (catch clojure.lang.ExceptionInfo e (ex-message e)))))

(defn range-check [dir base at] (run-check dir {:base base :at at :range? true}))

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


(deftest requires-repo-and-a-strict-utc-ts
  (with-repo (fn [dir base]
               (testing "missing :repo"
                 (write! dir seg (str (pr-str (dissoc receipt :repo)) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "invalid envelopes")))
               (testing "blank :repo"
                 (write! dir seg (str (pr-str (assoc receipt :repo " ")) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "invalid envelopes")))
               (doseq [ts ["not-a-time" "2026-10-13T00:00:00+00:00" "2026-02-30T00:00:00Z"
                           "2026-10-13 00:00:00Z" "2026-10-13T00:00Z"]]
                 (testing (str ":ts " ts)
                   (write! dir seg (str (pr-str (assoc receipt :ts ts)) "\n"))
                   (is (str/includes? (check dir base (commit! dir)) "invalid envelopes"))))
               (testing "fractional Z :ts is accepted"
                 (write! dir seg (str (pr-str (assoc receipt :ts "2026-10-13T00:00:00.123Z")) "\n"))
                 (is (= :ok (check dir base (commit! dir))))))))

(deftest segment-is-one-whole-edn-form
  (with-repo (fn [dir base]
               (testing "pretty-printed multi-line receipt is accepted"
                 (write! dir seg (str (str/replace (pr-str receipt) ", " ",\n ") "\n"))
                 (is (< 1 (count (str/split-lines (slurp (fs/file dir seg))))))
                 (is (= :ok (check dir base (commit! dir)))))
               (testing "two forms on one line are rejected"
                 (write! dir seg (str (pr-str receipt) " " (pr-str (assoc receipt :ts "2026-10-13T00:00:01Z")) "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "exactly one"))))))

(deftest legacy-ledger-must-stay-a-regular-file
  (with-repo (fn [dir base]
               (testing "executable legacy ledger"
                 (sh! dir "git" "update-index" "--chmod=+x" ".ημ/receipts.edn")
                 (sh! dir "git" "-c" "user.name=t" "-c" "user.email=t@t" "commit" "-qm" "x")
                 (is (str/includes? (check dir base (sh! dir "git" "rev-parse" "HEAD")) "regular")))
               (testing "symlinked legacy ledger"
                 (sh! dir "git" "update-index" "--chmod=-x" ".ημ/receipts.edn")
                 (fs/move (fs/file dir ".ημ/receipts.edn") (fs/file dir ".ημ/legacy.edn"))
                 (fs/create-sym-link (fs/file dir ".ημ/receipts.edn") "legacy.edn")
                 (is (str/includes? (check dir base (commit! dir)) "regular"))))))

(deftest tagged-literal-payloads-are-canonical
  (with-repo (fn [dir base]
               (let [body (subs (pr-str receipt) 1)]
                 (write! dir seg (str "{:x #foo {:a 1 :b 2} " body "\n"))
                 (write! dir ".ημ/receipts/2026-10-13T000001Z-b.edn" (str "{:x #foo {:b 2 :a 1} " body "\n"))
                 (is (str/includes? (check dir base (commit! dir)) "repeats"))))))

(defn branch! [dir name & [from]]
  (apply sh! dir "git" "checkout" "-q" "-b" name (when from [from])))

(defn merge! [dir other]
  (sh! dir "git" "-c" "user.name=t" "-c" "user.email=t@t" "merge" "-q" "--no-ff" "-m" "m" other)
  (sh! dir "git" "rev-parse" "HEAD"))

(deftest range-checks-real-parent-edges
  (testing "a merge of sibling branches that each add a segment passes"
    (with-repo (fn [dir base]
                 (branch! dir "b")
                 (write! dir seg (str (pr-str receipt) "\n"))
                 (commit! dir)
                 (branch! dir "c" base)
                 (write! dir ".ημ/receipts/2026-10-13T000001Z-c.edn" (str (pr-str (assoc receipt :ts "2026-10-13T00:00:01Z")) "\n"))
                 (commit! dir)
                 (is (= :ok (range-check dir base (merge! dir "b")))))))
  (testing "PR merged with newer main, then synthetically merged into main, passes"
    (with-repo (fn [dir m0]
                 (sh! dir "git" "branch" "-M" "main")
                 (branch! dir "pr")
                 (write! dir "x.txt" "1\n")
                 (commit! dir)
                 (sh! dir "git" "checkout" "-q" "main")
                 (write! dir seg (str (pr-str receipt) "\n"))
                 (let [m1 (commit! dir)]
                   (sh! dir "git" "checkout" "-q" "pr")
                   (merge! dir "main")
                   (sh! dir "git" "checkout" "-q" "main")
                   (is (= :ok (range-check dir m1 (merge! dir "pr"))))
                   (is (some? m0))))))
  (testing "adding then deleting a segment inside the range fails"
    (with-repo (fn [dir base]
                 (write! dir seg (str (pr-str receipt) "\n"))
                 (commit! dir)
                 (fs/delete (fs/file dir seg))
                 (is (str/includes? (range-check dir base (commit! dir)) "removed"))))))

(deftest force-push-keeps-the-supplied-base
  (with-repo (fn [dir a]
               (write! dir seg (str (pr-str receipt) "\n"))
               (commit! dir)
               (write! dir "x.txt" "1\n")
               (let [c (commit! dir)]
                 (sh! dir "git" "checkout" "-q" "-b" "rewrite" a)
                 (write! dir "y.txt" "1\n")
                 (let [d (commit! dir)]
                   (is (str/includes? (range-check dir c d) "removed")))))))

(let [{:keys [fail error]} (run-tests 'verify-receipts-test)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
