---
uuid: "rheos-issue-3-relationship-parity"
title: "Prove relationship parity, concurrent admission and preserved history"
type: "task"
status: "incoming"
priority: "P1"
points: "5"
labels: "relationships, parity, concurrency, verification"
epic: "rheos-issue-3-relationship-authoring"
parent: "rheos-issue-3-relationship-authoring"
dependency: "rheos-issue-3-relationship-writer"
created_at: "2026-10-06"
---

# Prove relationship parity, concurrent admission and preserved history

## Context

Correct pure graph decisions alone do not prove that public writers share them
or that concurrent requests preserve the accepted graph and immutable ledger.
This follows the canonical writer story and qualifies its complete behavior.

## Outcome

Executable evidence proves equivalent accepted/refused relationships through
actual compiled CLI, HTTP and MCP paths, faithful event replay, and the reviewed
concurrency contract in isolated fixtures. Required hosted gates bind the head.

## Scope

Real create/edit/read round trips, classified errors, removals/no-ops, whole-batch
refusals, protected fields, graph races, append-only history and runtime parity.
The browser client consuming HTTP must preserve accepted references; a new UI
editor is not required unless planning review explicitly adds it to scope.

## Non-goals

No live board, shared watcher, production port/store, direct fixture mutation
presented as native admission, synthetic provider review or custom event parser.

## Acceptance criteria

- [ ] Equivalent request matrices exercise actual compiled CLI and real isolated
  HTTP/MCP handlers. Success, malformed input, missing/ambiguous target, cycles,
  protected-field edits and mixed-batch refusals agree under the accepted law.
- [ ] Seed canonical ledger events; every successful mutation preserves their
  exact byte prefix and appends the reviewed event count/old/new/correlation
  values. Refusals and no-ops preserve the full card and ledger. Canonical replay
  yields the accepted relationships and preserved task identity.
- [ ] Independent processes attempt reciprocal dependencies against one private
  fixture graph. A rejected cyclic final graph cannot be published. Record
  actual scheduling/conflict/refusal results; a mocked sequential test is not
  evidence of inter-process serialization. Parallel fixtures use different roots.
- [ ] Controlled failure between file/event effects is reported according to
  the accepted recovery contract; no success is claimed without its required
  history/readback. Historical events are never rewritten to repair the fixture.
- [ ] Hosted tests/build/lint run on the eventual exact head with pinned source
  dependencies and no signing/deployment credentials. Existing standalone
  qualification owns bootstrap; missing tools remain failures, not passes.

## Verification

Store raw red/green commands, fixture identities, before/after hashes and exact
hosted job IDs. Replay and relationship interpretation use Rheos's canonical
code. Native planning review and lawful readiness precede these implementation
tests; this incoming card supplies neither.

## Risks

In-process locks do not serialize two CLI processes. File-only success can omit
an immutable fact, while reused fixture roots can contaminate another lane.
Explicit inter-process and recovery evidence is required to close those gaps.
