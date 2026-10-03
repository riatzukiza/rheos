(ns rheos.backend.infra.task-writeback
  "Writing task changes back to markdown files."
  (:require ["node:fs/promises" :as fsp]
            [rheos.backend.infra.content-parser :as content-parser]))

;; The standalone compiler does not transform ^:async/await. A native Promise
;; keeps this filesystem boundary executable without assuming an absent macro.
#_{:clj-kondo/ignore [:promise-chain/prefer-async-workflow]}
(defn write-task-status [task _tasks-dir new-status write-id]
  (let [file-path (:source-path task)]
    (-> (.readFile fsp file-path "utf8")
        (.then (fn [raw]
                 (let [updated-raw (-> raw
                                       (content-parser/update-frontmatter "status" new-status)
                                       (content-parser/inject-write-id write-id))]
                   (.writeFile fsp file-path updated-raw "utf8"))))
        (.then (fn [] (assoc task :status new-status))))))
