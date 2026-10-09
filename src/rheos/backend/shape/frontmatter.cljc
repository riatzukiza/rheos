(ns rheos.backend.shape.frontmatter
  (:require [clojure.string :as str]))

(def ^:private unsupported ::unsupported)

(def ^:private canonical-string-sequence-pattern
  #"^\[\s*(?:\"[^\"]*\"(?:\s*,\s*\"[^\"]*\")*)?\s*\]$")

(def ^:private quoted-string-pattern
  #"\"([^\"]*)\"")

(defn parse-canonical-string-sequence
  "Decode Rheos's supported YAML subset for one inline string sequence.

   Returns nil for syntax outside that subset so every consumer can make the
   same fail-closed decision instead of growing a second comma-splitting parser."
  [value]
  ;; Escapes are not decoded here, so a sequence containing one is outside the
  ;; subset. Returning raw backslash sequences would publish the wrong value.
  (when (and (re-matches canonical-string-sequence-pattern value)
             (not (str/includes? value "\\")))
    (mapv second (re-seq quoted-string-pattern value))))

(defn- flat-scalar
  "Plain and simple double-quoted scalars. A YAML comment (` #` or tab `#`)
   ends a plain scalar. A quoted scalar that is unterminated, carries escapes,
   or has trailing content other than a comment is unsupported, so callers fall
   back instead of publishing a partial value."
  [value]
  (cond
    (str/starts-with? value "#") unsupported
    (str/starts-with? value "\"")
    (if-let [[_ inner] (re-matches #"^\"([^\"\\]*)\"(?:[ \t]+#.*)?$" value)]
      inner
      unsupported)
    :else
    (let [plain (str/trim (str/replace value #"[ \t]+#.*$" ""))]
      (if (empty? plain) unsupported plain))))

(defn- flat-value [raw]
  (let [value (str/trim raw)]
    (cond
      (empty? value) unsupported
      (or (str/starts-with? value "|")
          (str/starts-with? value ">")
          (str/starts-with? value "{")) unsupported
      (str/starts-with? value "[")
      (or (parse-canonical-string-sequence value) unsupported)
      :else (flat-scalar value))))

(defn parse-flat [frontmatter-raw]
  (reduce (fn [acc line]
            (if-let [[_ k v] (re-matches #"^([A-Za-z0-9_-]+):[ ]*(.*)$" line)]
              (let [value (flat-value v)]
                (if (= unsupported value)
                  acc
                  (assoc acc (keyword k) value)))
              acc))
          {}
          (str/split-lines (or frontmatter-raw ""))))
