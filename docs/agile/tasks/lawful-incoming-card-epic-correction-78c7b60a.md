---
uuid: "a6de84ce-06c4-4556-a9ba-6bbb78c7b60a"
title: "Lawful incoming-card epic correction"
status: "done"
type: "task"
priority: "P1"
points: "3"
labels: "cephalon, admission, provenance, repair"
dependency: ["62b3d579-70a2-4670-9d7c-a23bc89b06f8"]
category: "tasks"
write-id: "1791754954073-0.m03bzsuivgmgijpwjj"
created_at: "2026-10-11T19:29:55.518Z"
---

# Lawful incoming-card epic correction

## Context

Muse PR6 coordinator `bb5b1ee2-111c-4762-8321-889a7256b2f9` is Incoming
and has epic category/labels, but lacks the actual `type: epic`. Two Incoming
children name it as both parent and epic. Native `frontmatter --set type=epic`
refuses the protected/unknown key; removing only one child's epic also refuses
because the complete proposed graph remains invalid. Findings 4242786186 and
the actual native refusal proof retain this distinction. The human included
the narrow owning repair; hand-editing operational type or filtering away the
other child is not authorized.

## Outcome

The existing canonical writer can correct an Incoming ordinary card to an
actual epic when, and only when, the complete proposed graph is lawful. The
correction retains UUID, status, source identity, body and prior provenance.

## Scope

Add a portable conditional admission law for this one direction: a card whose
canonical current status is incoming and whose prior type is absent, task or
story may become exact epic. Route it through the existing frontmatter writer
and its complete selected-board snapshot, reservation, revision recheck and
file/event publication boundary. Keep public CLI/HTTP/MCP diagnostics and
documentation consistent; do not add an alternate operational command engine.
An already-epic Incoming card's identical request may be an admitted semantic
no-op only after whole-graph validation, with no file/event effect.

## Non-goals

No general type editor, epic downgrade, status transition/bypass, mutable UUID,
identity/provenance rewrite, inference from labels/category/title, partial graph,
manual live type patch, caller-local parser, new board engine, broad repair of
other cards, release/deployment or character milestone completion.

## Acceptance criteria

- [ ] The pure law admits absent/task/story to exact epic only for an Incoming
  subject and a valid complete post-update graph. It can repair existing
  child epic references to that subject; it does not require the inherited
  graph to be valid before the lawful correction is considered.
- [ ] Other target types, malformed prior types, non-Incoming subjects,
  unknown/ambiguous subjects, unrelated malformed cards, missing references,
  cycles and conflicting memberships refuse before any write or event.
  UUID/status/write-id/provenance updates remain protected, including mixed
  batches. An epic downgrade is refused even if its proposed graph is valid.
- [ ] The native complete-board writer preserves subject identity, status,
  body/comments and unrelated card bytes. The successful event accurately
  records the old/new type; all historical events retain their exact prefix.
- [ ] Concurrent source change, publication reservation contention and
  file/event failures retain the existing classified failure behavior. The
  correction cannot reuse a stale graph decision or bypass a lower-level
  writer guard. CLI, HTTP and MCP use that same canonical admission.
- [ ] After current-head qualification and exact CLI adoption, the actual Muse
  correction is performed natively. Independent readback proves exact epic,
  both children's retained UUID relationships and scalar dependency, and an
  unchanged original event prefix. Remaining Ready/FSM/review gates still apply.

## Verification

Planning qualification and native Ready precede portable/native RED, then
domain/adapters GREEN. Exercise JVM and compiled CLJS laws and real guarded
writers on complete owned boards, with refusal/no-op byte checks and existing
reservation/publication regressions retained. Run full lint/test/build gates.
Record source/build hashes, exact commands and independent native consumer
readback. No local fixture alone admits the real Muse board.

## Risks

Adding type to a general descriptive allowlist would accidentally open every
status and direction; the conditional authority must stay separate. Snapshot
status normalization must use the existing canonical loader while retaining
stored bytes. The full graph can remain invalid for unrelated reasons, in which
case the real correction must truthfully refuse instead of widening this scope.

Process documentation: GPL-3.0-or-later.


---
Implementation plan after qualified event-ID predecessor Done: commit portable and real native writer RED for exact Incoming absent/task/story to epic, full proposed graph validation, same-epic no-op, protected/mixed/type/status/refusal cases and byte/event custody. Then implement conditional authority at the existing common admission, propagate public CLI/HTTP/MCP diagnostics/schema, retain reservation/revision/publication gates, run JVM/CLJS/full lint/test/build, and obtain fresh exact-head source review before real Muse correction. No general type editor or runtime deployment.

RED4ff8cd0 committed: compiled262tests2567assertions63fail0errors, JVM31tests671assertions54fail0errors; initial delimiter compile failure retained separately. Candidate GREENcompiled262tests2606assertions0fail/error and JVM31/6710fail/error; full lint0errors0warnings8baselineinfos. Conditional Incoming exactepic guard is separate from descriptive mutable keys; CLI/HTTP/MCP share complete-graph admission, real writer no-op/custody/refusal/reservation/revision/file-event fixtures pass. Native Review full build/lint/test gate now starts; completion and release hashes are not yet claimed. Actual Muse epic correction waits qualified source adoption.

Qualified personal PR17 MERGED ae5cebca6124dae5fdd47707647b07daf10a8a4a, exact reviewed head8ef43eb2cb590f465552f462c8743a04282603fd/base7712a86d49c53f3c73d40058092b3f610fce5e7f/tree58edd246a4a3b20fe81f080d61bdabe1cbad3de1. Canonical gate PASS, one complete available configured cohort CodeRabbit/Codex/MiMo, three passing checks, zero finding threads. Real qualified CLI SHA85369213440634989365f75929045d36eb69b6c6b1e7679f68d483374a0d526b corrected actual Muse coordinatorbb5b1ee2-111c-4762-8321-889a7256b2f9 from absent type to exact epic while Incoming. Independent native coordinator/two-child reads pass; both parent/epic links and B scalar dependency A retained; body/UUID/status,16unrelated cards and journals unchanged. Whole57074-byte original event prefix SHA3241b199e71ce90031492fe153762a0d8415e891a5ef346820843b40229e6c38 retained; one477-byte native type event SH2822a793922f8a6ce1cf80084ce09a442243abea3ecf25785a1b9af5df684782, newIDkanban-bb5b1ee2-111c-4762-8321-889a7256b2f9-d3d23f00-5d6d-4bec-9c85-1290ad32f7c5. Both original duplicate-ID rows remain unchanged. Private observed-native proof SHAfd8769b463799679ee3f2c6472bf7bef24d9f032e12cf1a030c9f6fd670338a2. Initial operator repeat-card assertion was vacuous and gets no custody credit; separate actual frozen17-card/ledger/journal native repeat proved byteexact noop, correctionproof SHA9fdea9392f138155ecc6222af19b3ab481da40599e255b9e345bea74b82d5db9. Acceptance consumer proof now complete for this narrow card. No character field/mood/live recall or wider epic completion claimed.
Qualified full CLI SHA256: 85369213440634989365f75929045d36eb69b6c6b1e7679f68d483374a0d526b.

---