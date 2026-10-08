(ns rheos.backend.law.dependency-admission-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is]]
            [rheos.backend.domain.dependency-admission :as admission]
            [rheos.backend.law.fsm :as fsm]
            [rheos.backend.shape.dependency-admission :as shape]))

(def reviewed-policy
  {:successful-states ["done"] :guarded-targets ["ready" "todo" "in_progress"]})

(deftest promethean-owns-the-reviewed-admission-policy
  (is (= reviewed-policy (:dependency-admission (fsm/resolve-fsm {:fsm "promethean"}))))
  (is (= reviewed-policy (:dependency-admission
                          (fsm/resolve-fsm {:fsm {:extends :promethean
                                                 :build-gate-commands ["true"]}})))))

(deftest named-policy-is-data-and-refuses-invalid-declarations
  (is (shape/valid-policy? reviewed-policy))
  (is (= shape/registry (reader/read-string (pr-str shape/registry))))
  (is (nil? (admission/policy-errors {:states []})))
  (is (empty? (admission/policy-errors (assoc fsm/promethean-fsm :dependency-admission reviewed-policy))))
  (doseq [bad [nil {} (assoc reviewed-policy :extra true)
               (assoc reviewed-policy :successful-states [])
               (assoc reviewed-policy :successful-states ["done" "done"])
               (assoc reviewed-policy :successful-states ["unregistered"])
               (assoc reviewed-policy :guarded-targets [42])]]
    (is (seq (admission/policy-errors (assoc fsm/promethean-fsm :dependency-admission bad)))
        (pr-str bad))))
