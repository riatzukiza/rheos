---
uuid: "a6de84ce-06c4-4556-a9ba-6bbb78c7b60a"
title: "Lawful incoming-card epic correction"
status: "in_progress"
type: "task"
priority: "P1"
points: "3"
labels: "cephalon, admission, provenance, repair"
dependency: ["62b3d579-70a2-4670-9d7c-a23bc89b06f8"]
category: "tasks"
write-id: "1791751541319-0.2ez5snyhsord63ndalr"
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

---