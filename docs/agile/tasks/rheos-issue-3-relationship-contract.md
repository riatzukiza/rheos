---
uuid: "rheos-issue-3-relationship-contract"
title: "Specify portable UUID relationship shapes and graph admission"
type: "task"
status: "incoming"
priority: "P1"
points: "3"
labels: "relationships, shapes, graph-laws, planning"
epic: "rheos-issue-3-relationship-authoring"
parent: "rheos-issue-3-relationship-authoring"
created_at: "2026-10-06"
---

# Specify portable UUID relationship shapes and graph admission

## Context

The existing descriptive frontmatter law refuses relationships, creation checks
only an existing parent, and loaded tasks do not retain relationship data. See
[issue 3](https://github.com/open-hax/rheos/issues/3) and the epic's proposed
contract. Syntax, references and graph meaning must be reviewed before adapters.

## Outcome

Portable `.cljc` shapes, normalization and admission laws describe the reviewed
relationship contract on Clojure data. Host loaders supply board facts; pure
decisions depend on neither Node objects nor filesystem/HTTP/MCP effects.

## Scope

Exact UUID membership, optional singular parent/epic, dependency cardinality,
representation compatibility, removals, duplicates, missing/ambiguous targets,
self-reference, directed cycles and hierarchy consistency. Evaluate a complete
proposed update against a complete selected-project snapshot before effects.

## Non-goals

No title/similarity matching, consumer validation, status admission, file writer,
new ledger, cross-project inference or broad runtime migration.

## Acceptance criteria

- [ ] Planning explicitly settles every proposed rule in the linked design,
  including legacy malformed graphs and mixed descriptive/relationship edits.
- [ ] Red fixtures expose missing relationship admission and lost projections;
  they fail assertions about behavior, not merely unresolved dependencies.
- [ ] Pure fixtures cover exact existing UUIDs, a multi-dependency update,
  unset/empty cases, malformed values, duplicate claims, unavailable targets,
  self edges, direct/indirect cycles and contradictory parent/epic membership.
- [ ] Rejected batches have a classified result with no partial admitted subset.
  Deterministic normalization and decisions are insensitive to input map order.
- [ ] The same graph fixtures run in two appropriate supported Clojure hosts;
  compiled browser/Node adapters consume this one contract rather than fork it.

## Verification

Review the fixture matrix before implementation. Future red/green evidence
records each pinned host and exact result. Native incoming-card readback does
not validate graph admission or supply readiness.

## Risks

Combined hierarchy/dependency cycles can be confused with separate edge
semantics. The accepted graph orientation and cycle sets must be explicit.
Missing references from a configured projection must not be guessed globally.
