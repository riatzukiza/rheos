(ns rheos.backend.domain.events-test
  (:require [cljs.test :refer [deftest testing is]]
            [open-hax.openplanner-protocols :as protocols]
            [rheos.backend.domain.events :as events]
            [rheos.backend.law.event-identity :as identity-law]
            [rheos.backend.shape.event-identity :as identity]))

(deftest generate-write-id-format
  (testing "Write ID is a non-empty string"
    (let [wid (events/generate-write-id)]
      (is (string? wid))
      (is (pos? (count wid))))))

(deftest generate-write-id-unique
  (testing "Two write IDs are different"
    (is (not= (events/generate-write-id) (events/generate-write-id)))))

(deftest envelope->kanban-event-basic
  (testing "Envelope converts to kanban event"
    (let [envelope {:event/type "kanban.status-change" :session/id "board"
                    :event/time "2026-06-08T00:00:00.000Z"
                    :payload {:task-id "t1" :from "incoming" :to "breakdown" :source "cli" :agent "eta-mu"}}
          result (events/envelope->kanban-event envelope)]
      (is (= "board" (:board result)))
      (is (= "t1" (:task-id result)))
      (is (= "incoming" (:from result)))
      (is (= "breakdown" (:to result)))
      (is (nil? (:correlation/status result))))))

(deftest envelope->kanban-event-correlation
  (testing "Envelope includes correlation status when present"
    (let [envelope {:event/type "kanban.file-changed" :session/id "board"
                    :event/time "2026-06-08T00:00:00.000Z"
                    :payload {:task-id "t1" :source "watcher" :agent "eta-mu"
                              :write-id "wid" :correlation/status "correlated"}}
          result (events/envelope->kanban-event envelope)]
      (is (= "correlated" (:correlation/status result)))
      (is (= "wid" (:write-id result)))
      (is (= "file-changed" (:type result))))))

