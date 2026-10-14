#!/usr/bin/env bb
;; Segmented Receipt River ledger check for Rheos.
;;
;; The ledger is the frozen legacy file `.ημ/receipts.edn` plus every
;; `.ημ/receipts/<UTC timestamp>-<id>.edn` segment (one record each). Rules
;; ported from riatzukiza/foresight#35 (`scripts/evidence.clj verify-receipts`):
;; - every base segment still exists byte-for-byte (no edits, no deletes);
;; - the segment directory, once present, is kept;
;; - every base record is still present in the head ledger;
;; - the legacy file only loses whole lines (stays an ordered subsequence of
;;   the base) and gains no record the base lacked;
;; - no record appears twice in the merged head ledger;
;; - the legacy file stays a regular non-executable file (mode 100644);
;; - segment names are real UTC timestamps; segments are non-empty,
;;   newline-terminated, regular files holding exactly one EDN map (the whole
;;   file is one form, so pretty-printing across lines is fine);
;; - every new record has a valid receipt envelope, including a nonblank
;;   `:repo` and a strict UTC `:ts` (`YYYY-MM-DDTHH:MM:SS[.fraction]Z`).
;;
;; `--range` checks every commit in `base..at` against each of its real
;; parents (merge commits against all parents), then `at` against `base`.
;; In range mode the supplied base is used as-is, so a force-push is compared
;; with the pre-push tip; without `--range` the base is the merge-base.
;;
;; TODO(clio/eta-mu): replace this script with eta-mu's receipt/ledger
;; commands built on Clio segment directories (`clio canonicalize-dir`) once
;; receipt-river moves onto Clio after the E1.11 package move-out. Canonical
;; order will then be Clio's cause/stream order; `:ts` is display only.
;;
;; Usage: bb scripts/verify_receipts.clj [--base <rev>] [--at <rev>] [--range]
;;   --base defaults to origin/main, --at defaults to HEAD.
(ns verify-receipts
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (java.security MessageDigest)
           (java.time Instant LocalDateTime)
           (java.time.format DateTimeFormatter ResolverStyle)))

