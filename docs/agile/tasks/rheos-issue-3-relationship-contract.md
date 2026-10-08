---
labels: "relationships, shapes, graph-laws, planning"
parent: "rheos-issue-3-relationship-authoring"
type: "task"
write-id: "1791449847854-0.hq94ugnvtuckmviznfb"
points: "3"
title: "Specify portable UUID relationship shapes and graph admission"
priority: "P1"
status: "review"
epic: "rheos-issue-3-relationship-authoring"
uuid: "rheos-issue-3-relationship-contract"
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

---
Planning qualification: canonical pr-flow gate PASS at 2026-10-08T08:07:24Z on e8105ee22f3c71be7871fa77173d23554be69d8b/base11811264a308d406cb612aefa1dad40818675e5e. Native MiMo APPROVED5453342097 and CodeRabbit completed request6055459294/reply6055461197/current summary6022099803; all53 selected inputs, no declared omissions,1 completed available-agent planning round,3 passing deterministic checks,2 settled findings. Starting only this3-point pure contract story. Writer and parity prerequisites stay Incoming. Implementation choices within the reviewed scope: identity means exact existing nonblank UUID-field string including supported legacy opaque IDs, not a new hexadecimal-only restriction; parent/epic are optional singular references; dependency admits a single string, comma-separated strings or a vector of strings, with nil/blank/empty vector as explicit removal and malformed/duplicate members refused. Parent hierarchy and dependency graphs are independently acyclic; epic membership agrees with the nearest epic ancestor. Relationship mutations validate the whole proposed selected-project graph, with classified atomic refusal; descriptive-only legacy edits keep their existing authority. Deterministic normalized dependencies are sorted. Reading malformed legacy data reports it without silent repair. No-op relationship replacement/removal changes neither card bytes nor successful mutation history. Actual portable RED fixtures precede implementation and subsequent code review; these choices are author decisions, not a claim that either reviewer explicitly enumerated every spelling. This is prerequisite implementation, not B1/B2/B3 delivery or deployment.

--text

Correction to the preceding literal --text comment: installed native help names add-comment UUID --text TEXT; comment is an older positional surface and the initial invocation preserved only its flag. Preserve that event unchanged. Portable contract checkpoint: JVM Clojure1.12.4 and compiled ClojureScript1.12.134/Node22.20/Malli0.16.4 each23tests497assertions0failures0errors; existing full lint0errors0warnings,173tests1269assertions0failures0errors,4releasebuilds0compilerwarnings. Input-removal mismatch RED146a43d and host-whitespace RED19bf20b are retained before the shared lexical law repair. New actual built public CLI raw task retains parent/epic/dependency/type but read-board drops all four:2tests8assertions4failures0errors/exit1 after correcting the NBB exit reporter; initial printed-failure/exit0 proof attempt is retained. Full documentation and command/source-hash evidence are in docs/verification/portable-relationship-contract.md and the existing .ημ paths. Native InProgress→Review additionally executed the configured full test gate successfully. No current implementation approval, projection repair, writer/parity readiness, release, persistent encounter, consumed mood, automatic recall, B1/B2/B3 completion or runtime mutation is claimed.
---