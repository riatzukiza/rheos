(ns rheos.backend.infra.task-writeback-test
  (:require ["node:fs/promises" :as fsp]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :refer [deftest testing is]]
            [clojure.string :as str]
            [rheos.backend.infra.task-writeback :as writeback]
            [rheos.backend.shape.content-parser :as content-parser]))

(defn- tmp-dir []
  (path/join (.tmpdir os) (str "rheos-writeback-test-" (.now js/Date) "-" (rand-int 100000))))

(defn- write-task! [dir uuid title]
  (let [file-path (path/join dir (str uuid ".md"))
        raw (str "---\n"
                 "uuid: \"" uuid "\"\n"
                 "title: \"" title "\"\n"
                 "status: \"incoming\"\n"
                 "priority: \"P3\"\n"
                 "metadata:\n  owner: Someone\n  links:\n    - docs/a.md\n"
                 "summary: |\n  Line one\n  Line two\n"
                 "---\n\n# " title "\n\nBody  \n\n```yaml\n---\nstatus: example\n---\n```\n\n---\nstatus: should be preserved\n---\n")]
    (.writeFile fsp file-path raw "utf8")))

(deftest ^:async write-task-status-updates-frontmatter-only
  (testing "Status writeback only touches YAML frontmatter, not body/comment status lines"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t1" "Task One"))
          file-path (path/join dir "t1.md")
          task {:uuid "t1" :source-path file-path :status "incoming"}
          before (await (.readFile fsp file-path "utf8"))]
      (try
        (let [updated (await (writeback/write-task-status task dir "in_progress" "wid-123"))
              raw (await (.readFile fsp file-path "utf8"))
              parsed (content-parser/parse-task-content raw)
              comments (filter #(= "comment" (:type %)) (:sections parsed))]
          (is (= "in_progress" (:status updated)))
          (is (= "in_progress" (get-in parsed [:frontmatter :status])))
          (is (= "wid-123" (get-in parsed [:frontmatter :write-id])))
          (is (= (-> before
                     (str/replace "status: \"incoming\"" "status: \"in_progress\"")
                     (str/replace "\n---\n\n#" "\nwrite-id: \"wid-123\"\n---\n\n#"))
                 raw))
          (is (some #(re-find #"status: should be preserved" (:content %)) comments)))
        (finally
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async write-task-status-refuses-before-writing
  (testing "invalid YAML or update values leave the on-disk source byte-identical"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          file-path (path/join dir "invalid.md")
          task {:uuid "invalid" :source-path file-path :status "incoming"}]
      (try
        (doseq [[raw status] [["---\nstatus: incoming\nstatus: ready\n---\nBody  \n" "done"]
                              ["---\nmetadata: [unterminated\n---\nBody  \n" "done"]
                              ["---\nstatus: incoming\n---\nBody  \n" {"invalid" "value"}]]]
          (await (.writeFile fsp file-path raw "utf8"))
          (try
            (await (writeback/write-task-status task dir status "refused-write"))
            (is false "invalid input must refuse the write")
            (catch :default _
              (is (= raw (await (.readFile fsp file-path "utf8")))
                  "refusal leaves the original file intact"))))
        (finally
          (await (.rm fsp dir #js {:recursive true :force true})))))))
