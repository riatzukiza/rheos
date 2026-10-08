(ns rheos.backend.infra.task-writeback
  "Writing task changes back to markdown files."
  (:require ["node:fs/promises" :as fsp]
            [rheos.backend.infra.content-parser :as content-parser]
            [rheos.backend.law.frontmatter :as law-frontmatter]))

;; The standalone compiler does not transform ^:async/await. A native Promise
;; keeps this filesystem boundary executable without assuming an absent macro.
#_{:clj-kondo/ignore [:promise-chain/prefer-async-workflow]}
(defn write-task-status [task _tasks-dir new-status write-id]
  (let [file-path (:source-path task)]
    (-> (.readFile fsp file-path "utf8")
        (.then (fn [raw]
                 (let [_ (try
                           (law-frontmatter/assert-task-frontmatter-shape
                            (:frontmatter (content-parser/parse-frontmatter raw)))
                           (catch :default error
                             (if (= :refused (:kind (ex-data error)))
                               (throw (ex-info (.-message error)
                                               (assoc (ex-data error)
                                                      :source-path file-path
                                                      :diagnostic (.-message error))
                                               error))
                               (throw error))))
                       updated-raw (-> raw
                                       (content-parser/update-frontmatter "status" new-status)
                                       (content-parser/inject-write-id write-id))]
                   (.writeFile fsp file-path updated-raw "utf8"))))
        (.then (fn [] (assoc task :status new-status))))))
