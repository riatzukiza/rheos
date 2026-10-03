(ns rheos.backend.domain.task-edit-test
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])
            [rheos.backend.domain.task-edit :as task-edit]))

(deftest frontmatter-plan-admits-update-keys-at-its-own-boundary
  (doseq [[updates message] [[{:foreign/title "new"} "Invalid frontmatter update key"]
                             [{7 "new"} "Invalid frontmatter update key"]
                             [(array-map :title "new" "title" "other")
                              "Duplicate frontmatter update keys"]]]
    (testing (str "refuses " updates " before returning a change plan")
      (let [error (try
                    (task-edit/plan-frontmatter-update
                      {:title "old"} {:title "new"} "rendered source" updates)
                    nil
                    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error error))]
        (is (= message (when error (ex-message error))))))))

(deftest frontmatter-plan-preserves-admitted-extension-events
  (let [value [7 false nil]
        updates (array-map :custom value "title" false)
        frontmatter {:title "false" :custom value :metadata {:keep true}}
        raw "rendered source\n"
        plan (task-edit/plan-frontmatter-update
               {:title "old" :custom "prior"} frontmatter raw updates)]
    (is (identical? frontmatter (:frontmatter plan)))
    (is (= raw (:raw plan)))
    (is (= [{:key "custom" :old-value "prior" :new-value value}
            {:key "title" :old-value "old" :new-value false}]
           (:changes plan)))
    (is (identical? value (:new-value (first (:changes plan)))))))

(deftest frontmatter-plan-refuses-collection-valued-core-fields
  (doseq [field [:title :priority :status]
          value [["one" "two"] {:name "one"}]]
    (let [error (try
                  (task-edit/plan-frontmatter-update
                    {} {field value :metadata {:keep [7 false nil]}}
                    "candidate" {field value})
                  nil
                  (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error error))]
      (is (= {:kind :refused :field field} (ex-data error)))
      (is (= (str "Task " (name field) " must be a string")
             (when error (ex-message error)))))))
