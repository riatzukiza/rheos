---
uuid: "rheos-issue-3-relationship-writer"
title: "Route relationship creation and edits through the canonical writer"
type: "task"
status: "incoming"
priority: "P1"
points: "5"
labels: "relationships, adapters, ledger, planning"
epic: "rheos-issue-3-relationship-authoring"
parent: "rheos-issue-3-relationship-authoring"
dependency: "rheos-issue-3-relationship-contract"
created_at: "2026-10-06"
---

# Route relationship creation and edits through the canonical writer

## Context

Existing creation, task-edit and transition chokepoints already own writes.
Relationship edits must be distinct from permissive descriptive-key updates
while preserving those authorities. This story follows the accepted pure
contract, not an independent interpretation of UUID or graph semantics.

## Outcome

Existing creation and frontmatter surfaces admit accepted relationships through
one canonical decision/write path, and canonical reads retain them. Their events
use the existing event vocabulary with accurate old/new values and provenance.

## Scope

Wire the accepted contract into creation and existing-card edits; retain fields
through task loading/shapes and content/board/event projections. Expose the
reviewed CLI flags and HTTP/MCP schemas, with boundary conversion to Clojure data.
Admit the existing `--set dependency=<existing-uuid>` syntax through the reviewed
relationship contract; the current engine refuses that dependency edit.

Also consume retained dependencies at the existing FSM transition chokepoint.
The selected consumer requires missing/unfinished/cyclic predecessor refusal
and completed-predecessor admission before character implementation. A graph
reference being well-formed does not establish that its predecessor finished.
The portable relationship contract's non-goal of status admission is retained;
the additional pure lifecycle decision belongs to the owning transition law,
with host facts and effects supplied by its existing infra path.

## Non-goals

No replacement parser, alternate command engine, status/frontmatter bypass,
foreign caller edits, second ledger or broad rewrite of unrelated descriptive
fields. No credential, signing or deployment change.

## Acceptance criteria

- [ ] Each public surface uses the accepted contract and existing creation or
  edit chokepoint; no caller can gain unvalidated relationships by calling a
  lower-level writer directly. Identity/status/provenance updates remain refused.
- [ ] Create and edit cover all reviewed fields, cardinalities, replacements
  and removals. CLI help, command registry, HTTP errors and MCP schemas/documented
  examples agree with the actual surface; no unsupported command is advertised.
- [ ] Valid writes preserve body/comments, unrelated frontmatter and immutable
  identity/provenance. Refusals and no-ops preserve bytes and emit no successful
  mutation event. One successful batch has its reviewed correlation/event rules.
- [ ] Reads and existing event replay recover the same accepted references and
  task type needed for epic checks. Hand-authored Markdown remains supported;
  malformed legacy relationships follow the reviewed visible policy.
- [ ] Validation and publication obey the accepted concurrency contract. A stale
  snapshot cannot silently publish a relationship rejected by the current graph;
  partial file/event failure remains visible and is not reported as success.
- [ ] The owning FSM/configuration explicitly names the successful predecessor
  states and guarded implementation-admission transitions. For the selected
  Promethean workflow, the proposal is `done` as success and admission into
  `ready`, `todo` and `in_progress` as guarded boundaries. Fresh review must
  settle this policy before RED; no consumer-side status list is introduced.
- [ ] Each guarded transition validates the complete selected-project snapshot
  and all reachable dependency predecessors. Missing/ambiguous references,
  malformed relationships, dependency cycles, unavailable complete reads and
  unfinished predecessors produce a classified refusal naming the actual
  blockers. A predecessor's rejected/archived status is not assumed successful.
  Parent/epic edges retain their distinct reviewed graph meaning.
- [ ] Completed-predecessor admission still requires the original FSM edge,
  WIP limit and executable gates. No-dependency cards retain those obligations.
  CLI, HTTP and MCP cannot bypass the same canonical transition decision.
- [ ] Relationship edits, creation and guarded transitions share the reviewed
  inter-process publication/reservation boundary. Revalidate after executable
  gates and before effects; a changed predecessor or relationship cannot be
  published from an earlier accepted snapshot. Refusal preserves card/history
  bytes; partial file/event failure stays visible. Timeout alone grants no
  authority to steal a live writer's reservation.

## Verification

Future red/green adapter fixtures use private task roots, seeded event history
and explicit configuration. Record canonical events and readbacks via Rheos,
without consumer-side parsing or a test-only replacement writer.

This amended incoming story requires fresh planning qualification. Its existing
5-point estimate is provisional; review must assess the enlarged writer/FSM
boundary and recommend a split or re-estimate when necessary. It is not Ready.

## Risks

Creation currently emits parent metadata but not other relationships, and loaded
tasks lose those fields. A read-only or event-only partial port would preserve a
different final state. Source-preserving writes are owned by their existing lane;
consume its qualified revision if implementation needs it.