(deftest ^:async emit-file-changed-records-correlation
  (testing "file-changed event carries correlation status"
    (let [ledger (reify protocols/EventAdmission
                   (append-event! [_ envelope]
                     (js/Promise.resolve envelope))
                   (append-events! [_ envelopes]
                     (js/Promise.resolve (vec envelopes)))
                   (query-events [_ _] (js/Promise.resolve #js []))
                   (watch-events [_ _ _] #js {:close! (fn [])}))
          captured (atom nil)
          unsub (events/subscribe! #(reset! captured %))]
      (try
        (await (events/emit-file-changed! ledger "board" "t1" "wid" "correlated"))
        (is (= "file-changed" (:type @captured)))
        (is (= "correlated" (:correlation/status @captured)))
        (is (= "wid" (:write-id @captured)))
        (finally
          (unsub))))))

(deftest ^:async emit-drift-detected-records-correlation
  (testing "drift-detected event carries drift correlation status"
    (let [ledger (reify protocols/EventAdmission
                   (append-event! [_ envelope]
                     (js/Promise.resolve envelope))
                   (append-events! [_ envelopes]
                     (js/Promise.resolve (vec envelopes)))
                   (query-events [_ _] (js/Promise.resolve #js []))
                   (watch-events [_ _ _] #js {:close! (fn [])}))
          captured (atom nil)
          unsub (events/subscribe! #(reset! captured %))]
      (try
        (await (events/emit-drift-detected! ledger "board" "t1" nil))
        (is (= "drift-detected" (:type @captured)))
        (is (= "drift" (:correlation/status @captured)))
        (is (string? (:write-id @captured)))
        (finally
          (unsub))))))

(defn- capturing-admission [append-fn]
  (reify protocols/EventAdmission
    (append-event! [_ envelope] (append-fn envelope))
    (append-events! [_ envelopes] (js/Promise.resolve (vec envelopes)))
    (query-events [_ _] (js/Promise.resolve []))
    (watch-events [_ _ _] {:close! (fn [])})))

(deftest ^:async producer-supplies-fresh-entropy-to-the-portable-constructor
  (let [calls (atom [])
        recorded (atom [])
        admission (capturing-admission #(do (swap! recorded conj %) (js/Promise.resolve %)))
        board "literal, id"
        task " legacy-雪 "
        compose identity/generated-id]
    (with-redefs [identity/generated-id
                  (fn [board-id task-id entropy]
                    (swap! calls conj [board-id task-id entropy])
                    (compose board-id task-id entropy))]
      (await (events/emit-comment! admission board task "First" "shared-write" "test"))
      (await (events/emit-comment! admission board task "Second" "shared-write" "test")))
    (is (= 2 (count @calls)) "Every actual emission obtains entropy at the runtime edge")
    (is (= [[board task] [board task]] (mapv #(subvec % 0 2) @calls)))
    (is (every? #(identity-law/uuid-entropy? (nth % 2)) @calls))
    (is (= 2 (count (set (map #(nth % 2) @calls)))))
    (is (every? #(identity-law/generated-id? board task (:event/id %)) @recorded))
    (is (= [board board] (mapv :session/id @recorded)))
    (is (= [task task] (mapv #(get-in % [:payload :task-id]) @recorded)))))

(deftest ^:async frozen-clock-emissions-retain-vocabulary-with-distinct-identities
  (let [saved-now (.-now js/Date)
        saved-iso (.-toISOString (.-prototype js/Date))
        timestamp "2026-10-11T18:45:52.193Z"
        recorded (atom [])
        admission (capturing-admission
                   #(do (swap! recorded conj %) (js/Promise.resolve %)))
        emitters [#(events/emit-status-change! admission "board" "subject" "incoming" "accepted" "write" "test")
                  #(events/emit-frontmatter-change! admission "board" "subject" "title" "Old" "New" "write" "test")
                  #(events/emit-comment! admission "board" "subject" "Retained text" "write" "test")
                  #(events/emit-task-created! admission "board" "subject"
                                              {:title "Created" :card-type "task" :status "incoming"
                                               :parent "parent" :epic "epic" :dependency ["predecessor"]
                                               :source-path "/fixture/subject.md" :body "Body"} "write" "test")
                  #(events/emit-file-changed! admission "board" "subject" "write" "correlated")
                  #(events/emit-drift-detected! admission "board" "subject" "write")
                  #(events/emit-drift-protocol-rerun! admission "board" "subject" "passed" {:ok true})]
        payloads [{:task-id "subject" :from "incoming" :to "accepted" :write-id "write" :source "test" :agent "eta-mu" :type "status-change"}
                  {:task-id "subject" :key "title" :old-value "Old" :new-value "New" :write-id "write" :source "test" :agent "eta-mu" :type "frontmatter"}
                  {:task-id "subject" :text "Retained text" :write-id "write" :source "test" :agent "eta-mu" :type "comment"}
                  {:task-id "subject" :title "Created" :card-type "task" :status "incoming"
                   :parent "parent" :epic "epic" :dependency ["predecessor"]
                   :source-path "/fixture/subject.md" :body "Body" :write-id "write" :source "test" :agent "eta-mu" :type "task-created"}
                  {:task-id "subject" :write-id "write" :source "watcher" :agent "eta-mu" :correlation/status "correlated" :type "file-changed"}
                  {:task-id "subject" :write-id "write" :source "watcher" :agent "eta-mu" :correlation/status "drift" :type "drift-detected"}
                  {:task-id "subject" :source "watcher" :agent "eta-mu"
                   :protocol "kanban.drift/fsm-status-check" :status "passed" :result {:ok true} :type "drift-protocol-rerun"}]]
    (set! (.-now js/Date) (fn [] 1791744352193))
    (set! (.-toISOString (.-prototype js/Date)) (fn [] timestamp))
    (try
      (dotimes [_ 2]
        (doseq [emit emitters] (await (emit))))
      (is (= 14 (count @recorded)))
      (is (= 14 (count (set (map :event/id @recorded))))
          "All emissions, including repeated same-family events, need independent identities")
      (doseq [[envelope payload] (map vector @recorded (concat payloads payloads))]
        (is (and (string? (:event/id envelope)) (seq (:event/id envelope))))
        (is (= {:event/type (str "kanban." (:type payload)) :event/time timestamp
                :session/id "board" :delivery/mode "tell" :payload payload}
               (dissoc envelope :event/id))
            "An identity repair preserves every other envelope field"))
      (finally
        (set! (.-now js/Date) saved-now)
        (set! (.-toISOString (.-prototype js/Date)) saved-iso)))))

(deftest ^:async event-publication-waits-for-successful-persistence
  (let [resolve-append (atom nil)
        stored (atom nil)
        published (atom [])
        admission (capturing-admission
                   #(do (reset! stored %)
                        (js/Promise. (fn [resolve _] (reset! resolve-append resolve)))))
        unsubscribe (events/subscribe! #(swap! published conj %))]
    (try
      (let [completion (events/emit-comment! admission "board" "subject" "Text" "write" "test")]
        (await (js/Promise.resolve))
        (is (some? @stored))
        (is (empty? @published) "An admitted but unpersisted event is not yet published")
        (@resolve-append @stored)
        (is (= @stored (await completion)))
        (is (= [(events/envelope->kanban-event @stored)] @published)))
      (let [failure (js/Error. "Controlled append refusal")
            refused (capturing-admission (fn [_] (js/Promise.reject failure)))
            outcome (try (await (events/emit-comment! refused "board" "subject" "Refused" "write" "test"))
                         nil
                         (catch :default error error))]
        (is (identical? failure outcome))
        (is (= 1 (count @published)) "A failed append does not publish a second success"))
      (finally (unsubscribe)))))
