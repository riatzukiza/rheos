(ns rheos.backend.law.relationships
  "Relationship identity and field authority. Decisions live in domain.relationships."
  (:require [clojure.string :as str]))

(def fields #{:parent :epic :dependency})

(defn reference?
  "An exact nonblank stored identity. Never trim, case-fold or resolve a title."
  [value]
  (and (string? value) (not (str/blank? value))))
