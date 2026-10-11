---
uuid: "62b3d579-70a2-4670-9d7c-a23bc89b06f8"
title: "Distinct future native event identities"
status: "in_progress"
type: "task"
priority: "P1"
points: "2"
labels: "cephalon, admission, provenance, repair"
category: "tasks"
write-id: "1791749829833-0.0vwgcwnd1nzma28vwwt7"
created_at: "2026-10-11T19:29:54.964Z"
---

# Distinct future native event identities

## Context

The native Muse planning edit emitted two frontmatter events at the same
millisecond on 2026-10-11. Both retained the event ID
`kanban-bb5b1ee2-111c-4762-8321-889a7256b2f9-1791744352193`, while
their title/labels payloads and causal/root identities differ. The owning
`kanban-envelope` currently identifies events by board, task and `Date.now()`.
Muse PR6 findings 4242786176 and 4242844100 identify the collision. The human
included this narrow owning prerequisite in the accepted encounter/mood/recall
milestone. Existing receipts and events are immediately append-only.

## Outcome

Every future native event emission receives an independently generated,
collision-resistant identity. Frozen or coarse wall-clock time cannot make
distinct emissions reuse an event ID. Original events remain inspectable.

## Scope

Repair identity construction at the shared owning emission chokepoint. Keep
board/task identity, event type, timestamp, payload, write correlation,
causal/root provenance, awaited persistence and notification ordering intact.
Use a host identity primitive at the effect boundary; keep any reusable shape
or decision portable. Exercise all existing native event families and the real
multi-field writer. Consumer correction uses a native comment referring to
the original collision and exact retained rows after qualified adoption.

## Non-goals

No renumbering, deletion, deduplication or reordering of historical events;
no ledger migration, new correction protocol, local consumer event writer,
clock/status redesign, dependency upgrade, package release, paid review,
runtime restart, deployment or claim that the character milestone is complete.

## Acceptance criteria

- [ ] Distinct emissions for the same board/task/type remain distinct when
  time is held constant, including multiple fields from one real writer batch.
  Independent instances do not depend on a shared in-memory counter alone.
- [ ] All native event families retain their exact event vocabulary, board,
  task, timestamp, payload, source/agent and write correlation meanings. The
  identifier remains an opaque string accepted by the existing admission and
  event readers; no consumer parsing/migration is required.
- [ ] Persistence is still awaited before publication. Refused writes and
  publication/reservation failures retain their existing visible outcomes;
  generating a nonce cannot turn an unpersisted event into a success.
- [ ] Tests retain a seeded historical duplicate-ID prefix byte-for-byte.
  New event appends have distinct IDs and remain independently readable.
- [ ] After qualified source adoption, an actual native Muse corrective
  comment identifies both original rows by ID, location and exact byte hashes.
  Independent readback proves the comment and original prefix coexist. This
  documents the historical collision without claiming the old IDs are fixed.

## Verification

Planning qualification and native Ready precede implementation. Commit failing
compiled regressions against the qualified predecessor, then repair the owner.
Run existing full lint/test/build gates and meaningful frozen-clock native
writer tests. Record exact source/CLI hashes, actual commands, independently
read-back events and append-only prefix checks. Owned fixtures clean up in
finally. Source approval and real consumer adoption are separate evidence.

## Risks

Random identities provide collision resistance, not a mathematical proof of
global uniqueness. Time freezing must not accidentally alter UUID generation
or skip real ledger admission. Historical duplicate IDs remain a known data
defect; corrective provenance must distinguish their different immutable rows.

Process documentation: GPL-3.0-or-later.


---
RED committed1caf71f0c7449fdfc60919feb1252cc25da15f36:255tests2396assertions3collision failures0errors. Candidate GREEN255tests2396assertions0fail/error; lint0errors0warnings8baselineinfos; all4release builds0warnings. Four separate candidate CLI processes under fixed Date.now emit four distinct IDs and retain the original duplicate-ID prefix; actual native readback and finally cleanup pass. Candidate CLI SHA25634c3092e8862ff550b2c22fbff91dee3b506f935cc60d0f0d146a424d0a6fc70. Source review/qualified adoption/actual Muse corrective comment remain outstanding; dependent epic-correction task stays Incoming. No live runtime or publication change.

---