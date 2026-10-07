(ns rheos5.planning-receipt
  (:require [eta-mu.receipt-river.api :as api]
            [eta-mu.receipt-river.domain.receipt :as receipt]
            [eta-mu.receipt-river.shape.edn :as edn]
            ["node:fs" :as fs]))
(let [[path] *command-line-args*
      now (.toISOString (js/Date.))
      payload (receipt/build-payload
               {:kind :decision :owner "root/issues" :origin "Rheos5 full readonly-composition planning"
                :dod "All five native outcomes; proposed epic8 and stories3/3/2; canonical complete/refused evidence, full native surfaces/cache/history/writes and future qualification"
                :pi "Planning proposal only; no implementation, native approval, lifecycle transition or release admission"
                :host "Independent complete persistent store; copied Node22.20.0 and actual NBB1.4.207; read-only no-network owner CLI fixture"
                :manifest "docs/agile/tasks/readonly-composition-epic.md;readonly-evidence-contract.md;readonly-native-surfaces.md;readonly-composition-qualification.md;docs/notes/readonly-composition-planning.md;docs/verification/readonly-composition-planning.md;.ημ/verification/readonly-composition-planning"
                :refs "https://github.com/open-hax/rheos/issues/5;parent ea92504b9e09d06320a64aa3a7b595d47cdfe903;accepted ef3c4abf1ea75199486f693e9470df3fec88dd49;RR154440f3c997aa9208194bba59b5edbef3654f78;attributed diagnosis7c0a8f1c084b20768155901cf84f749047e31d93"
                :note "Complete five outcomes retained; strict/partial/history/ABI/adapter/size remain review decisions. Four native read-task reads and help exit0, five diagnostic fixture inputs byte/mode exact; ephemeral config not accepted board/WIP/readiness. Previous source-built CLI791418 artifact not new compile/release. No product tests/build/provider/CI rerun. Initial outside filename collision and account-path selection guard refusal preserved as own preparation errors, no foreign/source operational effect. Parent1–4 ownership/supplier and Eta239 distribution holds explicit; root sole DRAFT blocked/autooff publisher."}
               "open-hax/rheos" now :decision)
      event (api/build-event {:event-id (str (random-uuid)) :recorded-at now
                              :component-manifest {:eta-mu/version "1.1.1"}
                              :command "prepare full Rheos5 planning" :producer {:actor "root/issues"}
                              :subject {:repo "open-hax/rheos"}} payload)
      line (edn/format-line event)
      result (api/validate-line line 14)]
  (assert (:ok result))
  (assert (= :declared (get-in result [:source/schema :status])))
  (.appendFileSync fs path (str line "\n"))
  (println (js/JSON.stringify (clj->js (select-keys result [:ok :line-number :errors :source/schema])))))
