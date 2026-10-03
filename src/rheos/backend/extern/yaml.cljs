(ns rheos.backend.extern.yaml
  "Decode YAML source nodes into Clojure-shaped ranges for targeted edits."
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            ["yaml" :as yaml]))

(defn- pair-source [^js pair]
  (let [^js key (.-key pair)
        ^js node (.-value pair)
        key-end (aget (.-range key) 1)
        range (when node (.-range node))
        ^js token (when node (.-srcToken node))]
    {:key (.-value key)
     :key-end key-end
     ;; Empty values normally have a zero-width Scalar range. Keep a safe
     ;; insertion point after the colon when the parser supplies no value node.
     :start (if range (aget range 0) (inc key-end))
     :end (if range (aget range 1) (inc key-end))
     :header-comment (when (and token (= "block-scalar" (.-type token)))
                       (some #(when (= "comment" (.-type %)) (.-source %))
                             (seq (.-props token))))}))

(def ^:private tag-warning-codes
  #{"TAG_RESOLVE_FAILED" "BAD_COLLECTION_TYPE"})

(defn- incompatible-standard-tag [^js document]
  (let [tagged-nodes (atom [])]
    (yaml/visit document
                (fn [_ ^js node]
                  (when (and node (.-tag node) (.-range node))
                    (swap! tagged-nodes conj {:tag (.-tag node)
                                             :start (aget (.-range node) 0)}))))
    (some (fn [^js warning]
            (when (contains? tag-warning-codes (.-code warning))
              ;; A warning spans the tag token; its node's range starts after
              ;; that token. Use those source positions and the canonical AST
              ;; tag, rather than parsing a diagnostic message or tag spelling.
              (let [tag-end (aget (.-pos warning) 1)
                    tag (:tag (first (sort-by :start
                                             (filter #(<= tag-end (:start %)) @tagged-nodes))))]
                (when (and tag (str/starts-with? tag "tag:yaml.org,2002:")) tag))))
          (seq (.-warnings document)))))

(defn- source-document [source schema]
  (let [^js document (yaml/parseDocument source #js {:keepSourceTokens true :stringKeys true :schema schema})
        ^js contents (.-contents document)]
    (when (pos? (.-length (.-errors document)))
      (throw (ex-info "Cannot update invalid YAML frontmatter" {})))
    ;; The library may report incompatible standard tags as warnings, including
    ;; collection-kind mismatches. Use its resolution for all standard tags;
    ;; unknown application tags remain preserved. Failsafe reads intentionally
    ;; keep scalar conventions.
    (when (= schema "core")
      (when-let [tag (incompatible-standard-tag document)]
        (throw (ex-info "Cannot update an incompatible standard YAML tag" {:tag tag}))))
    (when (and contents (or (not (yaml/isMap contents)) (.-flow contents)))
      (throw (ex-info "YAML frontmatter must be a block mapping" {})))
    ;; Alias resolution errors are reported by conversion, not parseDocument.
    ;; Validate them before writing; the native result is not domain authority.
    (try
      (.toJS document #js {:maxAliasCount 100})
      (catch :default _
        (throw (ex-info "Cannot update invalid YAML frontmatter" {}))))
    document))

(defn block-map-entries [source]
  (let [^js contents (.-contents ^js (source-document source "core"))]
    (if contents (mapv pair-source (seq (.-items contents))) [])))

(defn read-frontmatter [source]
  (let [^js document (source-document source "failsafe")
        data (js->clj (.toJS document #js {:maxAliasCount 100}) :keywordize-keys true)]
    ;; The card API historically exposes flat scalar fields as strings, with
    ;; empty YAML values as "". Decode comments/quotes/aliases correctly while
    ;; retaining those conventions and structured extension data.
    (if (nil? data)
      {}
      (walk/postwalk (fn [value]
                       (cond
                         (nil? value) ""
                         (or (number? value) (boolean? value)) (str value)
                         :else value))
                     data))))

(defn finite-number? [value]
  (js/Number.isFinite value))

(defn replacement-value [value]
  ;; JSON scalar/flow syntax is valid YAML and escapes quotes and newlines.
  ;; An empty value retains the existing card convention for nil.
  (if (nil? value) "" (js/JSON.stringify (clj->js value))))
