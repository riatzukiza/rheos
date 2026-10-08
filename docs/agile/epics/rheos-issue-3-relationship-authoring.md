---
uuid: "rheos-issue-3-relationship-authoring"
title: "Admit UUID relationships through canonical Rheos authoring"
type: "epic"
status: "incoming"
priority: "P1"
points: "16"
labels: "relationships, cli, ledger, planning"
created_at: "2026-10-06"
write-id: "1791479229481-0.qd9md44myxvjj9ft2b"
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

The selected Cephalon consumer also requires dependency-aware lifecycle
admission. The existing Rheos FSM remains the status authority: its canonical
transition path must refuse missing, unfinished or cyclic predecessors before
implementation admission, and admit the corresponding completed-predecessor
case. This requirement was already recorded in the consumer's character epic;
the original authoring plan did not cover the unfinished-predecessor predicate.
This amendment makes that delivery gap explicit for fresh planning review.

## Scope

Review the proposed contract in
[the source-grounded plan](../../notes/uuid-relationship-authoring-plan.md), then
complete these proposed stories in order:

1. `rheos-issue-3-relationship-contract`: portable shapes and graph laws, 3 points.
2. `rheos-issue-3-relationship-writer`: canonical creation/edit/read adapters
   and dependency-aware admission through the existing FSM transition path,
   proposed 8 points, dependent on the contract story.
3. `rheos-issue-3-relationship-parity`: compiled CLI/HTTP/MCP, server-read
   projections, lifecycle admission, history and concurrent-write evidence,
   proposed 5 points, dependent on the writer story.

HTTP acceptance ends at canonical server reads and projections that preserve
accepted references. Browser-client code, a new UI editor and end-to-end browser
tests are outside these stories.

These are provisional estimates for native planning review, not accepted
capacity assignments. The dependency UUIDs on the new Markdown stories are
first-class planning input; their presence does not demonstrate that the
currently unsupported edit operation admitted them.

The explicit fresh proposal is 3/8/5, totaling 16 points. Rheos recorded the
writer's estimate change and this aggregate; the original 13-point estimate
remains in history. The three added estimated points have been reported against
the consumer's fixed milestone as existing prerequisite capacity, without a
new story or behavioral inventory item. Native planning review must assess the
within-card breakdown below and the admission policy before readiness or RED.
An additional story or further capacity change still requires an explicit
report before selection; this proposal supplies no silent expansion.

The writer's eight points comprise three for complete scoped reads and
creation/edit/read integration, three for FSM admission and the shared writer
reservation, and two for boundary/error handling and refusal/no-op evidence.
The parity story's five comprise two for the compiled public-surface matrix,
two for independent-process races and partial effects, and one for consumed
package identity plus native admission readback. These are estimates of the
existing stories, not additional cards or claims of completed implementation.

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
- [ ] The owning FSM/configuration defines predecessor success and the
  implementation-admission boundaries. Native transition calls refuse missing,
  unfinished and cyclic predecessor cases, admit completed predecessors, and
  retain every existing FSM, WIP and executable gate. Relationship authoring,
  a successful pure graph decision or a passing command alone cannot supply
  that admission.
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


---
Fresh planning capacity proposal after qualified PR7 merge54ae39a598fe813ddea7f19b19cb4122a907d060: existing three-story scope becomes3+8+5=16points instead of the prior provisional13, pending current-head native planning review. The portable contract3 is already qualified and Done; writer8 and parity5 remain Incoming. This explicitly reports3additional estimated points for an already selected Cephalon prerequisite; no new story or B1-B3 inventory expansion is selected. The proposal and within-card breakdown address the estimate question left unanswered by the prior no-actionable reviews. Do not infer implementation readiness from PR7's planning merge. Retain supported Markdown input and the sole native Rheos transition/writer/event authority.

---
