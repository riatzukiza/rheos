---
uuid: "adopt-qualified-event-id-checks-in-the-muse-consumer"
title: "Adopt qualified event-ID checks in the Muse consumer"
status: "incoming"
type: "task"
priority: "P1"
points: "2"
labels: "cephalon, events, identity, consumer, provenance"
dependency: ["af67891c-fbb4-4f40-a6a2-e9bda75d5068"]
category: "tasks"
write-id: "1791818730059-0.ztb8eohw33ck66he87w"
created_at: "2026-10-12T15:25:30.059Z"
---

## Context

The human authorized the two colliding Muse ledger-row corrections and requested standard creation/checking plus script checks. Muse commit8acedb549a93c7a97634f9f40a4844a34e3285e3 already corrects event IDs in physical rows76/77 and retains original raw rows, source Git blob and exact hashes. Rheos owner taskaf67891c-fbb4-4f40-a6a2-e9bda75d5068 separately supplies the future constructor/checker. This rollout card is recorded on the same coordinating Rheos board so its UUID dependency is native and checkable; implementation and the consumer PR remain in personal Muse.

## Outcome

Muse scripts/CI invoke the qualified revision-bound Rheos owner checker on the active ledger, and the complete corrected ledger passes. Owner completion does not depend on this consumer rollout completing.

## Scope

Retain the expressly authorized two-row correction and original-row custody. After the owner implementation is qualified and its card Done, pin its exact reviewed source/release and invoke its canonical CLI/script API from Muse CI. Add a deliberately duplicated owned fixture that fails that same owner and a unique compatible legacy fixture that passes. The actual active ledger must pass the canonical check. Record actual checker/process exits and source/input hashes, and qualify the consumer change with full ordinary gates and fresh exact-head reviews.

## Non-goals

No Muse/Foresight parser or validator, ledger deduplication/deletion, broad migration, timestamp/causal-root/write-ID changes, owner gate bypass, namespace-loss CLI repair, provider/model/credential changes, runtime/deployment/publication, field/mood completion or general-purpose history-repair protocol.

## Acceptance criteria

1. Original two rows, immutable source commit/blob, before/after ledger hashes and old/new row bindings remain recoverable. The correction changes only two ID strings; the other78 original rows and all non-ID fields/order/count remain exact. Subsequent provenance appends are distinct new records, not a false claim that the complete corrected head equals its older prefix.
2. Dependency af67891c-fbb4-4f40-a6a2-e9bda75d5068 is retained. Consumer adoption begins only after qualified owner implementation and native Done, never from a planning approval alone. Pin the actual qualified source/release and verify that invocation uses that binding.
3. Consumer CI checks an explicit active-ledger path through the owner. It does not recursively scan custody archives: original-rows.edn intentionally preserves the old duplicate IDs. Missing/unreadable/empty/malformed/duplicate data fail through the owner contract with nonzero exits.
4. A duplicate fixture fails and a unique compatible legacy fixture passes the exact same owner API; both fixture bytes remain unchanged. The complete real corrected Muse ledger passes, with actual report and source/input SHA retained. This is ID integrity, not full event-schema or Mongo import proof.
5. The consumer's normal CI/build/lint/tool gates and fresh current-head source reviews/settlements qualify an ordinary history-preserving MERGE. Approval, cohort and completion do not transfer from the owner. Owning Muse transport readiness and B1/B2/liveB3 remain separate.

## Verification

This is a separate two-point consumer rollout estimate, dependent on the three-point owner story; the split retains the original total five-point estimate. Estimates remain provisional for independent planning review. Native Ready precedes committed consumer RED/GREEN controls. Preserve both the raw historical archive and all receipt/reflection prefixes. No local demonstration or source plan supplies qualified adoption or live character completion.

## Risks

A wrong target path can pass only if the checker wrongly accepts missing/empty data; owner refusal tests remain mandatory. Original ID-based external references may exist, so original bindings and hashes must remain visible. A copied validator or unqualified owner pin would fork authority. Provider review outages are not quota or approval evidence.

Process documentation: GPL-3.0-or-later.
