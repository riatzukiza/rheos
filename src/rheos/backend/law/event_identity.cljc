(ns rheos.backend.law.event-identity
  "Portable generated-ID and retained-ID contracts. Ledger identity is not a
   complete event schema; historical opaque IDs remain admissible."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.registry :as mr]
            [rheos.backend.law.relationships :as relationships]))

(def uuid-pattern "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

(def registry
  {::opaque-id [:and :string [:re relationships/reference-pattern]]
   ;; JVM `$` also matches before a final line terminator. Exact length makes
   ;; the shared grammar cover the whole input on both JVM and JavaScript.
   ::uuid-entropy [:and [:string {:min 36 :max 36}] [:re (str "^" uuid-pattern "$")]]
   ::generation-input [:map {:closed true}
                       [:board-id [:ref ::opaque-id]]
                       [:task-id [:ref ::opaque-id]]
                       [:entropy [:ref ::uuid-entropy]]]})

(defn validator [schema-key]
  (m/validator [:ref schema-key]
               {:registry (mr/composite-registry registry m/default-registry)}))

(def opaque-id? (validator ::opaque-id))
(def uuid-entropy? (validator ::uuid-entropy))
(def generation-input? (validator ::generation-input))

(defn generated-id?
  "Check the explicit expected prefix and complete suffix; never split on a
   hyphen inside an opaque component or normalize a retained identity."
  [board-id task-id event-id]
  (let [prefix (str board-id "-" task-id "-")]
    (and (opaque-id? board-id) (opaque-id? task-id) (string? event-id)
         (str/starts-with? event-id prefix)
         (uuid-entropy? (subs event-id (count prefix))))))
