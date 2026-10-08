(ns rheos.backend.infra.publication
  "One inter-process reservation for participating canonical board writers.
   No lease timeout or PID guess transfers ownership. A crashed/unknown owner
   remains a visible conflict until explicitly recovered outside this operation."
  (:require ["node:fs/promises" :as fsp]
            ["node:path" :as path]
            ["node:crypto" :as crypto]))

(defn conflict! [message data]
  (throw (ex-info message (assoc data :kind :conflict))))

(defn ^:async with-reservation!
  "Reserve the real project root exclusively, execute f, and release only the
   exact owned token in finally. This coordinates CLI and service processes;
   nonparticipating manual editors still require source-revision checks."
  [project f]
  (let [root (try (await (.realpath fsp (:tasks-dir project)))
                  (catch :default error
                    (throw (ex-info "Unavailable selected project root"
                                    {:kind :refused :cause :incomplete-projection
                                     :source-path (:tasks-dir project) :diagnostic (.-message error)} error))))
        dir (path/join root ".rheos-writer-reservation")
        owner-path (path/join dir "owner.json")
        token (.randomUUID crypto)
        owner (js/JSON.stringify #js {:token token :pid js/process.pid
                                     :createdAt (.toISOString (new js/Date))})]
    (try
      (await (.mkdir fsp dir))
      (catch :default error
        (if (= "EEXIST" (.-code error))
          (conflict! "Another or unresolved canonical writer owns the project reservation"
                     {:reservation-path dir})
          (throw error))))
    (try
      (await (.writeFile fsp owner-path owner #js {:encoding "utf8" :flag "wx" :mode 384}))
      (catch :default error
        (try (await (.rmdir fsp dir)) (catch :default _ nil))
        (throw error)))
    (try
      (await (f))
      (finally
        (let [current (await (.readFile fsp owner-path "utf8"))]
          (when-not (= owner current)
            (conflict! "Reservation ownership changed; refusing cleanup"
                       {:reservation-path dir :token token}))
          (await (.unlink fsp owner-path))
          (await (.rmdir fsp dir)))))))

(defn ^:async file-and-event!
  "File plus ledger is not atomic. A failed file or event effect reports the
   attempted phase and actual readback, never success. No retry or rollback."
  [{:keys [source-path write-id]} write! emit!]
  (let [phase (atom :file-write)]
    (try
      (let [result (await (write!))]
        (reset! phase :event-append)
        (await (emit!))
        result)
      (catch :default error
        (if (or (= :conflict (:kind (ex-data error)))
                (and (= :file-write @phase)
                     (= :refused (:kind (ex-data error)))
                     (= :create-conflict (:cause (ex-data error)))))
          (throw error)
          (let [readback (try
                           (let [bytes (await (.readFile fsp source-path))]
                             {:bytes (.-length bytes)
                              :sha256 (.digest (.update (.createHash crypto "sha256") bytes) "hex")})
                           (catch :default read-error
                             {:unavailable (.-code read-error)}))]
            (throw (ex-info "Canonical writer effect failed; native repair required"
                            {:kind :partial-effect :phase @phase
                             :source-path source-path :write-id write-id
                             :file-readback readback :diagnostic (.-message error)} error))))))))
