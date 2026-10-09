(ns rheos.backend.shape.frontmatter
  (:require [clojure.string :as str]))

(def ^:private unsupported ::unsupported)

(def ^:private canonical-string-sequence-pattern
  #"^\[\s*(?:\"[^\"]*\"(?:\s*,\s*\"[^\"]*\")*)?\s*\]$")

(def ^:private quoted-string-pattern
  #"\"([^\"]*)\"")

(def ^:private non-string-plain-pattern
  "YAML 1.2 core-schema null, boolean, integer and float plain scalars. The flat
   view only decodes strings, so these must not be published as their spelling."
  #"^(?:~|null|Null|NULL|true|True|TRUE|false|False|FALSE|[-+]?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)(?:[eE][-+]?[0-9]+)?|0x[0-9a-fA-F]+|0o[0-7]+|[-+]?\.(?:inf|Inf|INF)|\.(?:nan|NaN|NAN))$")

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
  "Plain, simple double-quoted and single-quoted string scalars. In a
   single-quoted scalar `''` is an escaped quote; the surrounding quotes are
   syntax, not content. A YAML comment (` #` or tab `#`)
   ends a plain scalar. A quoted scalar that is unterminated, carries escapes,
   or has trailing content other than a comment is unsupported, so callers fall
   back instead of publishing a partial value. Plain null, boolean and numeric
   scalars are not strings, so they are unsupported too; quote them to keep
   the spelling as a string. A leading `&` anchor, `*` alias or `!` tag is
   YAML node syntax rather than content, so it is unsupported as well."
  [value]
  (cond
    (str/starts-with? value "#") unsupported
    (re-find #"^[&*!]" value) unsupported
    (str/starts-with? value "\"")
    (if-let [[_ inner] (re-matches #"^\"([^\"\\]*)\"(?:[ \t]+#.*)?$" value)]
      inner
      unsupported)
    (str/starts-with? value "'")
    (if-let [[_ inner] (re-matches #"^'((?:[^']|'')*)'(?:[ \t]+#.*)?$" value)]
      (str/replace inner "''" "'")
      unsupported)
    :else
    (let [plain (str/trim (str/replace value #"[ \t]+#.*$" ""))]
      (if (or (empty? plain) (re-matches non-string-plain-pattern plain))
        unsupported
        plain))))

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

(defn parse-flat
  "The partial flat view of top-level keys. An indented line after a key
   continues that key's value (a folded plain scalar or a nested block), which
   this view does not decode, so the key is omitted rather than published as
   its first line."
  [frontmatter-raw]
  (:acc
   (reduce (fn [{:keys [acc current] :as state} line]
             (cond
               (str/blank? line) state

               (re-find #"^[ \t]" line)
               (if current
                 {:acc (dissoc acc current) :current current}
                 state)

               :else
               (if-let [[_ k v] (re-matches #"^([A-Za-z0-9_-]+):[ ]*(.*)$" line)]
                 (let [key (keyword k)
                       value (flat-value v)]
                   {:acc (if (= unsupported value) (dissoc acc key) (assoc acc key value))
                    :current key})
                 {:acc acc :current nil})))
           {:acc {} :current nil}
           (str/split-lines (or frontmatter-raw "")))))
