(ns rheos.backend.infra.task-writeback-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :refer [async deftest testing is]]
            [clojure.string :as str]
            [rheos.backend.infra.task-writeback :as writeback]
            [rheos.backend.shape.content-parser :as content-parser]))

(defn- tmp-dir []
  (.mkdtempSync fs (path/join (.tmpdir os) "rheos-writeback-test-")))

(defn- task-source [uuid title]
  (str "---\n"
       "uuid: \"" uuid "\"\n"
       "title: \"" title "\"\n"
       "status: \"incoming\" # workflow\n"
       "priority: \"P3\"\n"
       "metadata:\n  owner: Someone\n  links:\n    - docs/a.md\n"
       "summary: |\n  Line one\n  Line two\n"
       "---\n\n# " title "\n\nBody  \n\n```yaml\n---\nstatus: example\n---\n```\n\n---\nstatus: should be preserved\n---\n"))

;; cljs.test async explicitly waits for the Promise; ^:async/await is not
;; transformed by this standalone checkout's compiler.
#_{:clj-kondo/ignore [:promise-chain/prefer-async-workflow]}
(deftest write-task-status-updates-frontmatter-only
  (testing "Status writeback changes only status and write-id in the on-disk source"
    (async done
      (let [dir (tmp-dir)
            file-path (path/join dir "t1.md")
            before (task-source "t1" "Task One")
            task {:uuid "t1" :source-path file-path :status "incoming"}]
        (.writeFileSync fs file-path before "utf8")
        (-> (writeback/write-task-status task dir "in_progress" "wid-123")
            (.then (fn [updated]
                     (let [raw (.readFileSync fs file-path "utf8")
                           parsed (content-parser/parse-task-content raw)
                           comments (filter #(= "comment" (:type %)) (:sections parsed))]
                       (is (= "in_progress" (:status updated)))
                       (is (= "in_progress" (get-in parsed [:frontmatter :status])))
                       (is (= "wid-123" (get-in parsed [:frontmatter :write-id])))
                       (is (= (-> before
                                  (str/replace "status: \"incoming\"" "status: \"in_progress\"")
                                  (str/replace "\n---\n\n#" "\nwrite-id: \"wid-123\"\n---\n\n#"))
                              raw))
                       (is (some #(re-find #"status: should be preserved" (:content %)) comments)))))
            (.catch (fn [error] (is false (str "Unexpected writeback failure: " error))))
            (.finally (fn []
                        (.rmSync fs dir #js {:recursive true :force true})
                        (done))))))))

#_{:clj-kondo/ignore [:promise-chain/prefer-async-workflow]}
(deftest write-task-status-refuses-before-writing
  (testing "invalid YAML or update values leave the on-disk source byte-identical"
    (async done
      (let [dir (tmp-dir)
            file-path (path/join dir "invalid.md")
            task {:uuid "invalid" :source-path file-path :status "incoming"}
            cases [["---\nstatus: incoming\nstatus: ready\n---\nBody  \n" "done"]
                   ["---\nmetadata: [unterminated\n---\nBody  \n" "done"]
                   ["---\nstatus: !!int 3\n---\nBody  \n" "done"]
                   ["---\nstatus: !!bool true\n---\nBody  \n" "done"]
                   ["---\nstatus: !!set {a: null}\n---\nBody  \n" "done"]
                   ["---\nstatus: !!omap [{a: 3}]\n---\nBody  \n" "done"]
                   ["---\nstatus: !!pairs [{a: 3}]\n---\nBody  \n" "done"]
                   ["---\nstatus: incoming\nmetadata: &self {next: *self}\n---\nBody  \n" "done"]
                   ["---\nstatus: incoming\nmetadata: &self [*self]\n---\nBody  \n" "done"]
                   ["---\nstatus: incoming\n---\nBody  \n" {"invalid" "value"}]]]
        (-> (reduce (fn [pending [raw status]]
                      (.then pending
                             (fn []
                               (.writeFileSync fs file-path raw "utf8")
                               (-> (writeback/write-task-status task dir status "refused-write")
                                   (.then (fn [] (is false "invalid input must refuse the write")))
                                   (.catch (fn [_]
                                             (is (= raw (.readFileSync fs file-path "utf8"))
                                                 "refusal leaves the original file intact")))))))
                    (js/Promise.resolve) cases)
            (.catch (fn [error] (is false (str "Unexpected fixture failure: " error))))
            (.finally (fn []
                        (.rmSync fs dir #js {:recursive true :force true})
                        (done))))))))

#_{:clj-kondo/ignore [:promise-chain/prefer-async-workflow]}
(deftest write-task-status-retains-leading-bom-before-inserted-frontmatter
  (async done
    (let [dir (tmp-dir)
          file-path (path/join dir "bom.md")
          body "# Heading\r\n\r\nBody  \r\n"
          raw (str "\uFEFF" body)]
      (.writeFileSync fs file-path raw "utf8")
      (-> (writeback/write-task-status {:uuid "bom" :source-path file-path} dir "done" "bom-write")
          (.then (fn [_]
                   (let [updated (.readFileSync fs file-path "utf8")]
                     (is (= (str "\uFEFF---\nstatus: \"done\"\nwrite-id: \"bom-write\"\n---\n\n" body)
                            updated))
                     (is (= (str "\n" body) (:content (content-parser/parse-frontmatter updated)))))))
          (.catch (fn [error] (is false (str "Unexpected writeback failure: " error))))
          (.finally (fn []
                      (.rmSync fs dir #js {:recursive true :force true})
                      (done)))))))
