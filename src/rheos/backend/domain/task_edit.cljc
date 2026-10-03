(ns rheos.backend.domain.task-edit
  "Plan non-status task edits from already decoded source data.
   Infra owns YAML validation, write-id stamping, file writes and event emission."
  (:require [rheos.backend.law.frontmatter :as law-frontmatter]
            [rheos.backend.shape.content-parser :as content-parser]))

(defn plan-frontmatter-update
  "Admit update keys/values, judge decoded replacement frontmatter and preserve
   the requested event values.
   Returns the supplied rendered raw content, qualified frontmatter, and changes
   in the order updates iterates. Encoding and decoding belong to the adapter."
  [old-frontmatter new-frontmatter new-raw updates]
  {:raw new-raw
   :frontmatter (law-frontmatter/assert-title-shape new-frontmatter)
   :changes (mapv (fn [[key value]]
                    {:key key
                     :old-value (get old-frontmatter (keyword key))
                     :new-value value})
                  (content-parser/checked-updates updates))})

(defn plan-comment
  "Render the proposed comment from already decoded task data; infra stamps it."
  [raw parsed text]
  (content-parser/append-comment raw parsed text))
