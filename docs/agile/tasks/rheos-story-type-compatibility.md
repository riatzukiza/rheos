---
uuid: "4cbb1a2f-354e-46b4-ae9a-955041f59608"
title: "Admit authored story cards without rewriting their type"
type: "task"
status: "review"
priority: "P1"
points: "2"
labels: "compatibility, relationships, admission, cephalon"
created_at: "2026-10-09"
write-id: "1791511459911-0.hsld0vkw3sb4jrmsgrm"
---

# Admit authored story cards without rewriting their type

## Context

The qualified relationship writer merged in personal Rheos PR9 as
`8581e0c390b1fa5a66f74bafff3ac80427174aaa`. Its complete-graph admission
accepts absent legacy type and the exact strings `task` and `epic`, but
rejects `story`. Nine existing Foresight Codex cloud cards use that authored
spelling. The actual receipt-repair transition from Breakdown to Ready
returned exit 3 and `malformed-card-type` for those cards; independent native
readback retained Breakdown. The character-design user explicitly authorized
including this owning compatibility fix in the accepted encounter/mood/recall
milestone.

## Outcome

An exact authored `story` is an ordinary non-epic card for relationship and
dependency admission. Rheos preserves the stored type and identity, rather
than migrating cards or substituting a filtered graph.

## Scope

Change the owning portable relationship admission contract to accept the
literal `story`; document its semantics. Exercise pure JVM/compiled CLJS
decisions and the existing native transition/writeback paths with a complete
board containing stories. Review the intentionally changed old fixture that
classified `story` as malformed. Use the qualified source CLI for consumer
readiness after review; retain source/build hashes and actual native readback.

## Non-goals

No new card type editor, case folding, whitespace trimming, inferred epic,
filename identity, authored-card migration, local Foresight validator, graph
filter, status bypass, alternate reservation/ledger, package publication,
runtime deployment, maker interruption or claim that B1/B2/B3 is complete.

## Acceptance criteria

- [ ] Exact `story` is accepted alongside `task`, `epic` and an absent legacy
  type. Its retained projection still says `story`; opaque stored UUIDs and
  relationships retain their original meanings.
- [ ] A story can be a parent or predecessor and can belong to an actual epic.
  It cannot satisfy an epic target. Missing/ambiguous UUIDs, invalid references,
  cycles and conflicting epic membership remain refused on a complete graph.
- [ ] Explicit nil, keywords, booleans, collections, unknown spellings and
  differently cased or padded type strings remain classified refusals. The
  previous negative `story` fixture becomes an explicit positive compatibility
  regression, with the other negatives retained.
- [ ] Native guarded transitions admit a valid complete story-containing board
  only when its predecessors and original FSM/WIP/command gates pass. Stories
  with unfinished predecessors and malformed unrelated cards still refuse
  admission without changing card or event bytes.
- [ ] A successful native transition preserves unrelated story file bytes,
  body/comments, type and UUID; only the requested subject status/write-id and
  native append-only events may change. Independent canonical readback agrees.
- [ ] After current-head implementation qualification, repeat the actual
  Foresight receipt-repair Ready transition through the qualified Rheos CLI.
  Preserve the nine authored story cards byte-for-byte and the event prefix;
  report any remaining refusal instead of bypassing it.

## Verification

Planning review and lawful native Ready precede implementation. First commit
portable and native failing regressions against the qualified predecessor,
then repair the pure owner and run the existing full lint/test/build gates.
Bind review to both personal head/base and all changed inputs. Record actual
JVM and compiled Node results, CLI hash, subject readback and retained source
and event prefixes. Tests use private boards and real Rheos writers.

## Risks

Expanding the allowed type set can accidentally weaken epic membership or
hide other malformed cards. Positive story cases must be paired with exact
negative types, invalid story-as-epic, cycle and unfinished-predecessor cases.
The source checkout has existing owned review receipts; retain their bytes
and explicit provenance. A source CLI consumed here is not an installed
package release or proof of future board/runtime availability.

Process documentation: GPL-3.0-or-later.
