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

## Verification

Future red/green adapter fixtures use private task roots, seeded event history
and explicit configuration. Record canonical events and readbacks via Rheos,
without consumer-side parsing or a test-only replacement writer.

## Risks

Creation currently emits parent metadata but not other relationships, and loaded
tasks lose those fields. A read-only or event-only partial port would preserve a
different final state. Source-preserving writes are owned by their existing lane;
consume its qualified revision if implementation needs it.
