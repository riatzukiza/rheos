---
uuid: 845caf18-17b5-4179-9a8e-156b0e68b67b
title: "Admit complete readonly evidence and canonical adapter capability"
status: incoming
priority: P1
points: 3
labels: planning, readonly, issue5
parent: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
epic: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
---

## Context

[Upstream Rheos issue #5](https://github.com/open-hax/rheos/issues/5) proves a successful zero-task aggregate after an existing ledger append-open was denied by a readonly mount, while direct snapshot/task reads saw the source cards. Native source ef3c4abf1ea75199486f693e9470df3fec88dd49 and installed 0.1.0 reproduce the failure; neither artifact observation qualifies a released repair. Existing diagnosis is attributed, not repeated or promoted to full-suite proof.

## Outcome

Define and consume a reviewed canonical read contract that distinguishes complete input from refusal without initialization or dropped history. Pure evidence/outcome classification should be portable .cljc when practical; filesystem/permissions/clock/errors and protocol calls stay outer adapters.

## Scope

Full issue #5 outcomes1–3 and preparation for4–5. Bind project/config/tasks root, selected projection, ledger identity, read coverage and failure phase. Separate read/write cached capabilities without weakening existing EventAdmission mutations. Current actual supplier is open-hax/eta-mu packages/protocols at0ed56aa74a53a1d1e9c2e55ce95451817a7f3a90; Rheos must consume a qualified owner implementation if supplier changes are needed. No copied donor implementation or automatic migration to Clio.

## Non-goals

No source implementation in this plan, foreign pin movement, new parser/event authority, rewritten past ledger, silently synthesized history or reduced protocol semantics.

## Acceptance criteria

1. Review an explicit portable evidence/result schema and deterministic classification: complete, unavailable/refused or explicitly incomplete. A zero count cannot stand in for inaccessible/failed input; source coverage and project identity are inspectable.
2. Existing lawful readonly ledger/card/config reads perform no mkdir, append-open/O_CREAT, watcher setup, append, rewrite, reorder or operational event emission. Reading does not demand a writable capability.
3. Settle missing-ledger semantics explicitly. Preferred policy refuses absent known history and corrupt/partially parsed history; an approved explicit history-free board needs evidence and distinct labeling. Preserve legitimate explicit empty projections, contained future optional projection leaves and zero filter selections; do not blanket-refuse those existing supported contracts.
4. Preserve task discovery completeness: distinguish missing/inaccessible configured task root, lstat/readdir/readFile failure and malformed selected card from intentional exclusion, skipped symlink/cycle and reviewed optional missing leaf. Neither loader nor event supplier may silently discard a selected failure and return complete.
5. Canonical supplier read capability and cache modes are reviewed with cold/warm reader→writer and writer→reader sequencing, supplied history/failure propagation, independent project/run instances and unchanged write admission. No shared readonly query poisons or upgrades write state.
6. Identify required upstream supplier work and its qualification/pin-consumption hold before implementing Rheos consumption; if source supplier cannot provide the contract, emit a truthful unsupported/refused outcome. Estimate3 and architecture placement are proposals requiring review, not permission to narrow issue #5.

## Verification

Future .cljc laws on BB/JVM and compiled CLJS use complete/empty/failed/mixed/unknown/corrupt facts and negative controls proving no failed source becomes complete. Actual supplier adapter fixtures prove reads never create directories/files or drop parse/permission errors; constructor/watch/write compatibility and cache order are tested. Run full source gates after qualification.

## Risks

Historical parser behavior is not permission to classify malformed history as absent. This story does not grant an eta-mu change or external promotion; required supplier work remains a separately admitted owner prerequisite.
