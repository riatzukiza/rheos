---
uuid: 2d7c391d-b01b-4386-93ec-51a558bfa155
title: "Qualify readonly composition without weakening writes or release gates"
status: incoming
priority: P1
points: 2
labels: planning, readonly, issue5
parent: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
epic: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
dependency: 63796a65-d27d-430a-a7f6-8f1df8a48744
---

## Context

[Upstream Rheos issue #5](https://github.com/open-hax/rheos/issues/5) records silently successful zero-task composition after an existing ledger append-open failed on a readonly mount. The source diagnosis and direct snapshot/task controls are evidence, not an implemented fix or released qualification.

## Outcome

Provide observable source qualification for the complete repair and distinguish it from published package and installed-runtime readiness. Proposed size 2 depends on the two earlier stories; review must confirm whole scope or lawfully break it down.

## Scope

All five issue outcomes: full regression/adversarial matrix, input provenance, source/tool/gate receipts, runnable human fixture proof and distinct release-consumption handoff.

## Non-goals

This lane invokes no product test/build/provider commands or CI rerun during local preparation; inherited hosted workflows may run or skip after publication and their actual results will be reported separately; no package publication, CI waiver, install-policy rewrite, real project transition, signing secrets or live shared services.

## Acceptance criteria

1. Red uses actual accepted native CLI/API/MCP/HTTP code on existing readonly ledger+cards, with direct snapshot/task controls proving readable sources. Require truthful complete success or the reviewed nonzero structured refusal; reject exit 0 complete-empty disguises. Preserve known drift true/false and exact source/config/task roots.
2. EROFS/EACCES/missing/corrupt ledger, partial/unreadable cards/roots, all-fail/mixed/order/filter fixtures have meaningful independent negative controls. chmod under a privileged user is insufficient; use contained readonly mounts or explicit nonprivileged identity. Fixtures own every directory/cache/temp and never touch real project ledgers.
3. Genuine empty board, empty projection, allowed future leaf, zero filter selection, projection containment/symlink/cycle and existing filters stay green. Compare complete before/after bytes, modes, symlink targets and path manifests; observe no read mkdir/O_CREAT/append/watch through syscall or canonical adapter-effect controls. Counts alone are insufficient.
4. Cold/warm read→write/write→read, parallel independent project contexts and reset/cleanup retain old event/ledger prefixes. Legal existing mutations append exactly documented events/write identities; refused FSM/WIP/build-gate mutations perform no write. Lifecycle/gates/transition/event contracts remain unchanged.
5. Future full pnpm test, pnpm lint and pnpm build complete with nonempty assertions, zero failures/errors/warnings and all four server/cli/github-sync/app outputs. Retain source bootstrap pins and personal PR #2 frozen-install hold; no namespace filtering or ignored child/compiler exit. New portable laws need actual BB/JVM plus compiled CLJS discovery.
6. Provide a future owned runnable human script/guide with exact runtime/source/artifact identity and success/refusal observations across surfaces, no real board/provider. Separately verify released archive/package contents, documented cold install/Node behavior and consumer provenance under Eta Mu #239's full eight criteria. Source-built ef3, installed 0.1.0 and repaired release remain distinct. Native planning and lawful readiness precede implementation; the proposed 2-point story and 8-point epic require whole-scope review or explicit lawful breakdown.

## Verification

Retain meaningful red then green logs, every expected nonzero result, versions, source hashes and full input manifests. Full source test/build/lint and existing lifecycle regressions must complete; missing tools/supplier/install qualification remain visible holds. Package publication is a separate owner action after qualification.

## Risks

Privileged permission tests can falsely succeed; async cleanup and shared capability caches can cross contexts; installed version/source compilation cannot prove distribution. Existing tests may expect ledger initialization: preserve lawful empty intent while forbidding hidden read writes.
