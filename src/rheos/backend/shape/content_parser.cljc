(ns rheos.backend.shape.content-parser
  "Pure source frames, section morphisms and range-based task content patches."
  (:require [clojure.string :as str]))

(defn frontmatter-source
  "Locate an initial frontmatter frame without decoding its YAML source.
   Returns opening/source/closing/body strings or nil for frontmatter-free text."
  [raw]
  (if-let [[_ opening source closing body]
           (re-matches #"(?m)^((?:\uFEFF)?---[ \t]*\r?\n)([\s\S]*?)(^---[ \t]*(?:\r?\n|$))([\s\S]*)" raw)]
    {:opening opening :source source :closing closing :body body}
    (do
      (when (re-find #"^(?:\uFEFF)?---[ \t]*\r?\n" raw)
        (throw (ex-info "Unterminated YAML frontmatter" {})))
      nil)))

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

(defn parse-task-content
  "Combine decoded frontmatter with the sections parsed from its source body."
  [frontmatter content]
  {:frontmatter frontmatter :sections (parse-sections content)})

(defn- unicode-escape [code]
  (str "\\u" (apply str (map #(nth "0123456789abcdef"
                                  (bit-and 15 (bit-shift-right code %)))
                             [12 8 4 0]))))

(def ^:private quoted-string-escapes
  (into {\" "\\\"" \\ "\\\\" \newline "\\n" \return "\\r" \tab "\\t"}
        (map (fn [code] [(char code) (unicode-escape code)])
             (remove #{9 10 13}
                     (concat (range 32) [133 8232 8233] (range 55296 57344))))))

(defn- quoted-string
  "Portable YAML double-quoted scalar encoding; preserve UTF-16 units at UTF-8 writes."
  [value]
  (str "\"" (str/escape value quoted-string-escapes) "\""))

(defn serialize-frontmatter [frontmatter]
  (let [lines (mapv (fn [[k v]]
                      (cond
                        (vector? v) (str (name k) ": [" (str/join ", " (mapv #(quoted-string (str %)) v)) "]")
                        (string? v) (str (name k) ": " (quoted-string v))
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
      (and (number? value) (< ##-Inf value ##Inf))
      (and (vector? value) (every? update-value? value))))

(defn checked-updates
  "Normalize keys and admit only the portable values supported for updates."
  [updates]
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
                       value
                       (when header-comment (str " " header-comment))
                       (when empty-before-comment? " ")
                       trailing-newline)}))

(defn patch-frontmatter-source
  "Patch requested top-level YAML fields without serializing unrelated source.
   Entries contain normalized key names and already encoded replacement strings.
   Pairs contain decoded source ranges; the caller validates YAML before and after
   this pure patch. Body bytes are never parsed or rewritten."
  [raw entries pairs]
  (if (empty? entries)
    raw
    (if-let [{:keys [opening source closing body]} (frontmatter-source raw)]
        (let [{:keys [patches additions]}
              (reduce (fn [result [key value]]
                        (if-let [pair (some #(when (= key (:key %)) %) pairs)]
                          (update result :patches conj (value-patch source pair value))
                          (update result :additions conj (str key ": " value))))
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
          (str opening updated-source closing body))
        (let [bom? (str/starts-with? raw "\uFEFF")
              body (if bom? (subs raw 1) raw)]
          (str (when bom? "\uFEFF") "---\n"
               (str/join "\n" (map (fn [[key value]] (str key ": " value)) entries))
               "\n---\n\n" body)))))

(defn append-comment
  "Render a comment using already decoded task data and the original source frame."
  [raw parsed comment-text]
  (let [sections (:sections parsed)
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
