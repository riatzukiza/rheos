(ns rheos.relationships.baseline-frontmatter
  "Behavioral RED probe against the original e8105ee descriptive-key law.
   This historical baseline probe is not a substitute for the portable graph
   contract fixtures or the later public writer/parity tests."
  (:require [cljs.test :as t :refer [deftest is run-tests]]
            [rheos.backend.law.frontmatter :as frontmatter]))

(deftest reviewed-relationship-fields-have-an-authoring-path
  (doseq [[field value] [[:dependency "existing-predecessor"]
                         [:parent "existing-parent"]
                         [:epic "existing-epic"]]]
    (is (empty? (frontmatter/disallowed-keys {field value}))
        (str "The baseline rejects the reviewed relationship field " field))))

(deftest protected-identity-and-status-stay-protected
  (is (frontmatter/status-update? {:status "ready"}))
  (is (= [:uuid] (frontmatter/disallowed-keys {:uuid "replacement"})))
  (is (= [:write-id] (frontmatter/disallowed-keys {:write-id "replacement"}))))

(defmethod t/report [::t/default :end-run-tests] [summary]
  (set! (.-exitCode js/process) (if (t/successful? summary) 0 1)))

(run-tests 'rheos.relationships.baseline-frontmatter)
