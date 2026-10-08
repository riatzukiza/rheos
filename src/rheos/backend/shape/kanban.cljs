(ns rheos.backend.shape.kanban
  "Malli schemas for kanban domain types."
  (:require [malli.core :as m]))

(def StatusOrder
  ["icebox" "incoming" "accepted" "breakdown" "blocked" "ready"
   "todo" "in_progress" "review" "document" "done" "rejected"])

(def Task
  [:map
   [:uuid :string]
   [:title :string]
   [:slug :string]
   [:status :string]
   [:priority :string]
   [:labels [:vector :string]]
   [:created-at :string]
   [:content :string]
   [:source-path :string]
   ;; Read projections retain raw malformed legacy values with diagnostics.
   ;; Accepted writes are separately governed by shape/domain.relationships.
   [:type {:optional true} :any]
   [:parent {:optional true} :any]
   [:epic {:optional true} :any]
   [:dependency {:optional true} :any]
   [:relationship-errors {:optional true} [:vector :any]]
   [:domain {:optional true} [:maybe :string]]
   [:org {:optional true} [:maybe :string]]
   [:drift {:optional true} [:maybe :boolean]]])

(def ColumnSnapshot
  [:map
   [:status :string]
   [:title :string]
   [:task-count :int]
   [:tasks [:vector Task]]])

(def BoardSnapshot
  [:map
   [:generated-at :string]
   [:total-tasks :int]
   [:columns [:vector ColumnSnapshot]]])

(def ProjectConfig
  [:map
   [:id {:optional true} :string]
   [:title {:optional true} :string]
   [:tasks-dir :string]
   [:meta {:optional true} [:map-of :keyword :any]]])

(def Project
  [:map
   [:id :string]
   [:title :string]
   [:tasks-dir :string]
   [:meta [:map-of :keyword :any]]])

(defn valid? [schema value]
  (m/validate schema value))
