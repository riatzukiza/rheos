(ns rheos.backend.infra.task-edit-test
  (:require ["node:fs/promises" :as fsp]
            ["node:os" :as os]
            ["node:path" :as path]
            [cljs.test :refer [deftest testing is]]
            [clojure.string :as str]
            [rheos.backend.domain.events :as events]
            [rheos.backend.infra.task-edit :as task-edit]
            [rheos.backend.infra.task-store :as task-store]
            [rheos.backend.infra.content-parser :as content-parser]
            [rheos.backend.shape.content-parser :as content-shape]
            [rheos.backend.infra.watcher :as watcher]))

(defn- tmp-dir []
  (path/join (.tmpdir os) (str "rheos-test-" (.now js/Date) "-" (rand-int 100000))))

(defn- write-task! [dir uuid title]
  (let [file-path (path/join dir (str uuid ".md"))
        raw (str "---\n"
                 "uuid: \"" uuid "\"\n"
                 "title: \"" title "\"\n"
                 "status: \"incoming\"\n"
                 "priority: \"P3\"\n"
                 "---\n\n# " title "\n\nBody")]
    (.writeFile fsp file-path raw "utf8")))

(deftest ^:async collection-priority-edit-is-refused-before-write-or-event
  (let [dir (tmp-dir)
        _ (await (.mkdir fsp dir #js {:recursive true}))
        _ (await (write-task! dir "t1" "Task One"))
        project {:id "test" :tasks-dir dir}
        task {:uuid "t1" :source-path (path/join dir "t1.md")}
        before (await (.readFile fsp (:source-path task) "utf8"))
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (let [error (try (await (task-edit/update-frontmatter!
                               {:project project :task task :updates {:priority ["P0" "P1"]}}))
                       nil (catch :default error error))]
        (is (= :refused (:kind (ex-data error))))
        (is (= :priority (:field (ex-data error))))
        (is (= (:source-path task) (:source-path (ex-data error))))
        (is (= before (await (.readFile fsp (:source-path task) "utf8"))))
        (is (empty? @captured)))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async update-frontmatter-emits-events
  (testing "Updating frontmatter writes the file and records events"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t1" "Task One"))
          project {:id "test" :title "Test" :tasks-dir dir :meta {}}
          task {:uuid "t1" :source-path (path/join dir "t1.md")}
          captured (atom [])
          unsub (events/subscribe! #(swap! captured conj %))]
      (try
        (let [result (await (task-edit/update-frontmatter!
                             {:project project :task task
                              :updates {"priority" "P0"}
                              :source "test"}))
              raw (await (.readFile fsp (:source-path task) "utf8"))]
          (is (:ok result))
          (is (= "P0" (get-in result [:frontmatter :priority])))
          (is (re-find #"priority: \"P0\"" raw))
          (is (pos? (count (filter #(= "frontmatter" (:type %)) @captured))))
          (is (some #(= "P0" (:new-value %)) @captured)))
        (finally
          (unsub)
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async update-frontmatter-with-no-updates-writes-nothing
  (testing "An empty update leaves the file byte-identical and emits no event"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t1" "Task One"))
          project {:id "test" :title "Test" :tasks-dir dir :meta {}}
          task {:uuid "t1" :source-path (path/join dir "t1.md")
                :frontmatter {:uuid "t1" :priority "P3"}}
          before (await (.readFile fsp (:source-path task) "utf8"))
          captured (atom [])
          unsub (events/subscribe! #(swap! captured conj %))]
      (try
        (let [result (await (task-edit/update-frontmatter!
                             {:project project :task task :updates {} :source "test"}))
              after (await (.readFile fsp (:source-path task) "utf8"))]
          (is (:ok result))
          (is (:noop result) "an empty update reports itself as a no-op")
          (is (= before after) "the file is not rewritten with a fresh write-id")
          (is (empty? @captured) "and nothing is recorded that did not happen"))
        (finally
          (unsub)
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async update-frontmatter-injects-write-id
  (testing "Frontmatter update injects write-id and watcher can correlate"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t1" "Task One"))
          project {:id "test" :title "Test" :tasks-dir dir :meta {}}
          task {:uuid "t1" :source-path (path/join dir "t1.md")}
          captured (atom [])
          unsub (events/subscribe! #(swap! captured conj %))]
      (try
        (let [result (await (task-edit/update-frontmatter!
                             {:project project :task task
                              :updates {"priority" "P0"}
                              :source "test"}))
              raw (await (.readFile fsp (:source-path task) "utf8"))
              frontmatter (:frontmatter (content-parser/parse-task-content raw))]
          (is (:ok result))
          (is (string? (:write-id frontmatter)))
          (is (pos? (count (:write-id frontmatter))))
          (let [write-id (:write-id frontmatter)]
            (await (watcher/handle-file-event! "test" dir (:source-path task) "change"))
            (is (some #(and (= "file-changed" (:type %))
                            (= write-id (:write-id %))
                            (= "correlated" (:correlation/status %)))
                      @captured))))
        (finally
          (unsub)
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async title-updates-refuse-collections-before-writing-or-emitting-events
  (let [dir (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-title-update-")))
        task-path (path/join dir "typed.md")
        raw "---\nuuid: typed\ntitle: Original\nstatus: incoming\nmetadata:\n  values: [3, true, null]\n---\n\nBody  \n"
        project {:id "test" :tasks-dir dir :meta {}}
        task {:uuid "typed" :source-path task-path}
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (await (.writeFile fsp task-path raw "utf8"))
      (doseq [value [["wrong"] [] [7 false nil]]]
        (let [error (try (await (task-edit/update-frontmatter!
                                {:project project :task task :updates {"title" value}}))
                         nil (catch :default err err))]
          (is (= :refused (:kind (ex-data error))))
          (is (= :title (:field (ex-data error))))
          (is (= task-path (:source-path (ex-data error))))
          (is (and error (str/includes? (.-message error) "Task title must be a string")))
          (is (= raw (await (.readFile fsp task-path "utf8"))))
          (is (empty? @captured) "a refused edit emits no mutation event")))
      (doseq [[value expected] [["Quotes \"and\" C:\\work\nNext line" "Quotes \"and\" C:\\work\nNext line"]
                                [7 "7"] [false "false"] [nil ""]]]
        (let [before (await (.readFile fsp task-path "utf8"))
              before-title (get-in (content-parser/parse-frontmatter before) [:frontmatter :title])
              previous-event-count (count @captured)
              result (await (task-edit/update-frontmatter!
                            {:project project :task task :updates {"title" value}}))
              after (await (.readFile fsp task-path "utf8"))
              parsed (:frontmatter (content-parser/parse-frontmatter after))
              loaded (await (task-store/load-tasks dir))
              event (nth @captured previous-event-count nil)]
          (is (:ok result))
          (is (= expected (:title (first loaded))) "decoded scalar title spelling remains compatible")
          (is (= {:values [3 true nil]} (:metadata parsed)))
          (is (= (inc previous-event-count) (count @captured)))
          (is (= "frontmatter" (:type event)))
          (is (= "title" (:key event)))
          (is (= before-title (:old-value event)))
          (is (= value (:new-value event)))
          (is (= (:write-id parsed) (:write-id event)))
          (is (str/ends-with? after "---\n\nBody  \n"))))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async complex-frontmatter-comment-writes-remain-readable-and-ledger-backed
  (let [dir (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-complex-comment-")))
        task-path (path/join dir "complex.md")
        header (str "\uFEFF--- \t\r\nuuid: complex\r\n"
                    "title: 'Quotes \"and\" C:\\work'\r\nsummary: |\r\n  Line one\r\n  Line two\r\n"
                    "metadata: &shared\r\n  values: [3, true, null]\r\ncopy: *shared\r\n--- \r\n")
        body "    code begins here  \r\n\r\nBody  \r\n\tlast line\t \r\n\r\n"
        raw (str header body)
        project {:id "test" :tasks-dir dir :meta {}}
        task {:uuid "complex" :source-path task-path}
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (await (.writeFile fsp task-path raw "utf8"))
      (let [result (try (await (task-edit/append-comment!
                               {:project project :task task :text "Reviewed" :source "test"}))
                        (catch :default err err))
            after (await (.readFile fsp task-path "utf8"))
            parsed (content-parser/parse-task-content after)
            frontmatter (:frontmatter parsed)
            comment-events (filter #(= "comment" (:type %)) @captured)]
        (is (:ok result) "a valid complex card accepts the real comment write")
        (is (= "Quotes \"and\" C:\\work" (:title frontmatter)))
        (is (= "Line one\nLine two\n" (:summary frontmatter)))
        (is (= {:values [3 true nil]} (:metadata frontmatter) (:copy frontmatter)))
        (is (string? (:write-id frontmatter)))
        (is (str/starts-with? after
                             (str/replace-first header "--- \r\n"
                                                (str "write-id: " (js/JSON.stringify (:write-id frontmatter))
                                                     "\r\n--- \r\n")))
            "only the new write-id changes the original YAML header")
        (is (str/starts-with? (:body (content-shape/frontmatter-source after)) body)
            "the real write retains every existing body byte, including leading code indentation and CRLF")
        (is (= [{:type "body" :content "code begins here  \n\nBody  \n\tlast line"}
                {:type "comment" :content "Reviewed"}]
               (:sections parsed)))
        (is (= 1 (count comment-events)))
        (is (= "Reviewed" (:text (first comment-events))))
        (is (= (:write-id frontmatter) (:write-id (first comment-events))))
        (is (= "complex" (:uuid (first (await (task-store/load-tasks dir)))))))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async invalid-frontmatter-comment-is-refused-before-write-or-event
  (let [dir (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-refused-comment-")))
        task-path (path/join dir "invalid.md")
        project {:id "test" :tasks-dir dir :meta {}}
        task {:uuid "invalid" :source-path task-path}
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (doseq [field [:title :priority :status]
              value ["[one, two]" "{name: one}"]]
        (let [raw (str "---\r\nuuid: invalid\r\n" (name field) ": " value
                       "\r\nmetadata: {values: [7, false, null]}\r\n---\r\n"
                       "    code  \r\nBody\t \r\n")]
          (await (.writeFile fsp task-path raw "utf8"))
          (reset! captured [])
          (let [error (try (await (task-edit/append-comment!
                                   {:project project :task task :text "Reviewed" :source "test"}))
                           nil (catch :default error error))
                diagnostic (str "Task " (name field) " must be a string")]
            (is (= :refused (:kind (ex-data error))))
            (is (= field (:field (ex-data error))))
            (is (= task-path (:source-path (ex-data error))))
            (is (= diagnostic (:diagnostic (ex-data error))))
            (is (= (str "Refused card source " task-path ": " diagnostic)
                   (when error (ex-message error))))
            (is (= raw (await (.readFile fsp task-path "utf8")))
                "a refused comment retains every original source byte")
            (is (empty? @captured) "a refused comment emits no event"))))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async scalar-frontmatter-comments-retain-decoded-spelling-and-event
  (let [dir (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-scalar-comment-")))
        task-path (path/join dir "scalar.md")
        project {:id "test" :tasks-dir dir :meta {}}
        task {:uuid "scalar" :source-path task-path}
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (doseq [field [:title :priority :status]
              value ["7" "false" "null"]]
        (let [body "    code  \r\nBody\t \r\n"
              raw (str "---\r\nuuid: scalar\r\n" (name field) ": " value
                       "\r\nmetadata: {values: [7, false, null]}\r\n---\r\n" body)]
          (await (.writeFile fsp task-path raw "utf8"))
          (reset! captured [])
          (let [result (await (task-edit/append-comment!
                               {:project project :task task :text "Reviewed" :source "test"}))
                after (await (.readFile fsp task-path "utf8"))
                frontmatter (:frontmatter (content-parser/parse-task-content after))
                event (first @captured)]
            (is (:ok result))
            (is (= value (get frontmatter field)) "top-level scalar spelling remains a string")
            (is (= {:values [7 false nil]} (:metadata frontmatter)))
            (is (str/starts-with? (:body (content-shape/frontmatter-source after)) body))
            (is (= 1 (count @captured)))
            (is (= "comment" (:type event)))
            (is (= "Reviewed" (:text event)))
            (is (string? (:write-id frontmatter)))
            (is (= (:write-id frontmatter) (:write-id event))))))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))

(deftest ^:async append-comment-emits-event
  (testing "Appending a comment writes the file and records a comment event"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t2" "Task Two"))
          project {:id "test" :title "Test" :tasks-dir dir :meta {}}
          task {:uuid "t2" :source-path (path/join dir "t2.md")}
          captured (atom [])
          unsub (events/subscribe! #(swap! captured conj %))]
      (try
        (let [result (await (task-edit/append-comment!
                             {:project project :task task
                              :text "Looks good"
                              :source "test"}))
              raw (await (.readFile fsp (:source-path task) "utf8"))]
          (is (:ok result))
          (is (= "Looks good" (:text result)))
          (is (re-find #"Looks good" raw))
          (is (pos? (count (filter #(= "comment" (:type %)) @captured))))
          (is (some #(= "Looks good" (:text %)) @captured)))
        (finally
          (unsub)
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async append-comment-injects-write-id
  (testing "Comment append injects write-id into frontmatter"
    (let [dir (tmp-dir)
          _ (await (.mkdir fsp dir #js {:recursive true}))
          _ (await (write-task! dir "t3" "Task Three"))
          project {:id "test" :title "Test" :tasks-dir dir :meta {}}
          task {:uuid "t3" :source-path (path/join dir "t3.md")}
          captured (atom [])
          unsub (events/subscribe! #(swap! captured conj %))]
      (try
        (let [result (await (task-edit/append-comment!
                             {:project project :task task
                              :text "Looks good"
                              :source "test"}))
              raw (await (.readFile fsp (:source-path task) "utf8"))
              frontmatter (:frontmatter (content-parser/parse-task-content raw))]
          (is (:ok result))
          (is (string? (:write-id frontmatter)))
          (is (pos? (count (:write-id frontmatter)))))
        (finally
          (unsub)
          (await (.rm fsp dir #js {:recursive true :force true})))))))

(deftest ^:async invalid-identity-source-update-is-refused-before-write-or-event
  (let [dir (await (.mkdtemp fsp (path/join (os/tmpdir) "rheos-identity-update-")))
        task-path (path/join dir "identity.md")
        project {:id "test" :tasks-dir dir :meta {}}
        task {:uuid "identity" :source-path task-path}
        captured (atom [])
        unsub (events/subscribe! #(swap! captured conj %))]
    (try
      (doseq [field [:uuid :slug]
              value ["[one, two]" "{name: one}"]]
        (let [raw (str "---\r\n" (when (= field :slug) "uuid: identity\r\n")
                       (name field) ": " value
                       "\r\ntitle: Original\r\nstatus: incoming\r\n"
                       "metadata: {values: [7, false, null]}\r\n---\r\nBody\t \r\n")
              diagnostic (str "Task " (name field) " must be a string")]
          (await (.writeFile fsp task-path raw "utf8"))
          (reset! captured [])
          (let [error (try (await (task-edit/update-frontmatter!
                                   {:project project :task task :updates {:title "Updated"}}))
                           nil (catch :default error error))]
            (is (= :refused (:kind (ex-data error))))
            (is (= field (:field (ex-data error))))
            (is (= task-path (:source-path (ex-data error))))
            (is (= diagnostic (:diagnostic (ex-data error))))
            (is (= raw (await (.readFile fsp task-path "utf8")))
                "identity refusal leaves the disk source byte-identical")
            (is (empty? @captured) "identity refusal emits no mutation event"))))
      (finally
        (unsub)
        (await (.rm fsp dir #js {:recursive true :force true}))))))
