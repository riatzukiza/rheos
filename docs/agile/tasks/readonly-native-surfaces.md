---
uuid: 63796a65-d27d-430a-a7f6-8f1df8a48744
title: "Preserve composed-read diagnostics across every native surface"
status: incoming
priority: P1
points: 3
labels: planning, readonly, issue5
parent: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
epic: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
dependency: 845caf18-17b5-4179-9a8e-156b0e68b67b
---

## Context

[Upstream Rheos issue #5](https://github.com/open-hax/rheos/issues/5) proves a successful zero-task aggregate after an existing ledger append-open was denied by a readonly mount, while direct snapshot/task reads saw the source cards. Native source ef3c4abf1ea75199486f693e9470df3fec88dd49 and installed 0.1.0 reproduce the failure; neither artifact observation qualifies a released repair. Existing diagnosis is attributed, not repeated or promoted to full-suite proof.

## Outcome

Every canonical surface conveys the reviewed complete/refused contract without silent successful empty results or stripped project evidence.

## Scope

Issue5 outcomes1–4, preserving5. Rheos composition and current query semantics; agent read-board/search tools, CLI compose/read-board/search-tasks, HTTP compose plus events/drift read adapter consumers, programmatic API and MCP results.

## Non-goals

No new command surface or consumer implementation, unrelated UI change, controller, provider, lifecycle rule or default source/filter change.

## Acceptance criteria

1. Choose multi-project semantics before code. Proposed default is strict refusal if any selected required project is failed/incomplete, with all observed project diagnostics retained and no complete board masquerade. If review chooses partial output, its explicit non-complete status and failed/included/excluded identities survive every surface; unknown or unavailable projects never disappear.
2. Keep current status/priority/label/meta/where/across filtering, board enrichment, project selection, projection containment and known drift true/false semantics. Filter-excluded projects are not silently queried; zero selected projects/cards is a distinct lawful complete-empty control. Invalid selector/query decisions are reviewed explicitly.
3. Structured project diagnostics bind canonical project identifier/config/tasks root, stage (resolution/discovery/card/ledger/decode/query), stable error code, completeness and input provenance. Public diagnostics use reviewed path exposure rules; no raw ledger lines, secrets or unbounded logs.
4. Preserve diagnostics through snapshot->summary, search :matches, HTTP generatedAt/totalTasks/query/columns serialization and MCP transport. Throw-only text or dropped extra keys cannot satisfy project-scoped structured evidence. Pin documented success/error shapes, CLI nonzero exits, API result/exception data, MCP isError and HTTP status/JSON behavior before implementation.
5. CLI compose/read-board/search failures do not emit successful complete numeric output, save preset/snapshot side effects or create event input. Explicit user-requested --out/--save exports remain separate reviewed writes to owned destinations only after successful complete admission, not implicit read initialization. Ordinary readonly reads leave input roots byte/mode/path exact.
6. Current direct snapshot/read-task controls still demonstrate readable cards before aggregate refusal; events/drift reader paths use the canonical readonly capability rather than append-open. Retain lifecycle/FSM/gate/write-event behavior, owned async cleanup and whole three-point sizing review.

## Verification

Future CLI child-process exit/JSON checks and actual canonical API/agent dispatch/MCP/HTTP injection cover readonly positive, all-fail, mixed success/failure, source order permutations, filter exclusion and genuine empty controls. Assert entire structured result contracts, exact diagnostic identities/stages and event/drift values, not just task counts. No real backend/provider/services in planning; future fixtures own their roots and transient server resources.

## Risks

Current shallow projections erase future completeness fields; fixing only compose's catch cannot meet the outcome. Error codes/ABI/strict-vs-partial are review decisions, not accepted policy.
