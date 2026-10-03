(ns rheos.backend.shape.content-parser-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            ["yaml" :as yaml]
            [rheos.backend.infra.content-parser :as parser]
            [rheos.backend.shape.content-parser :as content-shape]))

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

(deftest frontmatter-read-refuses-an-unclosed-opening-fence
  (testing "opening frontmatter without a closing delimiter is never plain Markdown"
    (doseq [raw ["---\nuuid: broken\n# Body without a closing delimiter\n"
                 "--- \t\nuuid: broken\ntext ---\n"
                 "\uFEFF---\r\nuuid: broken\r\n# Body\r\n"]]
      (is (thrown-with-msg? cljs.core/ExceptionInfo #"Unterminated YAML frontmatter"
                           (parser/parse-frontmatter raw)))))
  (testing "frontmatter-free Markdown, including body examples, remains byte-identical"
    (doseq [raw ["# Heading\n\nBody  \n"
                 "# Example\n\n```yaml\n---\nuuid: example\n---\n```\n"
                 "---"]]
      (is (= {:frontmatter {} :content raw} (parser/parse-frontmatter raw)))))
  (testing "BOM and CRLF opening delimiters still accept a complete header"
    (is (= {:frontmatter {:uuid "complete"} :content "\r\nBody  \r\n"}
           (parser/parse-frontmatter
            "\uFEFF--- \t\r\nuuid: complete\r\n---\r\n\r\nBody  \r\n")))))

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

(deftest structured-frontmatter-retains-scalar-types
  (testing "legacy scalar spelling is retained only at the top level"
    (let [raw "---\nuuid: 001\npoints: 3.0\nflag: TRUE\nempty:\nnull-text: null\nmetadata:\n  count: 3\n  active: true\n  missing: null\n  values: [3, false, null, \"null\"]\n---\nBody\n"
          frontmatter (:frontmatter (parser/parse-frontmatter raw))]
      (is (= {:uuid "001" :points "3.0" :flag "TRUE" :empty "" :null-text "null"}
             (select-keys frontmatter [:uuid :points :flag :empty :null-text])))
      (is (= {:count 3 :active true :missing nil :values [3 false nil "null"]}
             (:metadata frontmatter)))))
  (testing "accepted typed vector updates read back with the written types"
    (let [updated (parser/update-frontmatter "---\nstatus: incoming\n---\nBody\n"
                                            "values" [3 true nil [false 2.5]])]
      (is (= [3 true nil [false 2.5]]
             (get-in (parser/parse-frontmatter updated) [:frontmatter :values]))))))