(def legacy-path ".ημ/receipts.edn")
(def segment-directory ".ημ/receipts")
(def segment-readme "README.md")
(def segment-name-pattern
  #"([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{6})[0-9]{0,9}Z-[0-9a-z][0-9a-z-]*[.]edn")

(defn fail! [message] (throw (ex-info message {:receipts/error true})))

(defn git-bytes ^bytes [& args]
  (let [{:keys [exit out err]} (apply process/shell {:out :bytes :err :string :continue true}
                                      "git" args)]
    (when-not (zero? exit) (fail! (str "git " (str/join " " args) " failed: " err)))
    out))

(defn git-text [& args] (String. ^bytes (apply git-bytes args) "UTF-8"))

(defn sha256 [^String value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes value "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn canonical-form
  "Order-independent EDN form: equal values print identically."
  [value]
  (cond
    (map? value) [:map (vec (sort-by pr-str (map (fn [[k v]] [(canonical-form k) (canonical-form v)]) value)))]
    (set? value) [:set (vec (sort-by pr-str (map canonical-form value)))]
    (vector? value) [:vector (mapv canonical-form value)]
    (sequential? value) [:list (mapv canonical-form value)]
    (tagged-literal? value) [:tagged (:tag value) (canonical-form (:form value))]
    :else value))

(defn record-id [record] (sha256 (pr-str (canonical-form record))))

(defn nonblank-string? [value] (and (string? value) (not (str/blank? value))))

(def utc-ts-pattern
  #"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:[.][0-9]{1,9})?Z")

(defn utc-timestamp?
  "Strict Receipt River `:ts`: an ISO-8601 UTC instant spelled with `Z`."
  [value]
  (and (string? value)
       (re-matches utc-ts-pattern value)
       (try (Instant/parse value) true (catch Exception _ false))))

(defn receipt-envelope? [receipt]
  (and (map? receipt)
       (keyword? (:kind receipt))
       (utc-timestamp? (:ts receipt))
       (every? nonblank-string? ((juxt :repo :origin :owner :dod :pi :host) receipt))
       (vector? (:manifest receipt))
       (every? nonblank-string? (:manifest receipt))
       (vector? (:refs receipt))
       (every? nonblank-string? (:refs receipt))))

(defn segment-name? [value]
  (when-let [[_ stamp] (and (string? value) (re-matches segment-name-pattern value))]
    (try
      (LocalDateTime/parse stamp (-> (DateTimeFormatter/ofPattern "uuuu-MM-dd'T'HHmmss")
                                     (.withResolverStyle ResolverStyle/STRICT)))
      true
      (catch Exception _ false))))

(defn read-one-edn-map!
  "Parse exactly one EDN map from `text`; trailing forms or non-maps fail."
  [^String text label]
  (try
    (with-open [rdr (java.io.PushbackReader. (java.io.StringReader. text))]
      (let [opts {:default tagged-literal :eof ::eof}
            form (edn/read opts rdr)
            more (edn/read opts rdr)]
        (when (= form ::eof)
          (fail! (str "Receipt segment must contain exactly one EDN map: " label)))
        (when-not (= more ::eof)
          (fail! (str "Receipt segment must contain exactly one EDN map: " label)))
        (when-not (map? form)
          (fail! (str "Receipt segment must contain exactly one EDN map: " label)))
        form))
    (catch Exception e
      (when (re-find #"exactly one EDN map" (str (ex-message e)))
        (throw e))
      (fail! (str "Unreadable EDN receipt in " label ": " (ex-message e))))))

(defn read-records [^String text label]
  (->> (str/split-lines text)
       (remove str/blank?)
       (mapv (fn [line] (read-one-edn-map! line label)))))

(defn path-exists? [rev path]
  (zero? (:exit (process/shell {:out :string :err :string :continue true}
                               "git" "cat-file" "-e" (str rev ":" path)))))

(defn tree-entry
  "The `[mode kind]` of `path` at `rev`, or nil when absent."
  [rev path]
  (let [out (git-text "ls-tree" "-z" rev "--" path)
        entry (first (remove str/blank? (str/split out #"\u0000")))]
    (when entry
      (let [[_ mode kind] (re-matches #"(?s)([0-7]{6}) ([a-z]+) [0-9a-f]{40,64}\t.+" entry)]
        [mode kind]))))

(defn read-legacy [rev]
  (when-let [[mode kind] (tree-entry rev legacy-path)]
    (when-not (and (= "100644" mode) (= "blob" kind))
      (fail! (str "The legacy ledger " legacy-path " must stay a non-executable regular file at " rev
                  " (found " mode " " kind ")"))))
  (let [bytes (if (path-exists? rev legacy-path) (git-bytes "show" (str rev ":" legacy-path)) (byte-array 0))]
    {:segment/path legacy-path :segment/legacy? true :segment/bytes bytes
     :segment/records (read-records (String. ^bytes bytes "UTF-8") legacy-path)}))

(defn read-segment [rev path mode kind]
  (when-not (and (= "100644" mode) (= "blob" kind))
    (fail! (str "Receipt segment must be a non-executable regular file: " path)))
  (let [^bytes bytes (git-bytes "show" (str rev ":" path))]
    (when (zero? (alength bytes)) (fail! (str "Receipt segment must not be empty: " path)))
    (when-not (= 10 (aget bytes (dec (alength bytes))))
      (fail! (str "Receipt segment must end with a newline: " path)))
    ;; The whole file is one EDN form; line-based parsing is legacy-only.
    {:segment/path path :segment/bytes bytes
     :segment/records [(read-one-edn-map! (String. bytes "UTF-8") path)]}))

(defn directory-at?
  "True when `path` is a git tree at `rev`. `ls-tree` without `-d` lists children,
  so a directory that contains README.md would otherwise look like a blob."
  [rev path]
  (let [out (str/trim (git-text "cat-file" "-t" (str rev ":" path)))]
    (= "tree" out)))

(defn read-ledger [rev]
  (let [directory? (and (path-exists? rev segment-directory)
                        (directory-at? rev segment-directory))
        _ (when (and (path-exists? rev segment-directory) (not directory?))
            (fail! (str segment-directory " must be a directory at " rev)))
        entries (if directory?
                  (remove str/blank?
                          (str/split (git-text "ls-tree" "-z" rev "--" (str segment-directory "/"))
                                     #"\u0000"))
                  [])
        segments (->> entries
                      (keep (fn [entry]
                              (let [[_ mode kind _ path] (re-matches #"([0-7]{6}) ([a-z]+) ([0-9a-f]{40,64})\t(.+)" entry)
                                    name (some-> path (str/replace-first (str segment-directory "/") ""))]
                                (cond
                                  (or (nil? path) (str/includes? name "/"))
                                  (fail! (str "Unexpected receipt segment entry: " entry))
                                  ;; Exempt only a regular blob README.md — never a tree/symlink.
                                  (and (= segment-readme name)
                                       (= "100644" mode)
                                       (= "blob" kind))
                                  nil
                                  (= segment-readme name)
                                  (fail! (str "Receipt segment README.md must be a regular file: " path))
                                  (segment-name? name) (read-segment rev path mode kind)
                                  :else (fail! (str "Receipt segment name must match <UTC timestamp>-<id>.edn: " path))))))
                      (sort-by :segment/path))]
    {:directory? directory?
     :segments (into [(read-legacy rev)] segments)}))

(defn merged-records
  "One ledger: every record once, ordered by :ts then record id (display order)."
  [segments]
  (let [entries (for [{:segment/keys [path records]} segments, r records]
                  {:path path :record r :id (record-id r)})
        repeated (->> entries (group-by :id) vals (filter #(< 1 (count %))))]
    (when (seq repeated)
      (fail! (str "Receipt ledger repeats a record in: " (pr-str (mapv #(mapv :path %) repeated)))))
    (->> entries (sort-by (fn [{:keys [record id]}] [(str (:ts record)) id])) (mapv :record))))

(defn line-subsequence? [^bytes head ^bytes base]
  (let [lines #(str/split (String. ^bytes % "UTF-8") #"\n" -1)]
    (loop [wanted (seq (lines head)) available (seq (lines base))]
      (cond (empty? wanted) true
            (empty? available) false
            (= (first wanted) (first available)) (recur (rest wanted) (rest available))
            :else (recur wanted (rest available))))))

(defn verify-against!
  "Check that `at` extends every ledger in `bases` (one base, or all parents of
  a merge commit); return a summary."
  [bases at]
  (let [base-ledgers (mapv read-ledger bases)
        {head-dir? :directory? head-segments :segments} (read-ledger at)
        _ (doseq [{:keys [segments]} base-ledgers] (merged-records segments))
        head-records (merged-records head-segments)
        head-by-path (into {} (map (juxt :segment/path identity)) head-segments)]
    (doseq [{base-dir? :directory? base-segments :segments} base-ledgers]
      (when (and base-dir? (not head-dir?))
        (fail! (str segment-directory " was removed; the segment directory must remain")))
      (doseq [{:segment/keys [path bytes legacy?]} base-segments :when (not legacy?)]
        (let [head (get head-by-path path)]
          (when-not head (fail! (str "Receipt segment was removed: " path)))
          (when-not (java.util.Arrays/equals ^bytes bytes ^bytes (:segment/bytes head))
            (fail! (str "Receipt segment was edited: " path))))))
    (let [base-ids (into #{} (comp (mapcat :segments) (mapcat :segment/records) (map record-id)) base-ledgers)
          head-ids (into #{} (map record-id) head-records)
          head-legacy (first head-segments)
          new-records (remove #(base-ids (record-id %)) head-records)]
      (when-let [missing (seq (remove head-ids base-ids))]
        (fail! (str "Receipt ledger drops " (count missing) " base record(s)")))
      (when (seq (remove #(base-ids (record-id %)) (:segment/records head-legacy)))
        (fail! (str "The legacy ledger " legacy-path " is frozen; write new receipts as segments under "
                    segment-directory "/")))
      (when-not (some #(line-subsequence? (:segment/bytes head-legacy) (:segment/bytes (first (:segments %))))
                      base-ledgers)
        (fail! (str "The legacy ledger " legacy-path " may only lose whole lines; it was rewritten or reordered")))
      (when-let [invalid (seq (remove receipt-envelope? new-records))]
        (fail! (str "New receipts have invalid envelopes: " (pr-str (vec invalid)))))
      {:receipts/total (count head-records)
       :receipts/new (count new-records)
       :receipts/segments (dec (count head-segments))})))

(defn verify!
  "Check that `at` extends `base` as a segmented ledger; return a summary."
  [base at]
  (verify-against! [base] at))

(defn parse-args [args]
  (loop [args args opts {:base "origin/main" :at "HEAD" :range? false}]
    (cond (empty? args) opts
          (= "--base" (first args))
          (let [[_ value & more] args]
            (when-not value (fail! "--base requires a revision"))
            (recur more (assoc opts :base value)))
          (= "--at" (first args))
          (let [[_ value & more] args]
            (when-not value (fail! "--at requires a revision"))
            (recur more (assoc opts :at value)))
          (= "--range" (first args))
          (recur (rest args) (assoc opts :range? true))
          :else (fail! (str "Unknown argument: " (first args))))))

(defn verify-range!
  "Verify every commit in `base..at` against each of its real parents, so a
  segment cannot be added then edited/deleted inside the range and sibling
  history is never compared out of order; then verify `at` against `base`."
  [base at]
  (let [rows (->> (str/split (git-text "rev-list" "--reverse" "--parents" (str base ".." at)) #"\n")
                  (remove str/blank?)
                  (mapv #(str/split (str/trim %) #" ")))]
    (when (empty? rows)
      (fail! (str "No commits between " base " and " at)))
    (doseq [[rev & parents] rows :when (seq parents)]
      (try (verify-against! (vec parents) rev)
           (catch clojure.lang.ExceptionInfo e
             (if (:receipts/error (ex-data e))
               (fail! (str (ex-message e) " (at commit " (subs rev 0 (min 12 (count rev))) ")"))
               (throw e)))))
    (verify! base at)))  ;; final base→at summary

(defn run
  "Range mode keeps the supplied base (force-pushes compare with the pre-push
  tip); otherwise compare with the merge-base so a branch behind main passes."
  [{:keys [base at range?]}]
  (if range?
    (assoc (verify-range! base at) :receipts/base base)
    (let [base (str/trim (git-text "merge-base" base at))]
      (assoc (verify! base at) :receipts/base base))))

(defn -main [& args]
  (try
    (let [{:keys [at range?] :as opts} (parse-args args)]
      (prn (assoc (run opts) :receipts/at at :receipts/range? range?)))
    (catch clojure.lang.ExceptionInfo e
      (if (:receipts/error (ex-data e))
        (do (binding [*out* *err*] (println "verify-receipts:" (ex-message e)))
            (System/exit 1))
        (throw e)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
