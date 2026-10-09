(ns rheos.backend.domain.relationships-test
  (:require [clojure.set :as set]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [rheos.backend.law.frontmatter :as frontmatter]
            [rheos.backend.domain.relationships :as relationships]
            [rheos.backend.domain.dependency-admission :as admission]
            [rheos.backend.law.relationships :as relationship-law]
            [rheos.backend.shape.relationships :as shape]))

(def board
  [{:uuid "epic-A" :type "epic"}
   {:uuid "epic-B" :type "epic"}
   {:uuid "parent" :type "task" :parent "epic-A" :epic "epic-A"}
   {:uuid "child" :type "task" :parent "parent" :epic "epic-A"
    :title "Original" :status "incoming" :write-id "server-owned"}
   {:uuid "pred-A" :type "task"}
   {:uuid "pred-B"}])

(defn with-card [cards uuid updates]
  (mapv #(if (= uuid (:uuid %)) (merge % updates) %) cards))

(defn kinds [result]
  (set (map :kind (:errors result))))

(defn refusal? [result]
  (and (false? (:ok? result)) (seq (:errors result))
       (not-any? #(contains? result %)
                 [:task :tasks :updates :relationships :changes])))

(deftest supported-representations-and-removal
  (doseq [field [:parent :epic]
          removal [nil "" " " "\t\n\r"]]
    (let [raw {field removal} result (relationships/normalize raw)]
      (is (shape/valid-input? raw) (str "input shape must permit removal " raw))
      (is (= {:ok? true :value {}} result))))
  (doseq [removal [nil "" " " "\t" []]]
    (is (= {:ok? true :value {}} (relationships/normalize {:dependency removal}))))
  (doseq [input ["pred-A" ["pred-A"]]]
    (is (= {:dependency ["pred-A"]} (:value (relationships/normalize {:dependency input})))))
  (doseq [input ["pred-B, pred-A" ["pred-B" "pred-A"] ["pred-A" "pred-B"]]]
    (is (= {:dependency ["pred-A" "pred-B"]}
           (:value (relationships/normalize {:dependency input})))))
  (is (= {:ok? true :value {}} (relationships/normalize {}))))

(deftest malformed-members-are-not-dropped
  (doseq [field [:parent :epic]
          value [false 0 :parent [] ["parent"] {}]]
    (let [result (relationships/normalize {field value})]
      (is (refusal? result))
      (is (= #{:malformed-relationship} (kinds result)))))
  (doseq [value [false 0 :pred #{} '("pred-A") {} [nil] [false] [0]
                 ["pred-A" ""] ["pred-A" " "] "pred-A," ",pred-A"
                 "pred-A,,pred-B" "pred-A, ,pred-B"]]
    (let [result (relationships/normalize {:dependency value})]
      (is (refusal? result))
      (is (= #{:malformed-relationship} (kinds result)))))
  (doseq [value [["pred-A" "pred-A"] "pred-A, pred-A"]]
    (is (= #{:duplicate-dependency}
           (kinds (relationships/normalize {:dependency value})))))
  (doseq [value [nil [] "parent"]]
    (is (= #{:malformed-relationship-input} (kinds (relationships/normalize value)))))
  (is (= #{:unknown-relationship-field} (kinds (relationships/normalize {:title "not here"})))))

(deftest identity-is-exact-existing-uuid-field
  (is (:ok? (relationships/inspect-graph board)))
  (doseq [guess ["Parent" " parent" "parent " "Original" "missing"]]
    (let [result (relationships/admit-update board "child" {:parent guess})]
      (is (refusal? result))
      (is (= #{:missing-reference} (kinds result)))))
  (let [legacy [{:uuid "literal, id"} {:uuid "unicode-汝"} {:uuid "consumer"}]]
    (is (:ok? (relationships/admit-update legacy "consumer" {:parent "literal, id"})))
    (is (:ok? (relationships/admit-update legacy "consumer" {:dependency ["literal, id" "unicode-汝"]}))))
  (is (= " parent " (get-in (relationships/normalize {:parent " parent "}) [:value :parent]))))

(deftest unicode-whitespace-has-one-portable-meaning
  ;; Unicode White_Space plus BOM, matching the explicit relationship contract.
  ;; JVM Character whitespace and JavaScript trim do not agree on these inputs.
  (doseq [blank ["\u0085" "\u00a0" "\u202f" "\ufeff"]]
    (is (not (relationship-law/reference? blank)))
    (is (= {:ok? true :value {}} (relationships/normalize {:parent blank})))
    (is (= #{:malformed-uuid}
           (kinds (relationships/inspect-graph [{:uuid blank}])))))
  (let [result (relationships/normalize {:dependency "\u00a0b\u00a0,\u202fa\u202f"})]
    (is (= {:dependency ["a" "b"]} (:value result))))
  (is (= "\u00a0a\u00a0" (get-in (relationships/normalize {:parent "\u00a0a\u00a0"})
                                   [:value :parent]))))

(deftest missing-ambiguous-and-malformed-identities
  (doseq [cards [[{:uuid "dup"} {:uuid "dup"}]
                 (conj board {:uuid "pred-A"})]]
    (is (= #{:ambiguous-uuid} (kinds (relationships/inspect-graph cards)))))
  (doseq [uuid [nil "" " " 3 false :uuid]]
    (is (contains? (kinds (relationships/inspect-graph [{:uuid uuid}])) :malformed-uuid)))
  (doseq [snapshot [nil {} '("task") [nil] ["task"]]]
    (is (= #{:malformed-snapshot} (kinds (relationships/inspect-graph snapshot)))))
  (is (= #{:task-not-found} (kinds (relationships/admit-update board "missing" {:parent nil}))))
  (is (= #{:ambiguous-uuid}
         (kinds (relationships/admit-update (conj board {:uuid "child"}) "child" {:parent nil})))))

(deftest actual-epic-type-required
  (doseq [target ["parent" "pred-B"]]
    (is (= #{:invalid-epic-target}
           (kinds (relationships/admit-update board "child" {:parent nil :epic target})))))
  ;; The reviewed compatibility contract reclassifies exact "story" below.
  (doseq [type [nil "Epic" :epic false "Story" " story" "story " "unknown" [] {}]]
    (is (= #{:malformed-card-type}
           (kinds (relationships/inspect-graph [{:uuid "card" :type type}])))))
  (is (:ok? (relationships/inspect-graph [{:uuid "legacy"}]))))

(deftest declared-card-types-are-ordinary-cards
  (let [vocabulary {:card-types #{"chore" "story"}}
        board [{:uuid "epic-A" :type "epic"}
               {:uuid "tidy" :type "chore" :epic "epic-A"}
               {:uuid "dep" :type "chore"}]]
    (is (= #{:malformed-card-type}
           (kinds (relationships/inspect-graph board)))
        "undeclared types stay closed without the board's vocabulary")
    (is (:ok? (relationships/inspect-graph board vocabulary)))
    (is (= #{:malformed-card-type}
           (kinds (relationships/inspect-graph (conj board {:uuid "x" :type "unknown"})
                                               vocabulary)))
        "the vocabulary only admits declared types")
    (is (= #{:invalid-epic-target}
           (kinds (relationships/inspect-graph [{:uuid "tidy" :type "chore"}
                                                {:uuid "child" :epic "tidy"}]
                                               vocabulary)))
        "a declared type never satisfies an epic membership target")
    (is (:ok? (relationships/admit-update board "tidy" {:dependency ["dep"]} vocabulary)))
    (is (:allowed? (admission/decide {} board "tidy" "ready" vocabulary)))))

(def story-board
  [{:uuid "epic" :type "epic" :status "done"}
   {:uuid "parent-story" :type "story" :parent "epic" :epic "epic" :status "done"}
   {:uuid "child-story" :type "story" :parent "parent-story" :epic "epic"
    :dependency ["parent-story"] :status "breakdown"}
   {:uuid "ordinary-task" :type "task" :dependency ["child-story"] :status "breakdown"}])

(def story-policy
  {:states ["breakdown" "ready" "todo" "in_progress" "done"]
   :dependency-admission {:successful-states ["done"]
                          :guarded-targets ["ready" "todo" "in_progress"]}})

(deftest authored-stories-retain-type-identity-and-relationships
  (let [result (relationships/inspect-graph story-board)]
    (is (:ok? result))
    (is (= (set story-board) (set (:tasks result)))))
  (let [result (relationships/admit-update story-board "ordinary-task"
                                          {:parent "child-story" :epic "epic"})]
    (is (:ok? result))
    (is (= "task" (get-in result [:task :type])))
    (is (= "child-story" (get-in result [:task :parent]))))
  (is (= {:allowed? true}
         (admission/decide story-policy story-board "child-story" "ready"))))

(deftest story-is-never-an-epic-target
  (is (= #{:invalid-epic-target}
         (kinds (relationships/inspect-graph
                 [{:uuid "story" :type "story"}
                  {:uuid "child" :type "story" :epic "story"}])))))

(deftest stories-retain-graph-refusals
  (doseq [[expected cards]
          [[:missing-reference [{:uuid "story" :type "story" :parent "missing"}]]
           [:ambiguous-uuid [{:uuid "same" :type "story"} {:uuid "same" :type "story"}]]
           [:malformed-uuid [{:type "story"}]]
           [:conflicting-epic (conj (with-card story-board "child-story" {:epic "other"})
                                    {:uuid "other" :type "epic"})]
           [:cycle [{:uuid "a" :type "story" :parent "b"}
                    {:uuid "b" :type "story" :parent "a"}]]
           [:cycle [{:uuid "a" :type "story" :dependency ["b"]}
                    {:uuid "b" :type "story" :dependency ["a"]}]]]]
    (let [result (relationships/inspect-graph cards)]
      (is (refusal? result))
      (is (= #{expected} (kinds result))))))

(deftest story-predecessor-completion-is-still-required
  (doseq [status ["breakdown" "ready" "in_progress"]]
    (let [result (admission/decide story-policy
                                   (with-card story-board "parent-story" {:status status})
                                   "child-story" "ready")]
      (is (false? (:allowed? result)))
      (is (= [{:kind :unfinished-predecessor :uuid "child-story"
               :target "parent-story" :status status}]
             (:errors result)))))
  (is (= {:allowed? true}
         (admission/decide story-policy
                           (with-card story-board "child-story" {:status "done"})
                           "ordinary-task" "ready"))))

(deftest self-reference-refused-for-each-edge
  (doseq [field [:parent :epic :dependency]]
    (let [result (relationships/admit-update board "child" {field "child"})]
      (is (refusal? result))
      (is (= #{:self-reference} (kinds result))))))

(deftest direct-and-indirect-cycles
  (doseq [field [:parent :dependency]
          cards [[{:uuid "a" field "b"} {:uuid "b" field "a"}]
                 [{:uuid "a" field "b"} {:uuid "b" field "c"} {:uuid "c" field "a"}]]]
    (let [result (relationships/inspect-graph cards)]
      (is (refusal? result))
      (is (= [{:kind :cycle :graph field :remaining (mapv :uuid cards)}]
             (:errors result)))))
  (testing "different meanings do not create a combined cycle constraint"
    (is (:ok? (relationships/inspect-graph [{:uuid "a" :parent "b"}
                                {:uuid "b" :dependency "a"}])))))

(deftest cycle-diagnostic-does-not-claim-an-exact-path
  (let [result (relationships/inspect-graph [{:uuid "a" :dependency "b"}
                                 {:uuid "b" :dependency ["a" "c"]}
                                 {:uuid "c"}])]
    (is (= [{:kind :cycle :graph :dependency :remaining ["a" "b" "c"]}]
           (:errors result)))
    (is (not-any? #(contains? (first (:errors result)) %) [:path :cycle-members]))))

(deftest hierarchy-membership-follows-nearest-epic
  (is (= #{:conflicting-epic}
         (kinds (relationships/admit-update board "child" {:epic "epic-B"}))))
  (let [declared (with-card board "parent" {:parent nil})]
    (is (= #{:conflicting-epic}
           (kinds (relationships/admit-update declared "child" {:epic "epic-B"})))))
  (let [nested [{:uuid "outer" :type "epic"}
                {:uuid "inner" :type "epic" :parent "outer" :epic "outer"}
                {:uuid "task" :parent "inner" :epic "inner"}]]
    (is (:ok? (relationships/inspect-graph nested)))
    (is (= #{:conflicting-epic}
           (kinds (relationships/admit-update nested "task" {:epic "outer"})))))
  (is (:ok? (relationships/admit-update board "child" {:epic nil})))
  (is (:ok? (relationships/admit-update board "child" {:parent nil :epic "epic-B"}))))

(deftest protected-and-unknown-keys-refuse-the-whole-mixed-batch
  (doseq [field frontmatter/forbidden-keys]
    (let [result (relationships/admit-update board "child" {field "forged" :dependency "pred-A" :title "New"})]
      (is (refusal? result))
      (is (= #{:protected-field} (kinds result)))))
  (doseq [field [:dependencies :type :allowlist :anything]]
    (let [result (relationships/admit-update board "child" {field "forged" :parent "parent"})]
      (is (refusal? result))
      (is (= #{:unknown-field} (kinds result)))))
  (is (= #{:missing-reference}
         (kinds (relationships/admit-update board "child" {:dependency "gone" :title "New"}))))
  (is (= #{:conflicting-epic}
         (kinds (relationships/admit-update board "child" {:parent "epic-B" :title "New"})))))

(deftest one-valid-mixed-batch-produces-only-actual-changes
  (let [result (relationships/admit-update board "child" {:dependency "pred-B, pred-A" :title "New"})]
    (is (:ok? result))
    (is (false? (:noop? result)))
    (is (= {:dependency ["pred-A" "pred-B"] :title "New"} (:updates result)))
    (is (= [{:field :dependency :old-value nil :new-value ["pred-A" "pred-B"]}
            {:field :title :old-value "Original" :new-value "New"}]
           (:changes result)))
    (is (= "incoming" (get-in result [:task :status])))
    (is (= "server-owned" (get-in result [:task :write-id])))))

(deftest malformed-inherited-data-is-visible-without-silent-repair
  (let [legacy (with-card board "pred-B" {:dependency false})]
    (is (= #{:malformed-relationship} (kinds (relationships/inspect-graph legacy))))
    (let [edit (relationships/admit-update legacy "child" {:title "New"})]
      (is (:ok? edit))
      (is (false? (:relationship-change? edit)))
      (is (= {:title "New"} (:updates edit))))
    (let [edit (relationships/admit-update legacy "child" {:dependency "pred-A"})]
      (is (refusal? edit))
      (is (= #{:malformed-relationship} (kinds edit))))
    (let [repair (relationships/admit-update legacy "pred-B" {:dependency nil})]
      (is (:ok? repair))
      (is (false? (:noop? repair)))
      (is (= [{:field :dependency :old-value false :new-value nil}] (:changes repair)))))
  (let [legacy (with-card board "child" {:parent "missing" :epic "epic-A"})]
    (is (:ok? (relationships/admit-update legacy "child" {:parent "parent"}))))
  (is (refusal? (relationships/admit-update (with-card board "pred-B" {:parent "missing"})
                                "child" {:parent "parent"}))))

(deftest semantic-noops-preserve-original-data-and-suppress-change
  (let [legacy (with-card board "child" {:dependency "pred-B, pred-A"})
        before (first (filter #(= "child" (:uuid %)) legacy))]
    (doseq [replacement [["pred-A" "pred-B"] "pred-A, pred-B" ["pred-B" "pred-A"]]]
      (let [result (relationships/admit-update legacy "child" {:dependency replacement})]
        (is (:ok? result))
        (is (:noop? result))
        (is (= before (:task result)))
        (is (= {} (:updates result)))
        (is (= [] (:changes result))))))
  (doseq [removal [nil "" " " []]]
    (let [result (relationships/admit-update board "child" {:dependency removal})]
      (is (:noop? result))
      (is (= [] (:changes result)))))
  (let [old (with-card board "child" {:dependency ["pred-A"]})
        result (relationships/admit-update old "child" {:dependency []})]
    (is (= {:dependency nil} (:updates result)))
    (is (not (contains? (:task result) :dependency)))))

(deftest generic-frontmatter-authority-is-still-closed
  (doseq [key relationship-law/fields]
    (is (= [key] (frontmatter/disallowed-keys {key "reference"}))))
  (doseq [key frontmatter/mutable-keys]
    (is (empty? (frontmatter/disallowed-keys {key "description"})))))

(deftest malformed-mutations-never-throw-or-partially-admit
  (doseq [[uuid updates] [[nil {}] ["" {}] [" " {:parent nil}]
                          ["child" nil] ["child" []] ["child" {"parent" "parent"}]
                          ["child" {}]]]
    (is (refusal? (relationships/admit-update board uuid updates))))
  (doseq [snapshot [nil {} [nil]]]
    (is (refusal? (relationships/admit-update snapshot "child" {:parent nil})))))

(defn permutations [items]
  (if (empty? items)
    [[]]
    (mapcat (fn [item]
              (map #(into [item] %) (permutations (vec (remove #{item} items)))))
            items)))

(deftest ordering-does-not-change-a-decision
  (let [cards [{:uuid "a"} {:uuid "b"} {:uuid "c" :title "Old"}]
        pairs [[:parent "a"] [:dependency ["b" "a"]] [:title "New"]]
        expected (relationships/admit-update cards "c" (into {} pairs))]
    (doseq [ordered (permutations cards) update-pairs (permutations pairs)]
      (is (= expected (relationships/admit-update ordered "c" (into {} update-pairs))))))
  (let [cards [{:uuid "a" :parent "gone"} {:uuid "b" :dependency "absent"}]
        expected (relationships/inspect-graph cards)]
    (is (= expected (relationships/inspect-graph (vec (reverse cards)))))))

(deftest tied-diagnostics-do-not-follow-snapshot-order
  (doseq [field [:parent :epic :dependency]
          :let [cards (mapv #(hash-map :uuid "dup" field %) [false 0 :invalid])
                expected (relationships/inspect-graph cards)]]
    (is (refusal? expected))
    (is (= #{:ambiguous-uuid :malformed-relationship} (kinds expected)))
    (is (= #{false 0 :invalid}
           (set (map :value (filter #(= :malformed-relationship (:kind %))
                                   (:errors expected))))))
    (doseq [ordered (permutations cards)]
      (is (= expected (relationships/inspect-graph ordered)) (str field ordered))))
  (testing "duplicate-dependency diagnostics retain their differing targets"
    (let [cards [{:uuid "dup" :dependency ["a" "a"]}
                 {:uuid "dup" :dependency ["b" "b"]}]
          expected (relationships/inspect-graph cards)]
      (is (refusal? expected))
      (is (= #{["a"] ["b"]}
             (set (keep :targets (:errors expected)))))
      (is (= expected (relationships/inspect-graph (vec (reverse cards)))))))
  (testing "nil and empty UUIDs must remain distinct diagnostics despite equal str keys"
    (let [cards [{:uuid nil} {:uuid ""}]
          expected (relationships/inspect-graph cards)]
      (is (= #{nil ""} (set (map :uuid (:errors expected)))))
      (is (= expected (relationships/inspect-graph (vec (reverse cards))))))))

(defn cycle-by-reachability?
  "Independent oracle: a vertex reaches itself along a nonempty path of at most N edges."
  [adjacency]
  (boolean
   (some (fn [start]
           (loop [reached (set (get adjacency start)), remaining (count adjacency)]
             (cond
               (contains? reached start) true
               (zero? remaining) false
               :else (recur (set/union reached (set (mapcat adjacency reached)))
                            (dec remaining)))))
         (keys adjacency))))

(defn subsets [items]
  (reduce (fn [prior item] (into prior (map #(conj % item) prior))) [[]] items))

(deftest exhaustive-small-dependency-graphs-agree-with-independent-oracle
  (let [vertices ["a" "b" "c"]
        edges (for [a vertices b vertices :when (not= a b)] [a b])]
    (doseq [selection (subsets edges)]
      (let [adjacency (reduce (fn [graph [a b]] (update graph a conj b))
                              (zipmap vertices (repeat [])) selection)
            cards (mapv (fn [id] {:uuid id :dependency (get adjacency id)}) vertices)
            expected-cycle? (cycle-by-reachability? adjacency)
            result (relationships/inspect-graph cards)]
        (is (= (not expected-cycle?) (:ok? result)) (str selection))
        (when expected-cycle?
          (is (= #{:cycle} (kinds result))))
        (when-not expected-cycle?
          (is (= (set vertices) (set (map :uuid (:tasks result))))))))))

(deftest exhaustive-small-parent-graphs-agree-with-independent-oracle
  (let [vertices ["a" "b" "c"]]
    (doseq [a [nil "b" "c"] b [nil "a" "c"] c [nil "a" "b"]]
      (let [parents [a b c]
            adjacency (zipmap vertices (mapv #(if % [%] []) parents))
            cards (mapv (fn [id parent] {:uuid id :parent parent}) vertices parents)]
        (is (= (not (cycle-by-reachability? adjacency)) (:ok? (relationships/inspect-graph cards)))
            (str parents))))))

(deftest long-hierarchy-does-not-depend-on-host-recursion-limit
  (let [ids (mapv #(str "node-" %) (range 1200))
        cards (mapv (fn [i id] (cond-> {:uuid id}
                                (pos? i) (assoc :parent (get ids (dec i)))))
                    (range 1200) ids)]
    (is (:ok? (relationships/inspect-graph cards)))
    (let [cycle (relationships/inspect-graph (with-card cards "node-0" {:parent "node-1199"}))]
      (is (= #{:cycle} (kinds cycle)))
      (is (= 1200 (count (:remaining (first (:errors cycle)))))))))
