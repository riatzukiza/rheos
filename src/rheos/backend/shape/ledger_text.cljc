(ns rheos.backend.shape.ledger-text
  "Strict per-physical-row EDN decoding. No filesystem or board configuration."
  (:require [clojure.string :as str]
            #?(:cljs [cljs.tools.reader.edn :as edn]
               :clj [clojure.tools.reader.edn :as edn])
            #?(:cljs [cljs.tools.reader.reader-types :as reader-types]
               :clj [clojure.tools.reader.reader-types :as reader-types])
            #?(:cljs [cljs.tools.reader.impl.utils :as reader-utils]
               :clj [clojure.tools.reader.impl.utils :as reader-utils])))

(defn physical-lines [text]
  (if (empty? text)
    []
    (let [lines (vec (str/split text #"\r?\n" -1))]
      ;; Exactly one terminal record delimiter does not add a phantom row.
      (if (str/ends-with? text "\n") (pop lines) lines))))

(defn- token-boundary? [ch]
  (or (reader-utils/whitespace? ch)
      (contains? #{\( \) \[ \] \{ \} \" \;} ch)))

(defn- reader-discard? [text]
  ;; EDN's reader deliberately hides discarded forms, even on the second EOF
  ;; read. Detect only the actual dispatch token before decoding. Literal text
  ;; in strings/comments/chars and # inside an already-started symbol remain
  ;; the reader's responsibility; this scanner never interprets an EDN value.
  (loop [i 0 state :normal]
    (if (>= i (count text))
      false
      (let [ch (nth text i)]
        (case state
          :string (cond
                    (= ch \\) (recur (+ i 2) :string)
                    (= ch \") (recur (inc i) :normal)
                    :else (recur (inc i) :string))
          :comment (recur (inc i) (if (#{\newline \return} ch) :normal :comment))
          :token (if (token-boundary? ch)
                   (recur i :normal)
                   (recur (inc i) :token))
          :char (if (token-boundary? ch)
                  (recur i :normal)
                  (recur (inc i) :char))
          (cond
            (= ch \;) (recur (inc i) :comment)
            (= ch \") (recur (inc i) :string)
            ;; A character literal consumes at least its following character,
            ;; including a quote, semicolon, backslash or dispatch character.
            (= ch \\) (recur (+ i 2) :char)
            (token-boundary? ch) (recur (inc i) :normal)
            (and (= ch \#) (= \_ (get text (inc i)))) true
            :else (recur (inc i) :token)))))))

(defn decode-row [row text]
  (if (reader-discard? text)
    {:row row :error :discarded-edn-form}
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
      {:row row :error :invalid-edn}))))

(defn decode-text [text]
  (mapv (fn [index line] (decode-row (inc index) line))
        (range) (physical-lines text)))
