(ns rheos.backend.extern.ledger-check
  "Read bytes and decode UTF-8 at the Node edge. Never constructs a ledger or board."
  (:require ["node:fs/promises" :as fsp]))

(defn- decode [bytes]
  (try
    {:status :readable :text (.decode (js/TextDecoder. "utf-8" #js {:fatal true}) bytes)}
    (catch :default _
      {:status :malformed :code "INVALID_UTF8"})))

(defn ^:async read-text [path]
  (try
    (decode (await (.readFile fsp path)))
    (catch :default error
      {:status :unavailable :code (if (string? (.-code error)) (.-code error) "IO_ERROR")})))
