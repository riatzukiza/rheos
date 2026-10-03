(ns rheos.backend.infra.content-parser
  "Compose YAML boundary decoding/encoding with pure source-preserving morphisms."
  (:require [rheos.backend.extern.content-parser :as extern-content]
            [rheos.backend.extern.yaml :as yaml]
            [rheos.backend.shape.content-parser :as content]))

(defn parse-frontmatter [raw]
  (if-let [{:keys [source body]} (content/frontmatter-source raw)]
    {:frontmatter (yaml/read-frontmatter source) :content body}
    {:frontmatter {} :content raw}))

(defn parse-task-content [raw]
  (let [{:keys [frontmatter content]} (parse-frontmatter raw)]
    (content/parse-task-content frontmatter content)))

(defn task-content->js [parsed]
  (extern-content/task-content->js parsed))

(defn update-frontmatter-keys
  "Decode and validate YAML around the pure patch before it reaches a writer."
  [raw updates]
  (let [entries (content/checked-updates updates)]
    (if (empty? entries)
      raw
      (let [frame (content/frontmatter-source raw)
            pairs (if frame (yaml/block-map-entries (:source frame)) [])
            replacements (mapv (fn [[key value]] [key (yaml/replacement-value value)]) entries)
            updated (content/patch-frontmatter-source raw replacements pairs)]
        (yaml/block-map-entries (:source (content/frontmatter-source updated)))
        updated))))

(defn update-frontmatter [raw key value]
  (update-frontmatter-keys raw {key value}))

(defn inject-write-id [raw write-id]
  (update-frontmatter raw "write-id" write-id))

(defn append-comment [raw comment-text]
  (content/append-comment raw (parse-task-content raw) comment-text))
