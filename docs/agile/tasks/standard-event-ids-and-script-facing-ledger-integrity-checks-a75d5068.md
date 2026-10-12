---
uuid: "af67891c-fbb4-4f40-a6a2-e9bda75d5068"
title: "Standard event IDs and script-facing ledger integrity checks"
status: "incoming"
type: "task"
priority: "P1"
points: "5"
labels: "cephalon, events, identity, provenance, repair"
category: "tasks"
write-id: "1791817778874-0.ncyop7770rstm8s6qvf"
created_at: "2026-10-12T15:09:38.874Z"
---

## Context

The human authorized the historical row repair on 2026-10-12: "You can fix the ledger rows. We should probably have a check for that on the scripts, and a standard way of creating and checking the ids". Muse PR6 retains two distinct title/labels events at ledger rows 76 and 77 with one event/id. Rheos PR16 already replaced timestamp-only event generation with UUID generation, but there is no reusable ID construction/check contract or script-facing ledger identity check.

## Outcome

One standard Rheos event-ID construction and validation boundary is used by the producer and available to scripts/CI. The explicitly authorized Muse data correction makes both existing records independently addressable and retains their original bytes and source revisions as provenance.

## Scope

Extract the existing board/task/UUID event-ID composition into a reusable portable shape/law boundary. Keep entropy generation at the runtime edge. Add a read-only canonical Rheos CLI check that reads every event ledger row, rejects malformed/non-map records and missing/non-string/blank IDs, and reports every duplicate with one-based row locations. Preserve compatible opaque legacy IDs; validation must not require rewriting distinct historical timestamp-based IDs. Event IDs identify individual records; write IDs remain correlation values shared by one multi-field operation.

The owning consumer applies only the two explicitly authorized event/id changes. Archive the exact original two lines, original Git blob/commit, full ledger SHA256 and before/after row hashes, preserving row order, record count, every other field and all other row bytes. This specific human authorization is an exception for these two data corrections, not a standing permission to rewrite receipts or history. Add a consumer CI invocation of the qualified canonical check through a revision-bound Rheos source; do not create a second Muse/Foresight validator.

## Non-goals

No runtime restart, deployment, live publication, unrelated board transitions, deduplication/deletion, broad historical migration, causal-root/write-ID changes, dependency upgrade, alternate identity authority, general-purpose ledger repair protocol or extra review-policy waiver.

## Acceptance criteria

1. The producer uses the shared constructor and generated IDs have a validated UUID suffix with the existing board/task prefix. Invalid creation inputs fail explicitly. Frozen time and two independent real writers retain distinct event IDs; existing event meanings and persistence-before-publication stay intact.
2. The read-only canonical check accepts unique compatible opaque IDs, including legacy timestamp IDs. It detects identical and different-payload duplicate records, missing/null/boolean/blank IDs, non-map data and malformed input with exact physical row numbers. It never silently skips an unreadable row or counts a partial scan as success.
3. The CLI has documented stable success/failure exits and makes no board/card/ledger write while checking, including missing/unreadable input and invalid-data cases. Scripts invoke this owner rather than reimplementing identity rules.
4. The real Muse rows 76 and 77 are corrected under the quoted authorization. Original raw rows remain recoverable; all original payloads, timestamps, write IDs and causal roots remain byte-identical, and all 78 other original rows remain byte-identical and in order. The qualified canonical check verifies the complete corrected ledger.
5. Consumer CI pins and invokes the reviewed owner. A deliberately duplicated fixture fails the same checker and a unique compatible fixture passes. The consumer's ordinary gates and fresh exact-head review/settlement qualify its new revision.

## Verification

Planning review and native Ready precede source implementation. Commit meaningful failing law/CLI tests first, then green portable semantics and outer adapters. Run full existing lint/test/build gates with zero compiler/lint warnings. Exercise actual checker process exits and unchanged fixture bytes, full real ledger validation and negative replay/index fixtures. Record exact input/output/source hashes and actual review IDs. No local test or source plan stands in for native source qualification or live character proof.

## Risks

A syntax reader can accept only a prefix unless whole-row consumption is checked. A uniqueness-only check must not claim complete event-schema validity. Random UUIDs are collision-resistant, so the check remains necessary. Changing original identifiers may affect an external ID-based reference; the correction manifest must retain original identity and exact row binding. The two original events shared a write ID legitimately; changing it would destroy correlation. Existing malformed history outside the authorized pair must be reported separately rather than silently rewritten.

Process documentation: GPL-3.0-or-later.
