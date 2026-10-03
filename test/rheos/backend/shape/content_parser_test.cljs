(ns rheos.backend.shape.content-parser-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["yaml" :as yaml]
            [rheos.backend.shape.content-parser :as parser]))

(deftest test-parse-frontmatter
  (testing "parses quoted string values"
    (let [raw "---\nuuid: \"test-uuid\"\ntitle: \"Test Title\"\n---\nBody content"
          result (parser/parse-frontmatter raw)]
      (is (= "test-uuid" (get-in result [:frontmatter :uuid])))
      (is (= "Test Title" (get-in result [:frontmatter :title])))
      (is (= "Body content" (:content result)))))

  (testing "parses unquoted values"
    (let [raw "---\nstatus: done\npriority: P0\n---\nBody"
          result (parser/parse-frontmatter raw)]
      (is (= "done" (get-in result [:frontmatter :status])))
      (is (= "P0" (get-in result [:frontmatter :priority])))))

  (testing "parses array values"
    (let [raw "---\nlabels: [\"epics\", \"cljs\", \"kanban\"]\n---\nBody"
          result (parser/parse-frontmatter raw)]
      (is (= ["epics" "cljs" "kanban"] (get-in result [:frontmatter :labels])))))

  (testing "parses empty values"
    (let [raw "---\ncategory:\n---\nBody"
          result (parser/parse-frontmatter raw)]
      (is (= "" (get-in result [:frontmatter :category])))))

  (testing "returns empty frontmatter when no match"
    (let [raw "No frontmatter here"
          result (parser/parse-frontmatter raw)]
      (is (= {} (:frontmatter result)))
      (is (= "No frontmatter here" (:content result))))))

(deftest source-preserving-updates-remain-readable
  (testing "updated values retain YAML meaning despite preserved comments and aliases"
    (let [raw "---\nuuid: test\nstatus: &workflow incoming # keep\ncategory: *workflow\npoints: 3\nflag: false\nempty:\n---\n\nBody\n"
          updated (parser/update-frontmatter raw "status" "done")
          frontmatter (:frontmatter (parser/parse-frontmatter updated))]
      (is (= "done" (:status frontmatter)))
      (is (= "done" (:category frontmatter)))
      (is (= "3" (:points frontmatter)) "flat numeric fields keep their string convention")
      (is (= "false" (:flag frontmatter)) "flat boolean fields keep their string convention")
      (is (= "" (:empty frontmatter)))))

  (testing "reading preserves full body spacing and decodes quoted/nested values"
    (let [raw "---\n\"title\": 'Research'\nmetadata:\n  owner: Someone\nsummary: |\n  Line one\n  Line two\n---\n\nBody  \n\n"
          parsed (parser/parse-frontmatter raw)]
      (is (= "Research" (get-in parsed [:frontmatter :title])))
      (is (= "Someone" (get-in parsed [:frontmatter :metadata :owner])))
      (is (= "Line one\nLine two\n" (get-in parsed [:frontmatter :summary])))
      (is (= "\nBody  \n\n" (:content parsed))))))

(deftest test-parse-sections
  (testing "parses single body section"
    (let [content "\n# Heading\nBody text"
          sections (parser/parse-sections content)]
      (is (= 1 (count sections)))
      (is (= "body" (:type (first sections))))
      (is (= "# Heading\nBody text" (:content (first sections))))))

  (testing "parses body and comment sections"
    (let [content "\nBody text\n---\nComment text\n---\nMore body"
          sections (parser/parse-sections content)]
      (is (= 3 (count sections)))
      (is (= "body" (:type (nth sections 0))))
      (is (= "comment" (:type (nth sections 1))))
      (is (= "body" (:type (nth sections 2)))))))

(deftest test-parse-task-content
  (testing "parses complete task file"
    (let [raw "---\nuuid: \"test\"\ntitle: \"Test\"\nstatus: done\npriority: P0\nlabels: [\"epics\", \"cljs\"]\n---\n\n# Title\n\nBody content"
          result (parser/parse-task-content raw)]
      (is (= "test" (get-in result [:frontmatter :uuid])))
      (is (= "Test" (get-in result [:frontmatter :title])))
      (is (= "done" (get-in result [:frontmatter :status])))
      (is (= "P0" (get-in result [:frontmatter :priority])))
      (is (= ["epics" "cljs"] (get-in result [:frontmatter :labels])))
      (is (pos? (count (:sections result)))))))

