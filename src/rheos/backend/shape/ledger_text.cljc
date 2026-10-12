(ns rheos.backend.shape.ledger-text
  "Strict per-physical-row EDN decoding. No filesystem or board configuration."
  (:require [clojure.string :as str]
            #?(:cljs [cljs.tools.reader.edn :as edn]
               :clj [clojure.tools.reader.edn :as edn])
            #?(:cljs [cljs.tools.reader.reader-types :as reader-types]
               :clj [clojure.tools.reader.reader-types :as reader-types])))

(defn physical-lines [text]
  (if (empty? text)
    []
    (let [lines (vec (str/split text #"\r?\n" -1))]
      ;; Exactly one terminal record delimiter does not add a phantom row.
      (if (str/ends-with? text "\n") (pop lines) lines))))

(defn decode-row [row text]
  (try
    (let [reader (reader-types/string-push-back-reader text)
          ;; An EDN value cannot forge this local function sentinel.
          eof (fn [])
          value (edn/read {:eof eof} reader)]
      (cond
        (identical? value eof) {:row row :error :missing-edn-form}
        (not (identical? (edn/read {:eof eof} reader) eof)) {:row row :error :extra-edn-form}
        :else {:row row :record value}))
    (catch #?(:cljs :default :clj Exception) _
      {:row row :error :invalid-edn})))

(defn decode-text [text]
  (mapv (fn [index line] (decode-row (inc index) line))
        (range) (physical-lines text)))
