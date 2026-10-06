---
uuid: "rheos-issue-3-relationship-authoring"
title: "Admit UUID relationships through canonical Rheos authoring"
type: "epic"
status: "incoming"
priority: "P1"
points: "13"
labels: "relationships, cli, ledger, planning"
created_at: "2026-10-06"
---

# Admit UUID relationships through canonical Rheos authoring

## Context

[Issue 3](https://github.com/open-hax/rheos/issues/3) records that the existing
`frontmatter --set dependency=<uuid>` surface refuses dependency edits and
creation has no dependency argument. The existing parent creation check does
not establish an editable relationship contract. Consumers currently describe
ordering in Markdown without claiming machine-admitted relationships.

## Outcome

Rheos admits creation and subsequent edits of `dependency`, `parent` and `epic`
relationships by exact UUID through its existing write authority. CLI, HTTP and
MCP share the same decision and event path. Identity, status and provenance
remain protected. A successful relationship edit grants no lifecycle admission.

## Scope

Review the proposed contract in
[the source-grounded plan](../../notes/uuid-relationship-authoring-plan.md), then
complete these proposed stories in order:

1. `rheos-issue-3-relationship-contract`: portable shapes and graph laws, 3 points.
2. `rheos-issue-3-relationship-writer`: canonical creation/edit/read adapters,
   5 points, dependent on the contract story.
3. `rheos-issue-3-relationship-parity`: compiled public-surface, history and
   concurrent-write evidence, 5 points, dependent on the writer story.

These are provisional estimates for native planning review, not accepted
capacity assignments. The dependency UUIDs on the new Markdown stories are
first-class planning input; their presence does not demonstrate that the
currently unsupported edit operation admitted them.

## Non-goals

No consumer-side parser, validator, sidecar, migration, alternate command or
event ledger. No cross-project identity guessing, title-based relationship,
live-board migration, foreign-source edits, lifecycle bypass, global install,
signing/settings change, automatic merge, or retroactive event/receipt rewrite.
The separate direct-blocker plan for issue 4 and CI/release qualification retain
their own scopes.

## Acceptance criteria

- [ ] Native planning review settles relationship cardinality and spellings,
  graph/reference rules, hierarchy consistency, edit/removal/no-op behavior,
  concurrency/refusal semantics, public-surface scope, estimates and breakdown.
  Every finding receives its canonical disposition.
- [ ] The stories reach ready through Rheos before implementation starts.
  Incoming Markdown and local peer review are not readiness evidence.
- [ ] Red laws and adapter fixtures demonstrate the missing current behavior
  for the right reason. Green implements the accepted pure contract first,
  then its canonical adapters. No blanket descriptive-key whitelist expansion
  substitutes for relationship validation.
- [ ] Accepted relationships survive canonical loading, content reads, board
  projections and existing event replay without silently dropping references.
  The rejected cases and historical bytes remain inspectable.
- [ ] Required hosted tests/build/lint qualify the eventual exact implementation
  head and dependency revisions. Final consumer integration uses a qualified
  immutable Rheos revision.

## Verification

This documentation change adds manually authored incoming cards, a supported
Rheos input. It changes no engine source, existing card, configuration or tracked
event. Current verification is source inspection, exact changed-path/prefix
checks and native readback when available. Future executable acceptance belongs
to the stories; no implementation or hosted pass is claimed here.

## Risks

A whitelist-only patch can admit dangling/cyclic links, and the current task
projection discards relationship fields. Validation against a stale board can
let concurrent edits violate a graph that each isolated request accepted.
Writer, event and replay evidence must address these boundaries explicitly.