(deftest test-serialize-frontmatter
  (testing "serializes quoted strings"
    (let [fm {:uuid "test" :title "Test"}
          result (parser/serialize-frontmatter fm)]
      (is (re-find #"uuid: \"test\"" result))
      (is (re-find #"title: \"Test\"" result))))

  (testing "serializes arrays"
    (let [fm {:labels ["epics" "cljs"]}
          result (parser/serialize-frontmatter fm)]
      (is (re-find #"labels: \[\"epics\", \"cljs\"\]" result))))

  (testing "serializes plain values"
    (let [fm {:status "done" :priority "P0"}
          result (parser/serialize-frontmatter fm)]
      (is (re-find #"status: \"done\"" result))
      (is (re-find #"priority: \"P0\"" result)))))

(deftest test-update-frontmatter
  (testing "updates a frontmatter field"
    (let [raw "---\nuuid: \"test\"\nstatus: \"incoming\"\n---\n\nBody"
          result (parser/update-frontmatter raw "status" "done")]
      (is (re-find #"status: \"done\"" result))
      (is (re-find #"uuid: \"test\"" result)))))

(deftest test-inject-write-id
  (testing "adds write-id to frontmatter"
    (let [raw "---\nuuid: \"test\"\nstatus: \"incoming\"\n---\n\nBody"
          result (parser/inject-write-id raw "wid-123")]
      (is (re-find #"write-id: \"wid-123\"" result))
      (is (re-find #"uuid: \"test\"" result))
      (is (re-find #"status: \"incoming\"" result))))

  (testing "updates existing write-id"
    (let [raw "---\nuuid: \"test\"\nwrite-id: \"old\"\n---\n\nBody"
          result (parser/inject-write-id raw "new")]
      (is (re-find #"write-id: \"new\"" result))
      (is (not (re-find #"write-id: \"old\"" result)))))

  (testing "preserves body and sections"
    (let [raw "---\nuuid: \"test\"\n---\n\n# Title\n\nBody\n---\nComment\n---"
          result (parser/inject-write-id raw "wid-abc")]
      (is (re-find #"write-id: \"wid-abc\"" result))
      (is (re-find #"# Title" result))
      (is (re-find #"Body" result))
      (is (re-find #"Comment" result)))))

(deftest test-update-frontmatter-multiple
  (testing "updates several frontmatter fields at once"
    (let [raw "---\nuuid: \"test\"\nstatus: \"incoming\"\n---\n\nBody"
          result (parser/update-frontmatter-keys raw {"status" "done" "priority" "P0"})
          parsed (parser/parse-task-content result)]
      (is (= "done" (get-in parsed [:frontmatter :status])))
      (is (= "P0" (get-in parsed [:frontmatter :priority]))))))

(deftest frontmatter-updates-preserve-source
  (testing "updating one scalar preserves unrelated YAML and every body byte"
    (let [raw (str "---\n"
                   "# retained metadata\n"
                   "uuid: 'test'\n"
                   "status: incoming # workflow\n"
                   "metadata:\n  author: Someone\n  links:\n    - docs/a.md\n"
                   "summary: |\n  Line one\n  Line two\n"
                   "---\n\n# Document\n\nParagraph.  \n\n"
                   "```yaml\n---\nstatus: example\n---\n```\n\n")
          expected (str "---\n"
                        "# retained metadata\n"
                        "uuid: 'test'\n"
                        "status: \"done\" # workflow\n"
                        "metadata:\n  author: Someone\n  links:\n    - docs/a.md\n"
                        "summary: |\n  Line one\n  Line two\n"
                        "---\n\n# Document\n\nParagraph.  \n\n"
                        "```yaml\n---\nstatus: example\n---\n```\n\n")]
      (is (= expected (parser/update-frontmatter raw "status" "done")))))

  (testing "adding a field and a write-id preserves CRLF, delimiters and body spacing"
    (let [raw "---  \r\nuuid: test\r\nsummary: >-\r\n  Keep this\r\n  folded value\r\n--- \r\n\r\nBody  \r\n\r\n"
          expected "---  \r\nuuid: test\r\nsummary: >-\r\n  Keep this\r\n  folded value\r\npriority: \"P0\"\r\nwrite-id: \"wid-123\"\r\n--- \r\n\r\nBody  \r\n\r\n"]
      (is (= expected (-> raw
                          (parser/update-frontmatter "priority" "P0")
                          (parser/inject-write-id "wid-123")))))))

(deftest frontmatter-update-edge-cases
  (testing "empty updates leave source byte-identical"
    (let [raw "---\nmetadata:\n  nested: true\n---\n\nBody  \n"]
      (is (= raw (parser/update-frontmatter-keys raw {})))))

  (testing "ordinary Markdown receives frontmatter without normalizing its body"
    (let [raw "# Heading\n\n```yaml\n---\nexample: value\n---\n```\n\n"]
      (is (= (str "---\npriority: \"P0\"\n---\n\n" raw)
             (parser/update-frontmatter raw "priority" "P0")))))

  (testing "an empty frontmatter block remains distinct from the body"
    (is (= "---\npriority: \"P0\"\n---\n\nBody\n"
           (parser/update-frontmatter "---\n---\n\nBody\n" "priority" "P0"))))

  (testing "an existing write-id changes without rewriting the surrounding source"
    (is (= "---\nuuid: test\nwrite-id: \"new\" # correlation\n---\nBody  \n"
           (parser/inject-write-id "---\nuuid: test\nwrite-id: \"old\" # correlation\n---\nBody  \n" "new")))))

(deftest frontmatter-update-replaces-value-shapes
  (testing "block values can become scalars or flow lists without consuming the next field"
    (is (= "---\ndescription: \"Replacement\" # keep\n# next field\nlabels: [\"one\",\"two\"]\npriority: P0\n---\nBody\n"
           (parser/update-frontmatter-keys
             "---\ndescription: | # keep\n  Old first\n  Old second\n# next field\nlabels:\n  - old\npriority: P0\n---\nBody\n"
             {"description" "Replacement" "labels" ["one" "two"]}))))

  (testing "empty values retain their inline comment"
    (is (= "---\ncategory: \"work\" # keep\n---\nBody\n"
           (parser/update-frontmatter "---\ncategory: # keep\n---\nBody\n" "category" "work"))))

  (testing "quoted keys and escaped replacement values remain valid YAML"
    (let [result (parser/update-frontmatter "---\n\"title\": Old\npriority: P0\n---\nBody\n"
                                             "title" "Quotes \"and\" a newline\nNext line")
          frontmatter (second (re-matches #"---\n([\s\S]*?)\n---\n[\s\S]*" result))]
      (is (= "Quotes \"and\" a newline\nNext line" (.-title (yaml/parse frontmatter))))
      (is (str/ends-with? result "priority: P0\n---\nBody\n"))))

  (testing "empty values without a space after the colon become valid scalars"
    (is (= "---\nsummary: \"New summary\"\nstatus: \"done\"\n---\nBody\n"
           (parser/update-frontmatter-keys "---\nsummary:\nstatus:\n---\nBody\n"
                                           {"summary" "New summary" "status" "done"}))))

  (testing "an anchored scalar retains its anchor and an alias can be replaced independently"
    (let [raw "---\nstatus: &workflow incoming\ncategory: *workflow\n---\nBody\n"
          result (parser/update-frontmatter raw "status" "done")
          alias-result (parser/update-frontmatter raw "category" "work")]
      (is (= "---\nstatus: &workflow \"done\"\ncategory: *workflow\n---\nBody\n" result))
      (is (= "---\nstatus: &workflow incoming\ncategory: \"work\"\n---\nBody\n" alias-result))
      (is (= "done" (.-category (yaml/parse "status: &workflow \"done\"\ncategory: *workflow\n"))))))

  (testing "a closing delimiter at EOF is preserved"
    (is (= "---\nstatus: \"done\"\n---"
           (parser/update-frontmatter "---\nstatus: incoming\n---" "status" "done")))))

(deftest frontmatter-update-refuses-ambiguous-source
  (doseq [raw ["---\nstatus: incoming\nstatus: ready\n---\nBody\n"
               "---\nmetadata: [unterminated\n---\nBody\n"
               "---\nstatus: incoming\nBody without closing delimiter\n"
               "---\nstatus: *missing\n---\nBody\n"
               "---\n{status: incoming}\n---\nBody\n"]]
    (testing (str "refuses invalid or unsupported source: " raw)
      (is (thrown? js/Error (parser/update-frontmatter raw "status" "done"))))))

(deftest frontmatter-update-refuses-invalid-updates
  (doseq [updates [["status" "done"]
                   {"status\ninjected" "done"}
                   {:workflow/status "done"}
                   {:status "done" "status" "ready"}
                   {"title" {"nested" "value"}}
                   {"points" js/NaN}]]
    (testing (str "refuses invalid updates: " updates)
      (is (thrown? js/Error
                   (parser/update-frontmatter-keys "---\nstatus: incoming\n---\nBody\n" updates)))))

  (testing "body delimiters are not treated as frontmatter"
    (let [raw "Introduction\n\n---\nstatus: example\n---\nBody\n"]
      (is (= (str "---\npriority: \"P0\"\n---\n\n" raw)
             (parser/update-frontmatter raw "priority" "P0"))))))

(deftest frontmatter-update-honors-explicit-tags
  (testing "replacement values must satisfy retained standard YAML tags"
    (is (thrown? js/Error
                 (parser/update-frontmatter "---\ndescription: !!int 3\n---\nBody  \n"
                                            "description" "new")))
    (is (thrown? js/Error
                 (parser/update-frontmatter "---\nstatus: !!bool true\n---\nBody  \n"
                                            "status" "done"))))

  (testing "unrelated custom tags survive alongside valid standard tags"
    (doseq [tag ["!custom" "!<tag:example.com,2026:content>"]]
      (let [raw (str "---\nmetadata: " tag " retained\npoints: !!int 3\nstatus: incoming\n---\nBody  \n")
            expected (str "---\nmetadata: " tag " retained\npoints: !!int 3\nstatus: \"done\"\n---\nBody  \n")]
        (is (= expected (parser/update-frontmatter raw "status" "done")))))))

(deftest frontmatter-update-honors-standard-collection-tags
  (testing "valid standard tagged collections survive unrelated updates"
    (doseq [[tag value] [["!!set" "{a: null}"]
                        ["!!omap" "[{a: 3}]"]
                        ["!!pairs" "[{a: 3}]"]]]
      (let [raw (str "---\nmetadata: " tag " " value "\nstatus: incoming\n---\nBody  \n")
            expected (str "---\nmetadata: " tag " " value "\nstatus: \"done\"\n---\nBody  \n")]
        (is (= expected (parser/update-frontmatter raw "status" "done"))))))

  (testing "replacements must satisfy retained standard collection tags"
    (doseq [[tag value] [["!!set" "{a: null}"]
                        ["!!omap" "[{a: 3}]"]
                        ["!!pairs" "[{a: 3}]"]]]
      (is (thrown? js/Error
                   (parser/update-frontmatter
                     (str "---\nmetadata: " tag " " value "\n---\nBody  \n")
                     "metadata" "new"))))
    ;; The library reports this map/sequence mismatch separately from an
    ;; unresolved scalar tag; both diagnostics must refuse the replacement.
    (is (thrown? js/Error
                 (parser/update-frontmatter "---\nmetadata: !!set {a: null}\n---\nBody  \n"
                                            "metadata" ["new"])))))

(deftest test-append-comment-creates-section
  (testing "appends a comment block when none exists"
    (let [raw "---\nuuid: \"test\"\n---\n\nBody"
          result (parser/append-comment raw "First comment")
          parsed (parser/parse-task-content result)
          comments (filter #(= "comment" (:type %)) (:sections parsed))]
      (is (= 1 (count comments)))
      (is (= "First comment" (:content (first comments)))))))

(deftest test-append-comment-appends-to-last
  (testing "appends to the last comment section"
    (let [raw "---\nuuid: \"test\"\n---\n\nBody\n\n---\nExisting\n---"
          result (parser/append-comment raw "More")
          parsed (parser/parse-task-content result)
          comments (filter #(= "comment" (:type %)) (:sections parsed))]
      (is (= 1 (count comments)))
      (is (re-find #"Existing" (:content (first comments))))
      (is (re-find #"More" (:content (first comments)))))))

(deftest test-roundtrip
  (testing "parse then serialize preserves data"
    (let [raw "---\nuuid: \"test\"\ntitle: \"Test\"\nstatus: done\npriority: P0\nlabels: [\"epics\", \"cljs\"]\n---\n\n# Title\n\nBody content"
          parsed (parser/parse-task-content raw)
          serialized (parser/serialize-task-content parsed)
          re-parsed (parser/parse-task-content serialized)]
      (is (= (get-in parsed [:frontmatter :uuid]) (get-in re-parsed [:frontmatter :uuid])))
      (is (= (get-in parsed [:frontmatter :title]) (get-in re-parsed [:frontmatter :title])))
      (is (= (get-in parsed [:frontmatter :status]) (get-in re-parsed [:frontmatter :status])))
      (is (= (get-in parsed [:frontmatter :labels]) (get-in re-parsed [:frontmatter :labels]))))))
