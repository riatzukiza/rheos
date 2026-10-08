(ns rheos.backend.shape.relationships-test
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer-macros [deftest is]])
            [malli.core :as m]
            [malli.registry :as mr]
            [rheos.backend.domain.relationships :as relationships]
            [rheos.backend.shape.relationships :as shape]))

(deftest named-registry-round-trips-as-plain-edn
  (let [restored (edn/read-string (pr-str shape/registry))
        options {:registry (mr/composite-registry restored m/default-registry)}]
    (is (= shape/registry restored))
    (doseq [key (keys restored)]
      (is (fn? (m/validator [:ref key] options))))
    (is (m/validate [:ref ::shape/input] {:dependency ["a" "b"]} options))))

(deftest shape-boundaries-are-closed-and-typed
  (doseq [input [{} {:parent nil} {:epic ""} {:dependency []}
                 {:dependency "b, a"} {:parent "a" :epic "epic" :dependency ["b"]}]]
    (is (shape/valid-input? input)))
  (doseq [input [{:parent false} {:epic []} {:dependency false}
                 {:dependency [nil]} {:status "done"} {"parent" "a"}]]
    (is (not (shape/valid-input? input))))
  (doseq [input [{:parent nil} {:dependency []} {:dependency "a"} {:unknown "x"}]]
    (is (not (shape/valid-normalized? input))))
  (is (shape/valid-mutation? {:uuid "legacy-opaque-id" :updates {:parent nil}}))
  (is (not (shape/valid-mutation? {:uuid "a" :updates {} :allowlist [:status]}))))

(deftest every-successful-normalization-has-the-named-output-shape
  (doseq [input [{} {:parent nil} {:epic ""} {:dependency []}
                 {:parent "a"} {:epic "epic"} {:dependency "b, a"}
                 {:parent "a" :epic "epic" :dependency ["b" "a"]}]]
    (let [result (relationships/normalize input)]
      (is (:ok? result))
      (is (shape/valid-normalized? (:value result)))
      (is (= result (relationships/normalize (:value result)))))))
