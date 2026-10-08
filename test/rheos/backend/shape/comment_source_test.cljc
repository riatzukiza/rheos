(ns rheos.backend.shape.comment-source-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
            [clojure.string :as str]
            [rheos.backend.shape.content-parser :as parser]))

(defn- append [raw text]
  (parser/append-comment raw
                         (parser/parse-task-content {}
                           (or (:body (parser/frontmatter-source raw)) raw))
                         text))

(defn- comments [raw]
  (filter #(= "comment" (:type %))
          (parser/parse-sections (or (:body (parser/frontmatter-source raw)) raw))))

(deftest first-comment-preserves-every-original-source-byte
  (doseq [newline ["\n" "\r\n"]
          header ["" (str "\uFEFF--- \t" newline "uuid: source" newline "--- " newline)]
          body [(str "    first code line  " newline "\tsecond\t " newline)
                (str "# Heading" newline newline "Body  " newline newline)
                "Body without a final newline"]]
    (let [raw (str header body)
          result (append raw "New comment")]
      (is (str/starts-with? result raw)
          "adding the first comment must not rewrite any prior source")
      (is (= 1 (count (comments result))))
      (is (= "New comment" (:content (first (comments result))))))))

(deftest later-comment-preserves-source-around-the-closing-fence
  (let [before (str "---\r\nuuid: source\r\n---\r\n"
                    "    Body  \r\n\r\n---\r\n    Prior comment  \r\n\t \r\n")
        after "--- \t\r\n \t\r\n"
        raw (str before after)
        result (append raw "New comment")
        split (when (<= (+ (count before) (count after)) (count result))
                {:before (subs result 0 (count before))
                 :inserted (subs result (count before) (- (count result) (count after)))
                 :after (subs result (- (count result) (count after)))})]
    (is (str/starts-with? result before))
    (is (str/ends-with? result after))
    (is (= raw (when split (str (:before split) (:after split))))
        "only an inserted range may separate the original prefix and suffix")
    (is (= "\r\n\r\nNew comment\r\n\r\n"
           (:inserted split)))
    (is (= 1 (count (comments result))))
    (is (str/includes? (:content (first (comments result))) "Prior comment"))
    (is (str/includes? (:content (first (comments result))) "New comment"))))

(deftest empty-and-open-comment-blocks-retain-their-delimiter-meaning
  (doseq [raw ["Body\n---" "Body\n---\n"
              "Body\n---\nExisting" "Body\n---\n---\n"]]
    (testing raw
      (let [result (append raw "New comment")]
        (is (= 1 (count (comments result))))
        (is (str/includes? (:content (first (comments result))) "New comment"))))))

(deftest body-after-a-comment-remains-outside-the-new-comment
  (let [raw "Body\r\n---\r\nExisting\r\n---\r\n    Later body  \r\n"
        result (append raw "New comment")
        sections (parser/parse-sections result)]
    (is (str/starts-with? result raw))
    (is (= ["body" "comment" "body" "comment"] (mapv :type sections)))
    (is (= ["Existing" "New comment"] (mapv :content (comments result))))))
