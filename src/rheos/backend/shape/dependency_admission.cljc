(ns rheos.backend.shape.dependency-admission
  "Named, data-only policy owned by the resolved workflow, not its callers."
  (:require [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  {::state [:string {:min 1}]
   ::states [:vector {:min 1} [:ref ::state]]
   ::policy [:map {:closed true}
             [:successful-states [:ref ::states]]
             [:guarded-targets [:ref ::states]]]})

(def valid-policy?
  (m/validator [:ref ::policy]
               {:registry (mr/composite-registry registry m/default-registry)}))
