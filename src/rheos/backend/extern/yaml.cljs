(ns rheos.backend.extern.yaml
  "Decode YAML source nodes into Clojure-shaped ranges for targeted edits."
  (:require [clojure.string :as str]
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

(defn- native-children [value]
  (cond
    (instance? js/Map value) (seq (js/Array.from (.entries value)))
    (instance? js/Set value) (seq (js/Array.from (.values value)))
    (array? value) (seq value)
    :else (seq (js/Object.values value))))

(defn- assert-acyclic! [value]
  (let [^js active (js/WeakSet.)
        ^js complete (js/WeakSet.)]
    (letfn [(visit! [node]
              (when (or (array? node) (object? node))
                (when (.has active node)
                  (throw (ex-info "Cyclic YAML aliases are unsupported" {:type :cyclic-alias})))
                (when-not (.has complete node)
                  (.add active node)
                  (doseq [child (native-children node)] (visit! child))
                  (.delete active node)
                  (.add complete node))))]
      (visit! value))))

(declare native->data)

(defn- data-key [value]
  (if (string? value) (keyword value) (native->data value)))

(defn- native->data [value]
  ;; Native tag results stay within this adapter. Arrays/maps/sets become
  ;; Clojure collections; timestamps and binary payloads use portable values.
  (cond
    (instance? js/Date value) (.toISOString value)
    (instance? js/Uint8Array value) (vec (js/Array.from value))
    (instance? js/Map value) (into {} (map (fn [entry]
                                          [(data-key (aget entry 0))
                                           (native->data (aget entry 1))])
                                        (seq (js/Array.from (.entries value)))))
    (instance? js/Set value) (into #{} (map native->data (seq (js/Array.from (.values value)))))
    (array? value) (mapv native->data value)
    (object? value) (into {} (map (fn [key] [(keyword key) (native->data (aget value key))])
                                (seq (js/Object.keys value))))
    :else value))

(defn- source-document [source schema]
  (let [^js document (yaml/parseDocument source #js {:keepSourceTokens true :stringKeys true :schema schema})
        ^js contents (.-contents document)]
    (when (pos? (.-length (.-errors document)))
      (throw (ex-info "Cannot update invalid YAML frontmatter" {})))
    ;; The library may report incompatible standard tags as warnings, including
    ;; collection-kind mismatches. Use its resolution for all standard tags;
    ;; unknown application tags remain preserved.
    (when (= schema "core")
      (when-let [tag (incompatible-standard-tag document)]
        (throw (ex-info "Cannot update an incompatible standard YAML tag" {:tag tag}))))
    (when (and contents (or (not (yaml/isMap contents)) (.-flow contents)))
      (throw (ex-info "YAML frontmatter must be a block mapping" {})))
    ;; Alias resolution errors are reported by conversion, not parseDocument.
    ;; Validate them before writing; the native result is not domain authority.
    (let [data (try
                 (.toJS document #js {:maxAliasCount 100})
                 (catch :default _
                   (throw (ex-info "Cannot update invalid YAML frontmatter" {}))))]
      ;; toJS permits actual circular references. Qualify the graph before any
      ;; recursive read conversion or before an update caller can write it.
      (assert-acyclic! data)
      {:document document :data data})))

(defn block-map-entries [source]
  (let [{:keys [document]} (source-document source "core")
        ^js contents (.-contents ^js document)]
    (if contents (mapv pair-source (seq (.-items contents))) [])))

(defn read-frontmatter [source]
  (let [{:keys [document data]} (source-document source "core")
        ^js contents (.-contents ^js document)
        frontmatter (native->data data)]
    ;; The library's Scalar.source is decoded text, retaining spelling such as
    ;; 001/3.0/TRUE and quoted escapes. Use it only for top-level scalar fields;
    ;; structured extension values retain their core-schema types.
    (reduce (fn [result ^js pair]
              (let [^js node (.-value pair)
                    ^js resolved (if (yaml/isAlias node) (.resolve node document) node)]
                (if (yaml/isScalar resolved)
                  (assoc result (data-key (.-value ^js (.-key pair))) (or (.-source resolved) ""))
                  result)))
            (or frontmatter {})
            (seq (when contents (.-items contents))))))

(defn finite-number? [value]
  (js/Number.isFinite value))

(defn replacement-value [value]
  ;; JSON scalar/flow syntax is valid YAML and escapes quotes and newlines.
  ;; An empty value retains the existing card convention for nil.
  (if (nil? value) "" (js/JSON.stringify (clj->js value))))
