(ns rheos.backend.shape.content-parser
  "Parse task markdown into frontmatter + body/comment sections."
  (:require [clojure.string :as str]
            [rheos.backend.extern.yaml :as yaml]))

(defn- frontmatter-source [raw]
  ;; Delimiters must occupy lines at the beginning of the file. In particular,
  ;; never search the body for a YAML-looking fenced example or comment block.
  (when-let [[_ opening source closing body]
             (re-matches #"(?m)^((?:\uFEFF)?---[ \t]*\r?\n)([\s\S]*?)(^---[ \t]*(?:\r?\n|$))([\s\S]*)" raw)]
    {:opening opening :source source :closing closing :body body}))

(defn parse-frontmatter [raw]
  (if-let [{:keys [source body]} (frontmatter-source raw)]
    {:frontmatter (yaml/read-frontmatter source) :content body}
    (do
      (when (re-find #"^(?:\uFEFF)?---[ \t]*\r?\n" raw)
        (throw (ex-info "Unterminated YAML frontmatter" {})))
      {:frontmatter {} :content raw})))

(defn parse-sections [content]
  (let [lines (str/split-lines content)
        result (loop [remaining lines
                      current-type "body"
                      buffer []
                      sections []]
                 (if (empty? remaining)
                   (let [text (str/trim (str/join "\n" buffer))]
                     (if (seq text)
                       (conj sections {:type current-type :content text})
                       sections))
                   (let [line (first remaining)
                         rest-lines (rest remaining)]
                     (if (= (str/trim line) "---")
                       (let [text (str/trim (str/join "\n" buffer))
                             new-sections (if (seq text)
                                            (conj sections {:type current-type :content text})
                                            sections)
                             new-type (if (= current-type "body") "comment" "body")]
                         (recur rest-lines new-type [] new-sections))
                       (recur rest-lines current-type (conj buffer line) sections)))))]
    result))

(defn parse-task-content [raw]
  (let [{:keys [frontmatter content]} (parse-frontmatter raw)
        sections (parse-sections content)]
    {:frontmatter frontmatter
     :sections sections}))

(defn task-content->js [parsed]
  #js {:frontmatter (clj->js (:frontmatter parsed))
       :sections (clj->js (mapv (fn [s] #js {:type (:type s) :content (:content s)}) (:sections parsed)))})

(defn serialize-frontmatter [frontmatter]
  (let [lines (mapv (fn [[k v]]
                      (cond
                        (vector? v) (str (name k) ": [" (str/join ", " (mapv #(str "\"" % "\"") v)) "]")
                        (string? v) (str (name k) ": \"" v "\"")
                        (nil? v) (str (name k) ": ")
                        :else (str (name k) ": " v)))
                    frontmatter)]
    (str "---\n" (str/join "\n" lines) "\n---")))

(defn serialize-sections [sections]
  (str/join "\n\n"
    (mapv (fn [section]
            (if (= (:type section) "comment")
              (str "---\n" (:content section) "\n\n---")
              (:content section)))
          sections)))

(defn serialize-task-content [parsed]
  (str (serialize-frontmatter (:frontmatter parsed))
       "\n\n"
       (serialize-sections (:sections parsed))))

(defn- update-value? [value]
  (or (nil? value) (string? value) (boolean? value)
      (and (number? value) (yaml/finite-number? value))
      (and (vector? value) (every? update-value? value))))

(defn- checked-updates [updates]
  (when-not (map? updates)
    (throw (ex-info "Frontmatter updates must be a map" {})))
  (let [entries (mapv (fn [[key value]]
                        (let [key-name (cond
                                         (keyword? key) (when-not (namespace key) (name key))
                                         (string? key) key
                                         :else nil)]
                          (when-not (and key-name (re-matches #"\w[\w_-]*" key-name))
                            (throw (ex-info "Invalid frontmatter update key" {:key key})))
                          (when-not (update-value? value)
                            (throw (ex-info "Unsupported frontmatter update value" {:key key})))
                          [key-name value]))
                      updates)]
    (when-not (= (count entries) (count (set (map first entries))))
      (throw (ex-info "Duplicate frontmatter update keys" {})))
    entries))

(defn- value-patch [source {:keys [start end key-end header-comment]} value]
  (let [separator (subs source key-end start)
        ;; A block value may start on another line. Move its replacement onto
        ;; the key's line when only whitespace separates the colon and value.
        block-separator? (and (re-matches #":\s*" separator)
                              (str/includes? separator "\n"))
        trailing-newline (second (re-find #"(\r?\n)$" (subs source start end)))
        empty-before-comment? (and (= start end)
                                   (= "#" (subs source start (min (count source) (inc start)))))]
    {:start (if block-separator? (inc key-end) start)
     :end end
     :replacement (str (when (or block-separator? (= ":" separator)) " ")
                       (yaml/replacement-value value)
                       (when header-comment (str " " header-comment))
                       (when empty-before-comment? " ")
                       trailing-newline)}))

(defn update-frontmatter-keys
  "Patch requested top-level YAML fields without serializing unrelated source.
   Frontmatter must be a valid block mapping. Body bytes are never parsed or
   rewritten; the general task serializer and comment append remain separate."
  [raw updates]
  (let [entries (checked-updates updates)]
    (if (empty? entries)
      raw
      (if-let [{:keys [opening source closing body]} (frontmatter-source raw)]
        (let [pairs (yaml/block-map-entries source)
              {:keys [patches additions]}
              (reduce (fn [result [key value]]
                        (if-let [pair (some #(when (= key (:key %)) %) pairs)]
                          (update result :patches conj (value-patch source pair value))
                          (update result :additions conj (str key ": " (yaml/replacement-value value)))))
                      {:patches [] :additions []} entries)
              patched (reduce (fn [text {:keys [start end replacement]}]
                                (str (subs text 0 start) replacement (subs text end)))
                              source
                              (sort-by :start > patches))
              newline (or (re-find #"\r?\n" patched)
                          (if (str/ends-with? opening "\r\n") "\r\n" "\n"))
              updated-source (str patched
                                  (when (seq additions)
                                    (str (when (and (seq patched) (not (str/ends-with? patched "\n"))) newline)
                                         (str/join newline additions) newline)))]
          ;; Qualify the replacement before any caller can write it. Source
          ;; properties such as tags and anchors can affect replacement syntax.
          (yaml/block-map-entries updated-source)
          (str opening updated-source closing body))
        (let [bom? (str/starts-with? raw "\uFEFF")
              body (if bom? (subs raw 1) raw)]
          (when (re-find #"^(?:\uFEFF)?---[ \t]*\r?\n" raw)
            (throw (ex-info "Unterminated YAML frontmatter" {})))
          (str (when bom? "\uFEFF") "---\n"
               (str/join "\n" (map (fn [[key value]] (str key ": " (yaml/replacement-value value))) entries))
               "\n---\n\n" body))))))

(defn update-frontmatter [raw key value]
  (update-frontmatter-keys raw {key value}))

(defn inject-write-id [raw write-id]
  (update-frontmatter raw "write-id" write-id))

(defn append-comment [raw comment-text]
  (let [parsed (parse-task-content raw)
        sections (:sections parsed)
        last-section (last sections)
        updated (if (= "comment" (:type last-section))
                  (assoc-in parsed [:sections (dec (count sections)) :content]
                            (str (:content last-section) "\n\n" comment-text))
                  (update parsed :sections conj {:type "comment" :content comment-text}))]
    (if-let [{:keys [opening source closing]} (frontmatter-source raw)]
      ;; Only the section body is reconstructed. Retain valid YAML spelling,
      ;; typed extension values, comments and aliases for write-id injection.
      (str opening source closing
           (if (str/ends-with? closing "\n") "\n" "\n\n")
           (serialize-sections (:sections updated)))
      (serialize-task-content updated))))
