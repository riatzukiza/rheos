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
write-id: "1791479227871-0.wsa2kye35pae6gowisb"
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
Include actual dependency-aware implementation admission through the existing
FSM path, using the writer story's reviewed policy and complete-read boundary.
HTTP acceptance covers canonical server reads and projections that preserve
accepted references. Browser-client code, a new UI editor and end-to-end browser
tests are outside this story's scope.

## Non-goals

No live board, shared watcher, production port/store, direct fixture mutation
presented as native admission, synthetic provider review or custom event parser.

## Acceptance criteria

- [ ] Equivalent request matrices exercise actual compiled CLI and real isolated
  HTTP/MCP handlers. Success, malformed input, missing/ambiguous target, cycles,
  protected-field edits and mixed-batch refusals agree under the accepted law.
- [ ] Canonical HTTP reads and server projections retain accepted dependency,
  parent and epic UUID references and task identity, matching canonical CLI/MCP
  readback. Acceptance requires this server/read contract, not browser-client
  execution.
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
- [ ] Actual compiled CLI and isolated HTTP/MCP transition calls refuse a
  missing predecessor, an unfinished predecessor and a dependency cycle, and
  admit the corresponding completed-predecessor graph. Include reachable
  transitive predecessors, malformed/ambiguous identities, incomplete reads,
  rejected/archived predecessors and no-dependency cards. Assert preserved
  card/ledger bytes and classified blocker diagnostics on refusal.
- [ ] A completed-predecessor case still fails an invalid FSM edge, saturated
  WIP or nonzero executable gate. A controlled inter-process predecessor
  change during an executable gate cannot commit admission from the old
  snapshot; record the actual reservation/conflict/refusal and final readback.
- [ ] Consumer delivery records the qualified immutable source and actual
  package/bundle identity, then repeats native positive/negative admission
  under that consumed revision. Source tests and a local candidate CLI do not
  prove installed enforcement or authorize character implementation.
- [ ] Hosted tests/build/lint run on the eventual exact head with pinned source
  dependencies and no signing/deployment credentials. Existing standalone
  qualification owns bootstrap; missing tools remain failures, not passes.

## Verification

Store raw red/green commands, fixture identities, before/after hashes and exact
hosted job IDs. Replay and relationship interpretation use Rheos's canonical
code. Native planning review and lawful readiness precede these implementation
tests; this incoming card supplies neither.

The fresh proposal retains this story at 5 points: two for the compiled
CLI/HTTP/MCP matrix using shared fixture data, two for independent-process
races and partial effects with immutable-prefix checks, and one for consumed
package identity plus native admission readback. Existing test and compiled
surface seams are reuse inputs, not evidence that the new cases already pass.
Fresh review must assess this breakdown and the explicit admission matrix.
Required additional work is reported against the fixed consumer milestone
before selection. The writer must actually satisfy its predecessor obligation;
its being Ready is not completion evidence for this successor.

## Risks

In-process locks do not serialize two CLI processes. File-only success can omit
an immutable fact, while reused fixture roots can contaminate another lane.
Explicit inter-process and recovery evidence is required to close those gaps.


---
Fresh planning proposal after qualified PR7 merge54ae39a598fe813ddea7f19b19cb4122a907d060: retain this existing parity story at5points, pending current-head native planning review. Breakdown within this card:2 for compiled CLI/isolated HTTP/MCP accepted/refused/create/edit/read/transition matrices using shared fixture data;2 for real independent-process graph races, predecessor changes during command gates and classified file/event failure with immutable prefix proof;1 for consumed immutable package/bundle identity and repeated native admission readback. Reuse the existing test/compiled-surface seams, but do not call unexecuted cases passing or treat browser tests as scope. This follows the writer; it must not be made Ready merely because its predecessor is itself Ready. Incoming remains truthful; source tests/local candidate alone do not authorize character implementation.

---
