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

(defn remove-frontmatter-source
  "Remove selected whole top-level pairs using boundary-supplied AST offsets.
   Preserve every other source/body byte; decode and validate aliases afterward."
  [raw keys-to-remove pairs]
  (if-let [{:keys [opening source closing body]} (frontmatter-source raw)]
    (let [selected (filter #(contains? keys-to-remove (:key %)) pairs)
          ranges (map (fn [{:keys [key-start pair-end]}]
                        {:start (- key-start (count (or (re-find #"[^\n]*$" (subs source 0 key-start)) "")))
                         :end pair-end}) selected)
          updated (reduce (fn [text {:keys [start end]}]
                            (str (subs text 0 start) (subs text end)))
                          source (sort-by :start > ranges))]
      (str opening updated closing body))
    raw))

(defn- comment-insertion
  "Locate the final comment boundary using the section delimiter grammar.
   Retain source offsets; never render existing sections from decoded text."
  [body]
  (let [{:keys [open? fence]}
        (reduce (fn [{:keys [offset] :as state} line]
                  (let [end (+ offset (count line))]
                    (cond-> (assoc state :offset end)
                      (= "---" (str/trim line))
                      (assoc :open? (not (:open? state))
                             :fence {:start offset :end end}))))
                {:offset 0 :open? false}
                (re-seq #"[^\n]*(?:\n|$)" body))
        closing? (and fence (not open?)
                      (str/blank? (subs body (:end fence))))]
    {:offset (if closing? (:start fence) (count body))
     :open? open?
     :closing? (boolean closing?)}))

(defn append-comment
  "Insert a comment without rewriting any existing Markdown or YAML bytes.
   A closed final comment receives text before its closing fence; an open one
   receives text at EOF. New delimiter spelling follows the existing newline."
  [raw _parsed comment-text]
  (let [body (or (:body (frontmatter-source raw)) raw)
        {:keys [offset open? closing?]} (comment-insertion body)
        insertion (+ (- (count raw) (count body)) offset)
        prefix (subs raw 0 insertion)
        newline (or (re-find #"\r?\n" body) (re-find #"\r?\n" raw) "\n")
        separator (str newline newline)]
    (str prefix
         ;; A serialized comment already leaves a blank line before its fence.
         ;; Keep that source spacing rather than doubling it on every append.
         (when-not (re-find #"(?:\r?\n){2}$" prefix) separator)
         (when-not (or open? closing?) (str "---" newline))
         comment-text separator
         (when-not closing? "---")
         (subs raw insertion))))