(deftest tagged-extension-data-remains-clojure-shaped
  (let [raw "---\nmetadata:\n  set: !!set {a: null}\n  ordered: !!omap [{b: 2}]\n  timestamp: !!timestamp 2026-10-03\n  binary: !!binary SGVsbG8=\n  custom: !app {count: 3, active: true}\n---\nBody\n"
        metadata (get-in (parser/parse-frontmatter raw) [:frontmatter :metadata])]
    (is (= #{"a"} (:set metadata)))
    (is (= {:b 2} (:ordered metadata)))
    (is (= "2026-10-03T00:00:00.000Z" (:timestamp metadata)))
    (is (= [72 101 108 108 111] (:binary metadata)))
    (is (= {:count 3 :active true} (:custom metadata)))))

(deftest frontmatter-aliases-refuse-cycles-and-support-sharing
  (testing "cyclic aliases produce a deterministic refusal, not stack overflow"
    (doseq [source ["metadata: &self {next: *self}\n"
                    "metadata: &self [*self]\n"
                    "metadata: &outer {next: &inner {parent: *outer}}\n"]]
      (let [raw (str "---\nstatus: incoming\n" source "---\nBody  \n")]
        (doseq [operation [#(parser/parse-frontmatter raw)
                           #(parser/update-frontmatter raw "status" "done")]]
          (let [error (try (operation) nil (catch :default e e))]
            (is (= :cyclic-alias (:type (ex-data error)))))))))
  (testing "acyclic shared mappings remain supported and preserve source"
    (let [raw "---\nstatus: incoming\nmetadata: &shared {count: 3, active: true}\ncopy: *shared\n---\nBody  \n"
          updated (parser/update-frontmatter raw "status" "done")
          frontmatter (:frontmatter (parser/parse-frontmatter updated))]
      (is (= (str/replace raw "status: incoming" "status: \"done\"") updated))
      (is (= {:count 3 :active true} (:metadata frontmatter) (:copy frontmatter))))))

(deftest test-parse-sections
  (testing "parses single body section"
    (let [content "\n# Heading\nBody text"
          sections (content-shape/parse-sections content)]
      (is (= 1 (count sections)))
      (is (= "body" (:type (first sections))))
      (is (= "# Heading\nBody text" (:content (first sections))))))

  (testing "parses body and comment sections"
    (let [content "\nBody text\n---\nComment text\n---\nMore body"
          sections (content-shape/parse-sections content)]
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
          result (content-shape/serialize-frontmatter fm)]
      (is (re-find #"uuid: \"test\"" result))
      (is (re-find #"title: \"Test\"" result))))

  (testing "serializes arrays"
    (let [fm {:labels ["epics" "cljs"]}
          result (content-shape/serialize-frontmatter fm)]
      (is (re-find #"labels: \[\"epics\", \"cljs\"\]" result))))

  (testing "serializes plain values"
    (let [fm {:status "done" :priority "P0"}
          result (content-shape/serialize-frontmatter fm)]
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

(deftest frontmatter-appends-follow-existing-yaml-line-endings
  (testing "the delimiter style does not replace the YAML entries' style"
    (doseq [[opening-end yaml-end] [["\r\n" "\n"] ["\n" "\r\n"]]]
      (let [body "\nBody  \r\n\n```yaml\r\n---\nexample: value\r\n---\n```\r\n"
            raw (str "---" opening-end "# kept" yaml-end
                     "uuid: mixed" yaml-end "status: incoming" yaml-end
                     "---" opening-end body)
            updated (-> raw
                        (parser/update-frontmatter-keys {"status" "done" "priority" "P0"})
                        (parser/inject-write-id "mixed-write"))
            expected (str "---" opening-end "# kept" yaml-end
                          "uuid: mixed" yaml-end "status: \"done\"" yaml-end
                          "priority: \"P0\"" yaml-end "write-id: \"mixed-write\"" yaml-end
                          "---" opening-end body)]
        (is (= expected updated))
        (is (= body (:content (parser/parse-frontmatter updated)))))))
  (testing "empty YAML falls back to the opening delimiter's line ending"
    (is (= "---\r\npriority: \"P0\"\r\n---\nBody  \r\n"
           (parser/update-frontmatter "---\r\n---\nBody  \r\n" "priority" "P0")))))

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

(deftest frontmatter-insertion-retains-leading-bom
  (let [body "# Heading\r\n\r\nBody  \r\n"
        raw (str "\uFEFF" body)
        updated (-> raw
                    (parser/update-frontmatter "status" "done")
                    (parser/inject-write-id "bom-write"))]
    (is (= (str "\uFEFF---\nstatus: \"done\"\nwrite-id: \"bom-write\"\n---\n\n" body)
           updated))
    (is (= (str "\n" body) (:content (parser/parse-frontmatter updated))))))

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

(deftest comment-appends-preserve-the-original-frontmatter-source
  (doseq [fields ["title: 'Quotes \"and\" C:\\work'\r\n"
                  "title: Complex\r\nsummary: |\r\n  Line one\r\n  Line two\r\n"]]
    (let [header (str "\uFEFF--- \t\r\n# Retained YAML comment\r\nuuid: complex\r\n"
                      fields "metadata: &shared\r\n  values: [3, true, null]\r\n"
                      "copy: *shared\r\n--- \r\n")
          raw (str header "\r\nBody  \r\n")
          expected-frontmatter (:frontmatter (parser/parse-frontmatter raw))
          appended (try (parser/append-comment raw "First comment") (catch :default err err))
          again (try (parser/append-comment appended "Second comment") (catch :default err err))]
      (doseq [[result text] [[appended "First comment"]
                            [again "First comment\n\nSecond comment"]]]
        (is (and (string? result) (str/starts-with? result header))
            "comment append does not reserialize the YAML header")
        (let [parsed (try (parser/parse-task-content result) (catch :default err err))]
          (is (= expected-frontmatter (:frontmatter parsed)))
          (is (= [{:type "body" :content "Body"} {:type "comment" :content text}]
                 (:sections parsed))))))))

(deftest test-comment-section-roundtrip
  (testing "serialization keeps a following body outside the comment block"
    (let [sections [{:type "body" :content "Before the comment."}
                    {:type "comment" :content "First comment.\nContinued paragraph."}
                    {:type "body" :content "After the comment."}]]
      (is (= sections
             (content-shape/parse-sections (content-shape/serialize-sections sections))))))
  (testing "first and second appends preserve comment text and section identity"
    (let [raw "---\nuuid: test\n---\n\nBody paragraph."
          first-comment "First comment.\nContinued paragraph."
          second-comment "Second comment."
          first-append (parser/append-comment raw first-comment)
          second-append (parser/append-comment first-append second-comment)]
      (doseq [[result text] [[first-append first-comment]
                            [second-append (str first-comment "\n\n" second-comment)]]]
        (let [parsed (parser/parse-task-content result)]
          (is (= [{:type "body" :content "Body paragraph."}
                  {:type "comment" :content text}]
                 (:sections parsed)))
          (is (= parsed
                 (parser/parse-task-content
                   (content-shape/serialize-task-content parsed)))))))))

(deftest test-roundtrip
  (testing "parse then serialize preserves data"
    (let [raw "---\nuuid: \"test\"\ntitle: \"Test\"\nstatus: done\npriority: P0\nlabels: [\"epics\", \"cljs\"]\n---\n\n# Title\n\nBody content"
          parsed (parser/parse-task-content raw)
          serialized (content-shape/serialize-task-content parsed)
          re-parsed (parser/parse-task-content serialized)]
      (is (= (get-in parsed [:frontmatter :uuid]) (get-in re-parsed [:frontmatter :uuid])))
      (is (= (get-in parsed [:frontmatter :title]) (get-in re-parsed [:frontmatter :title])))
      (is (= (get-in parsed [:frontmatter :status]) (get-in re-parsed [:frontmatter :status])))
      (is (= (get-in parsed [:frontmatter :labels]) (get-in re-parsed [:frontmatter :labels]))))))

(deftest ordered-map-cyclic-aliases-refuse-reads-and-updates
  (let [source "status: incoming\nmetadata: !!omap &self\n  - self: *self\n"
        raw (str "---\n" source "---\nBody  \n")
        ^js document (yaml/parseDocument source #js {:stringKeys true :schema "core"})
        ^js native (.toJS document #js {:maxAliasCount 100})
        ^js metadata (.-metadata native)]
    (testing "the accepted standard tag resolves to an actual native Map cycle"
      (is (empty? (seq (.-errors document))))
      (is (instance? js/Map metadata))
      (is (identical? metadata (.get metadata "self"))))
    (testing "reads and low-level updates refuse that cycle with its declared type"
      (doseq [operation [#(parser/parse-frontmatter raw)
                         #(parser/parse-task-content raw)
                         #(parser/update-frontmatter raw "status" "done")]]
        (let [error (try (operation) nil (catch :default e e))]
          (is (= :cyclic-alias (:type (ex-data error)))))))))

(deftest ordered-map-shared-aliases-remain-readable-and-editable
  (let [source "status: incoming\nmetadata: !!omap &shared\n  - a: &values [one, two]\n  - b: *values\ncopy: *shared\n"
        raw (str "---\n" source "---\nBody  \n")
        ^js document (yaml/parseDocument source #js {:stringKeys true :schema "core"})
        ^js native (.toJS document #js {:maxAliasCount 100})
        ^js metadata (.-metadata native)
        expected {:a ["one" "two"] :b ["one" "two"]}
        parsed (parser/parse-frontmatter raw)
        updated (parser/update-frontmatter raw "status" "done")
        reparsed (parser/parse-frontmatter updated)]
    (testing "the acyclic fixture shares both a native Map and its nested values"
      (is (empty? (seq (.-errors document))))
      (is (instance? js/Map metadata))
      (is (identical? metadata (.-copy native)))
      (is (identical? (.get metadata "a") (.get metadata "b"))))
    (testing "shared values remain Clojure-shaped before and after a targeted edit"
      (is (= expected (get-in parsed [:frontmatter :metadata])
             (get-in parsed [:frontmatter :copy])))
      (is (= (str/replace raw "status: incoming" "status: \"done\"") updated))
      (is (= "done" (get-in reparsed [:frontmatter :status])))
      (is (= expected (get-in reparsed [:frontmatter :metadata])
             (get-in reparsed [:frontmatter :copy])))
      (is (= "Body  \n" (:content parsed) (:content reparsed))))))
