(ns rheos.backend.law.relationships
  "Relationship identity and field authority. Decisions live in domain.relationships."
  (:require [clojure.string :as str]))

(def fields #{:parent :epic :dependency})

(def whitespace-class
  "Unicode White_Space plus BOM. Explicit ranges keep JVM and JS identical;
   neither host's default blank/trim predicate is relationship authority."
  "\\u0009-\\u000d\\u0020\\u0085\\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff")

(def reference-pattern (str "[^" whitespace-class "]"))
(def ^:private reference-regex (re-pattern reference-pattern))
(def ^:private edge-whitespace-regex
  (re-pattern (str "^[" whitespace-class "]+|[" whitespace-class "]+$")))

(defn reference?
  "An exact nonblank stored identity. Never trim, case-fold or resolve a title."
  [value]
  (and (string? value) (boolean (re-find reference-regex value))))

(defn blank?
  "Only nil or a string consisting of the declared whitespace is removable."
  [value]
  (or (nil? value) (and (string? value) (not (reference? value)))))

(defn trim-csv-member
  "Trim CSV representation syntax only. Singular/vector identities stay exact."
  [value]
  (str/replace value edge-whitespace-regex ""))
