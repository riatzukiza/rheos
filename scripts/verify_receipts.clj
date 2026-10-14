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
;; - segment names are real UTC timestamps; segments are non-empty,
;;   newline-terminated, regular files holding exactly one EDN map;
;; - every new record has a valid receipt envelope.
;;
;; TODO(clio/eta-mu): replace this script with eta-mu's receipt/ledger
;; commands built on Clio segment directories (`clio canonicalize-dir`) once
;; receipt-river moves onto Clio after the E1.11 package move-out. Canonical
;; order will then be Clio's cause/stream order; `:ts` is display only.
;;
;; Usage: bb scripts/verify_receipts.clj [--base <rev>] [--at <rev>]
;;   --base defaults to origin/main, --at defaults to HEAD.
(ns verify-receipts
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (java.security MessageDigest)
           (java.time LocalDateTime)
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
    :else value))

(defn record-id [record] (sha256 (pr-str (canonical-form record))))

(defn nonblank-string? [value] (and (string? value) (not (str/blank? value))))

(defn receipt-envelope? [receipt]
  (and (map? receipt)
       (keyword? (:kind receipt))
       (every? nonblank-string? ((juxt :ts :origin :owner :dod :pi :host) receipt))
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

(defn read-records [^String text label]
  (->> (str/split-lines text)
       (remove str/blank?)
       (mapv (fn [line]
               (try (edn/read-string {:default tagged-literal} line)
                    (catch Exception e
                      (fail! (str "Unreadable EDN receipt in " label ": " (ex-message e)))))))))

(defn path-exists? [rev path]
  (zero? (:exit (process/shell {:out :string :err :string :continue true}
                               "git" "cat-file" "-e" (str rev ":" path)))))

(defn read-legacy [rev]
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
    (let [records (read-records (String. bytes "UTF-8") path)]
      (when-not (and (= 1 (count records)) (map? (first records)))
        (fail! (str "Receipt segment must contain exactly one EDN map: " path)))
      {:segment/path path :segment/bytes bytes :segment/records records})))

(defn read-ledger [rev]
  (let [own (remove str/blank? (str/split (git-text "ls-tree" "-z" rev "--" segment-directory) #"\u0000"))
        _ (when (and (seq own) (not (str/starts-with? (first own) "040000 tree ")))
            (fail! (str segment-directory " must be a directory at " rev)))
        entries (remove str/blank? (str/split (git-text "ls-tree" "-z" rev "--" (str segment-directory "/")) #"\u0000"))
        segments (->> entries
                      (keep (fn [entry]
                              (let [[_ mode kind _ path] (re-matches #"([0-7]{6}) ([a-z]+) ([0-9a-f]{40,64})\t(.+)" entry)
                                    name (some-> path (str/replace-first (str segment-directory "/") ""))]
                                (cond
                                  (or (nil? path) (str/includes? name "/"))
                                  (fail! (str "Unexpected receipt segment entry: " entry))
                                  (= segment-readme name) nil
                                  (segment-name? name) (read-segment rev path mode kind)
                                  :else (fail! (str "Receipt segment name must match <UTC timestamp>-<id>.edn: " path))))))
                      (sort-by :segment/path))]
    {:directory? (boolean (seq own))
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

(defn verify!
  "Check that `at` extends `base` as a segmented ledger; return a summary."
  [base at]
  (let [{base-dir? :directory? base-segments :segments} (read-ledger base)
        {head-dir? :directory? head-segments :segments} (read-ledger at)
        _ (merged-records base-segments)
        head-records (merged-records head-segments)
        head-by-path (into {} (map (juxt :segment/path identity)) head-segments)]
    (when (and base-dir? (not head-dir?))
      (fail! (str segment-directory " was removed; the segment directory must remain")))
    (doseq [{:segment/keys [path bytes legacy?]} base-segments :when (not legacy?)]
      (let [head (get head-by-path path)]
        (when-not head (fail! (str "Receipt segment was removed: " path)))
        (when-not (java.util.Arrays/equals ^bytes bytes ^bytes (:segment/bytes head))
          (fail! (str "Receipt segment was edited: " path)))))
    (let [base-ids (into #{} (comp (mapcat :segment/records) (map record-id)) base-segments)
          head-ids (into #{} (map record-id) head-records)
          base-legacy (first base-segments)
          head-legacy (first head-segments)
          new-records (remove #(base-ids (record-id %)) head-records)]
      (when-let [missing (seq (remove head-ids base-ids))]
        (fail! (str "Receipt ledger drops " (count missing) " base record(s)")))
      (when (seq (remove #(base-ids (record-id %)) (:segment/records head-legacy)))
        (fail! (str "The legacy ledger " legacy-path " is frozen; write new receipts as segments under "
                    segment-directory "/")))
      (when-not (line-subsequence? (:segment/bytes head-legacy) (:segment/bytes base-legacy))
        (fail! (str "The legacy ledger " legacy-path " may only lose whole lines; it was rewritten or reordered")))
      (when-let [invalid (seq (remove receipt-envelope? new-records))]
        (fail! (str "New receipts have invalid envelopes: " (pr-str (vec invalid)))))
      {:receipts/total (count head-records)
       :receipts/new (count new-records)
       :receipts/segments (dec (count head-segments))})))

(defn parse-args [args]
  (loop [[flag value & more :as args] args opts {:base "origin/main" :at "HEAD"}]
    (cond (empty? args) opts
          (= "--base" flag) (recur more (assoc opts :base value))
          (= "--at" flag) (recur more (assoc opts :at value))
          :else (fail! (str "Unknown argument: " flag)))))

(defn -main [& args]
  (try
    (let [{:keys [base at]} (parse-args args)
          base (str/trim (git-text "merge-base" base at))]
      (prn (assoc (verify! base at) :receipts/base base :receipts/at at)))
    (catch clojure.lang.ExceptionInfo e
      (if (:receipts/error (ex-data e))
        (do (binding [*out* *err*] (println "verify-receipts:" (ex-message e)))
            (System/exit 1))
        (throw e)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
