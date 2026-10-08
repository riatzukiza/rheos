(ns rheos.backend.shape.relationships
  "Portable, named shapes at the relationship decision boundary.
   References are existing opaque UUID-field strings, not title matches or a
   new RFC-UUID restriction. Graph semantics belong to law.relationships."
  (:require [malli.core :as m]
            [malli.registry :as mr]))

(def registry
  {::reference [:string {:min 1}]
   ::singular-input [:maybe :string]
   ::dependency-input [:maybe [:or :string [:vector [:ref ::reference]]]]
   ::input [:map {:closed true}
            [:parent {:optional true} [:ref ::singular-input]]
            [:epic {:optional true} [:ref ::singular-input]]
            [:dependency {:optional true} [:ref ::dependency-input]]]
   ::normalized [:map {:closed true}
                 [:parent {:optional true} [:ref ::reference]]
                 [:epic {:optional true} [:ref ::reference]]
                 [:dependency {:optional true}
                  [:vector {:min 1} [:ref ::reference]]]]
   ::mutation [:map {:closed true}
               [:uuid [:ref ::reference]]
               [:updates [:map-of :keyword :any]]]})

(defn validator
  "Compile one named schema without changing the host's global registry."
  [schema-key]
  (m/validator [:ref schema-key]
               {:registry (mr/composite-registry registry m/default-registry)}))

(def valid-input? (validator ::input))
(def valid-normalized? (validator ::normalized))
(def valid-mutation? (validator ::mutation))
