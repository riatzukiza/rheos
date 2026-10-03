(ns rheos.backend.extern.content-parser
  "Convert parsed task data at the native JavaScript output boundary.")

(defn task-content->js [parsed]
  #js {:frontmatter (clj->js (:frontmatter parsed))
       :sections (clj->js (mapv (fn [s] #js {:type (:type s) :content (:content s)})
                              (:sections parsed)))})
