(ns rheos.backend.domain.event-ledger-test
  (:require #?(:cljs [cljs.test :refer [deftest is testing]]
               :clj [clojure.test :refer [deftest is testing]])
            [clojure.string :as str]
            [rheos.backend.domain.event-ledger :as ledger]
            [rheos.backend.law.event-identity :as law]
            [rheos.backend.shape.event-identity :as identity]))

(def entropy-a "12345678-1234-4234-8234-123456789abc")
(def entropy-b "12345678-1234-4234-8234-123456789abd")

(deftest deterministic-generated-identity-preserves-opaque-components
  (doseq [[board task] [["board" "task"] ["literal, id" " task-α " ]
                        ["board-uuid-shaped" "legacy:punctuation/雪"]]]
    (let [value (identity/generated-id board task entropy-a)]
      (is (= (str board "-" task "-" entropy-a) value))
      (is (= value (identity/generated-id board task entropy-a)))
      (is (not= value (identity/generated-id board task entropy-b)))
      (is (law/generated-id? board task value))
      (is (not (law/generated-id? (str board "x") task value)))
      (is (not (law/generated-id? board (str task "x") value)))))
  (doseq [bad [nil true false 42 "" " \t\r\n" "\u00a0\u2003"]]
    (is (not (law/opaque-id? bad)))
    (is (thrown? #?(:cljs cljs.core/ExceptionInfo :clj clojure.lang.ExceptionInfo)
                 (identity/generated-id bad "task" entropy-a)))
    (is (thrown? #?(:cljs cljs.core/ExceptionInfo :clj clojure.lang.ExceptionInfo)
                 (identity/generated-id "board" bad entropy-a))))
  (doseq [bad [nil true "not-a-uuid" (str/upper-case entropy-a) (str entropy-a " ")]]
    (is (not (law/uuid-entropy? bad)))
    (is (thrown? #?(:cljs cljs.core/ExceptionInfo :clj clojure.lang.ExceptionInfo)
                 (identity/generated-id "board" "task" bad))))
  (is (not (law/generated-id? "board" "task" "board-task-legacy"))))

(deftest canonical-entropy-refuses-every-trailing-line-terminator
  (doseq [ending ["\n" "\r" "\r\n" "\u2028" "\u2029"]]
    (let [bad (str entropy-a ending)]
      (is (not (law/uuid-entropy? bad)))
      (is (thrown? #?(:cljs cljs.core/ExceptionInfo :clj clojure.lang.ExceptionInfo)
                   (identity/generated-id "board" "task" bad)))
      (is (not (law/generated-id? "board" "task" (str "board-task-" bad)))))))

(deftest ledger-identities-remain-opaque-and-unique
  (let [text "{:event/id \"old-1791744352193\"}\n{:event/id \"literal, id-雪\" :payload {:x 1}}\n"
        expected {:ok true :record-count 2 :errors []}]
    (is (= expected (ledger/check-text text)))
    (is (= expected (ledger/check-text (str/replace text "\n" "\r\n"))))
    (is (= expected (ledger/check-text (subs text 0 (dec (count text)))))))
  (doseq [second-record ["{:event/id \"same\"}" "{:event/id \"same\" :payload {:different true}}"]]
    (let [result (ledger/check-text (str "{:event/id \"first\"}\n{:event/id \"same\"}\n" second-record))]
      (is (false? (:ok result)))
      (is (= 3 (:record-count result)))
      (is (= [{:row 3 :code :duplicate-event-id :event-id "same" :first-row 2}]
             (:errors result)))))
  (doseq [form ["{}" "{:event/id nil}" "{:event/id true}" "{:event/id 42}"
                "{:event/id \"\"}" "{:event/id \"  \"}" "[]" "nil"]]
    (let [result (ledger/check-text (str "{:event/id \"first\"}\n" form))]
      (is (false? (:ok result)))
      (is (= 2 (get-in result [:errors 0 :row])))
      (is (= (if (#{"[]" "nil"} form) :non-map-record :invalid-event-id)
             (get-in result [:errors 0 :code]))))))

(deftest every-physical-row-is-complete-and-consumed
  (doseq [[text row code] [["" nil :empty-ledger]
                         ["\n" 1 :missing-edn-form]
                         ["{:event/id \"ok\"}\n\n" 2 :missing-edn-form]
                         ["; only a comment\n" 1 :missing-edn-form]
                         ["{:event/id\n\"split\"}" 1 :invalid-edn]
                         ["{:event/id \"ok\"} {:event/id \"extra\"}" 1 :extra-edn-form]
                         ["{:event/id \"ok\"} junk" 1 :extra-edn-form]
                         ["{:event/id \"ok\"} :rheos.backend.shape.ledger-text/eof" 1 :extra-edn-form]
                         ["{:event/id \"ok\"} )" 1 :invalid-edn]]]
    (testing text
      (let [result (ledger/check-text text)]
        (is (false? (:ok result)))
        (is (= row (get-in result [:errors 0 :row])))
        (is (= code (get-in result [:errors 0 :code]))))))
  (let [result (ledger/check-text "{:event/id \"ok\"}\nnot-a-map\n{:event/id \"ok\"}\n")]
    (is (= [2 3] (mapv :row (:errors result))) "No malformed line stops or hides the later scan")))

(deftest discarded-forms-cannot-hide-physical-ledger-input
  (doseq [text ["{:event/id \"ok\"} #_{:event/id \"extra\"}"
                "#_{:event/id \"hidden\"} {:event/id \"ok\"}"
                "{:event/id \"ok\" :payload [#_42 1]}"
                "{:event/id \"ok\"}\u2003#_1"
                "{:event/id \"ok\"}#_#_1 2"]]
    (let [result (ledger/check-text text)]
      (is (false? (:ok result)))
      (is (= 1 (get-in result [:errors 0 :row])))
      (is (= :discarded-edn-form (get-in result [:errors 0 :code])))))
  (doseq [text ["{:event/id \"ok\" :payload \"literal #_ text\"}"
                "{:event/id \"ok\" :payload \"escaped \\\" #_ text\"} ; #_ is a comment"
                (str "{:event/id \"ok\" :payload " (pr-str [\# \_ \" \;]) "}")
                "{:event/id \"ok\" :payload foo#_bar}"]]
    (is (:ok (ledger/check-text text)))))

(deftest discard-adjacent-to-single-character-literals
  (let [single-chars [\; \" \\ \[ \] \{ \} \( \) \^ \@ \` \~]]
    (doseq [ch single-chars]
      (let [text (str "{:event/id \"ok\" :payload [\\" ch "#_1]}")
            result (ledger/check-text text)]
        (testing text
          (is (false? (:ok result)))
          (is (= 1 (get-in result [:errors 0 :row])))
          (is (= :discarded-edn-form (get-in result [:errors 0 :code]))))))
    (is (:ok (ledger/check-text
              (str "{:event/id \"ok\" :payload ["
                   (str/join " " (map #(str "\\" %) single-chars)) "]}")))
        "Literal reader-special characters without a discard remain valid")))
