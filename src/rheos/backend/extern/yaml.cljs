(ns rheos.backend.extern.yaml
  "Decode YAML source nodes into Clojure-shaped ranges for targeted edits."
  (:require [clojure.walk :as walk]
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

(defn- source-document [source schema]
  (let [^js document (yaml/parseDocument source #js {:keepSourceTokens true :stringKeys true :schema schema})
        ^js contents (.-contents document)]
    (when (pos? (.-length (.-errors document)))
      (throw (ex-info "Cannot update invalid YAML frontmatter" {})))
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
