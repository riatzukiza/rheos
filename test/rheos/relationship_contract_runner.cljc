(ns rheos.relationship-contract-runner
  "Selected portable contract tests. Production adapters use these same laws."
  (:require #?(:clj [clojure.test :as test] :cljs [cljs.test :as test])
            [rheos.backend.domain.relationships-test]
            [rheos.backend.shape.relationships-test]))

#?(:cljs
   (defmethod test/report [::test/default :end-run-tests] [result]
     (set! (.-exitCode js/process) (if (test/successful? result) 0 1))))

(defn -main [& _]
  #?(:clj
     (let [result (test/run-tests 'rheos.backend.domain.relationships-test
                                  'rheos.backend.shape.relationships-test)]
       (when-not (test/successful? result) (System/exit 1)))
     :cljs
     (test/run-tests 'rheos.backend.domain.relationships-test
                     'rheos.backend.shape.relationships-test)))

#?(:cljs (set! *main-cli-fn* -main))
