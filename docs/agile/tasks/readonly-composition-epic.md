---
uuid: c79a8f4e-6702-4b6a-9b79-7d3e2fa381cb
title: "Make composed board reads truthful and non-mutating"
status: incoming
priority: P1
points: 8
labels: planning, readonly, issue5
type: epic
---

## Context

[Upstream Rheos issue #5](https://github.com/open-hax/rheos/issues/5) proves a successful zero-task aggregate after an existing ledger append-open was denied by a readonly mount, while direct snapshot/task reads saw the source cards. Native source ef3c4abf1ea75199486f693e9470df3fec88dd49 and installed 0.1.0 reproduce the failure; neither artifact observation qualifies a released repair. Existing diagnosis is attributed, not repeated or promoted to full-suite proof.

## Outcome

Complete the five issue outcomes below through Rheos's canonical authority. Proposed epic at 8 points is the sum of stories at 3 + 3 + 2 points; planning must review the whole estimate and dependency/adapter decisions. New incoming Markdown is unadmitted planning input, not a transition or acceptance claim.

## Scope

Canonical read capability and complete-project evidence; composition/filter/error decisions; native CLI/API/MCP/HTTP projections; adversarial read/write/cache qualification. Adapter changes belong to the actual owner. No consumer parser or second ledger/board engine.

## Non-goals

No lifecycle rule change, deployment, credentials, provider work, Clio replacement, foreign PR adoption, package publication, npm-version assertion or Eta Mu #239 distribution closure.

## Acceptance criteria — all original issue outcomes

1. Native read-board/compose/search must support reading already-existing lawful readonly board/event inputs through the canonical owning adapter, or explicitly refuse with nonzero/structured project-scoped diagnostics. No silent successful empty/complete result when any configured project failed.
2. Distinguish genuinely empty boards from failed/inaccessible/malformed/partial sources. Preserve supported project filters and full current task/drift semantics; do not drop event evidence to get a green result. Define multi-project refusal/partial-result contract explicitly before implementation.
3. Reads must not initialize, append, rewrite or reorder ledger/card/config inputs. Define missing-ledger read behavior explicitly; absence is not permission to create a hidden empty ledger or treat known history as absent. Necessary adapter changes belong in its actual upstream owner, not a second Rheos/Foresight parser.
4. Prove nativeCLI/API/MCP read consistency with existing readonly ledger positivefixtures; EROFS/EACCES/missing/corruptledger and mixed-project negativefixtures; genuine empty controls; exactconfig/tasks root controls; snapshot/task control must demonstrate cards were readable before aggregatefailure. Preserve inputbytes/modes and historicalprefixes.
5. Retain lifecyclewrites/FSM/gates/transitionevent contracts unchanged. Current source tests and release/package gates remain distinct; no installedversion/release readiness claim from the isolated sourcebuilt artifact.

## Verification

The design maps every outcome to tests and surface proofs. Retain direct snapshot/task controls, full readonly and mixed-project cases, input bytes/modes/paths, lifecycle regression, full source test/lint/build gates and distinct released-artifact qualification. No implementation runs before planning qualification and lawful readiness.

## Risks

The pinned eta-mu EventAdmission adapter treats missing history as empty and malformed lines as omitted; splitting its constructor is insufficient. Task discovery also swallows errors. Current summary/search/MCP/HTTP projections can discard diagnostics. Missing intent, supplier qualification or complete baseline holds implementation; no automatic Ready inference.
