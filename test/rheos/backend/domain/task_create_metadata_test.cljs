(ns rheos.backend.domain.task-create-metadata-test
  (:require [cljs.test :refer [deftest is testing]]
            [rheos.backend.domain.task-create :as task-create]))

(deftest configured-card-dirs-declare-the-creation-vocabulary
  (testing "legacy boards keep task/epic compatibility"
    (is (= #{"task" "epic"} (task-create/card-types {})))
    (is (= "task" (task-create/check-request! {:project {} :title "T"}))))
  (testing "configured keys form a closed repository vocabulary"
    (let [project {:card-dirs {:story "stories" :chore "chores"}}]
      (is (= #{"story" "chore"} (task-create/card-types project)))
      (is (= "story" (task-create/check-request!
                       {:project project :title "T" :card-type "story"})))
      (is (= :usage
             (:kind (ex-data
                     (try (task-create/check-request!
                           {:project project :title "T" :card-type "task"})
                          nil (catch :default e e))))))
      (is (= :usage
             (:kind (ex-data
                     (try (task-create/check-request! {:project project :title "T"})
                          nil (catch :default e e)))))))))
