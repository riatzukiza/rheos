# UUID relationship authoring: proposed contract and source evidence

This plan addresses [Rheos issue 3](https://github.com/open-hax/rheos/issues/3).
Personal main is `11811264a308d406cb612aefa1dad40818675e5e`; the inspected upstream
main is `ef3c4abf1ea75199486f693e9470df3fec88dd49`. Proposed rules below require
native planning review. They are not assertions of existing engine behavior.

## Observed boundary

`src/rheos/backend/law/frontmatter.cljs` admits eight descriptive keys and
excludes dependency, parent and epic. Both the MCP/CLI path in
`infra/agent_tools.cljs` and HTTP frontmatter handler use that law. Creation in
`domain/task_create.cljs` checks an existing parent and emits it, but has no
dependency/epic request contract. `infra/task_store.cljs` and `shape/kanban.cljs`
do not retain these relationship fields in loaded tasks. A whitelist extension
alone therefore cannot fulfill author/edit/read parity.

`infra/task_edit.cljs` writes a card then emits frontmatter events through the
existing ledger. It does not currently supply graph validation or inter-process
serialization. `domain/events.cljs` and existing event-ledger authority own
event publication, projection and query. This inspection establishes no current
task-replay materializer. Relationship reconstruction and replay evidence remain
future verification through those canonical seams. The plan requires actual
failure/concurrency behavior to be specified before green implementation.

## Proposed rules to settle

| Boundary | Proposal for review |
| --- | --- |
| Identity universe | Exact, unambiguous UUIDs in the selected configured project/task projection. No title matching or guessed cross-project references. Missing or duplicate UUID targets fail visibly. |
| Cardinality | Optional singular `parent` and `epic`; zero or more distinct dependency UUIDs. Omit empty relationships. Keep existing single-UUID dependency input compatible. Review accepted multi-value spellings and explicit removal syntax before coding. |
| Targets | Existing parent task or epic; epic references an actual epic. Dependency targets an existing task or epic. Refuse self-reference and malformed values. |
| Graphs | Define orientation explicitly; parent hierarchy and dependency graphs are acyclic. Review epic membership and conflicting parent/epic ancestry, including whether a combined edge graph is meaningful. Do not accidentally forbid valid composition by assuming all edge kinds have the same meaning. |
| Batch | Validate the complete proposed post-update graph, not keys independently. Reject a mixed batch with invalid/protected data without a partially written subset. Descriptive-only edits retain their existing behavior. |
| Legacy input | Hand-authored Markdown remains first-class. Review whether malformed inherited relationships are reported/readable while mutations fail closed, and whether unrelated descriptive updates can proceed. Never silently remove or repair references. |
| Repetition | Accepted replacement/removal and true no-op behavior have explicit bytes/event contracts. Repeating an identical relationship must not fabricate another successful graph change. |
| Concurrency | Define a canonical revision/serialization boundary shared across actual CLI processes and service writers. Revalidate or visibly refuse stale proposals. Specify file/event failure recovery without rewriting immutable history. |
| Authority | Pure shapes/normalization/graph decisions are `.cljc` Clojure data. Existing creation/edit infra supplies host facts and effects; status stays with the FSM. Public callers cannot bypass the accepted relationship contract through generic frontmatter. |
| Public surfaces | Canonical CLI, HTTP/API and MCP authoring/read parity. HTTP acceptance ends at server reads and projections preserving accepted references. Browser-client code, a new UI editor and end-to-end browser tests are outside this issue's 3/5/5 story scope. |

## Review and implementation checkpoints

The epic proposes 13 points split 3/5/5. Native planning review must explicitly
assess those estimates and settle the table, not merely acknowledge the prose.
Reviewed incoming cards then transition through Rheos to ready. Red laws and
real adapter fixtures precede green; hosted exact-head gates and required review
convergence precede merge. Source-preserving writes, standalone CI/dependencies
and direct blocker reporting retain their existing owner PRs. Consumers use a
qualified immutable Rheos revision after implementation and release admission.

There is no checked-in board configuration or existing Markdown card at this
personal-main base. The new cards follow the existing tracked
`docs/agile/tasks/.events/ledger.edn` task root, with epics under the sibling
`docs/agile/epics` directory as Rheos's conventional creation layout specifies.
An explicit private readback configuration covers `docs/agile` recursively;
it is verification input, not a new repository configuration or live board.
This documents the absence of two neighbours rather than inventing them.

## Verification limits

The initial planning revision `03fefb941fc9218d193dedb6f9e4ee07cd5157e8`
added incoming planning Markdown and owned execution evidence, without changing
engine laws, existing cards, events or workflows. That statement describes the
initial revision only.

The current PR also adds the portable relationship law, shape and domain
contract, shared tests and a pinned-host runner, plus the review workflow and
subsequent native Rheos ledger records. The pure contract's executed tests
verify complete-snapshot decisions, normalization and diagnostic ordering.
Source inspection and native card readback establish what is present and
parseable. Neither those reads nor the portable tests establish writer
integration, public-surface parity, revision reservation, race safety, release
qualification or consumer readiness; those obligations remain with the writer
and parity stories and the qualified delivery boundary.

## Selected consumer admission gap — 2026-10-08

The [accepted Cephalon milestone](https://github.com/riatzukiza/foresight/blob/b3e24c329974a8f9d2452b644647c0a432aa89ec/docs/notes/2026-10-08-cephalon-loop-slice-milestone.md)
retains the dependency-admission prerequisite from the
[character epic](https://github.com/riatzukiza/foresight/blob/b3e24c329974a8f9d2452b644647c0a432aa89ec/docs/agile/kanban/cephalon-character-epic.md):
preserve dependencies in the installed loader,
refuse missing/unfinished/cyclic predecessors natively, and admit completed
predecessors before character implementation. The original authoring table
settled references and cycles while explicitly leaving status with the FSM.
It did not specify an unfinished-predecessor admission predicate. Rheos PR4's
merged portable contract therefore cannot supply this consumer proof by itself.

Inspected personal source `b85854514c613b13a3a9930795f5ae595ab30ac7`, now merged
as `e29551fefe5aac3c69568412724d4810bf64a5a9`, retains that gap:

- `infra/task_store.cljs` drops dependency/parent/epic/type in loaded tasks.
- `domain/transition.cljs` decides an FSM edge and WIP counts without a task's
  dependencies or predecessor status.
- `infra/transition.cljs` loads, gates and writes without revalidation under
  an inter-process graph publication reservation.
- The Promethean `law/fsm.cljs` Ready/Todo admission edges are `:always-allow`.

The amended existing writer/parity stories propose to close those boundaries
inside Rheos. Portable graph decisions remain shared; a separate portable
lifecycle decision consumes the owning FSM/configuration's admission policy.
The proposed selected policy is Done predecessors at Ready/Todo/InProgress
admission, checking all reachable dependency predecessors in a complete scoped
snapshot and retaining the ordinary FSM/WIP/executable gates. Review must settle
the policy, exact diagnostics, reservation/revalidation and failure behavior.
No status setting through generic frontmatter, local Foresight validator or
alternative ledger can stand in for this path.

This is a fresh planning amendment of the already selected prerequisite, not
implementation or admission. Original frontmatter identities, relationships,
estimates and Incoming statuses remain intact. The remaining 5/5 estimates and
13-point aggregate are provisional until the amended scope is reviewed. If
review requires another story or additional selected capacity, report that
requirement against the fixed milestone before expanding its inventory.

Source-preserving Rheos PR1 is now qualified and merged. Its merged tree and
612713-byte base-to-head diff are exact to the reviewed source, SHA256
`ee51824ced67f67f93ae2ae9d091e7f997d8ca870335df86619f26c1043ec93d`.
That repair supplies safe source handling; it does not implement relationship
writers, dependency-aware lifecycle admission, persisted encounters, consumed
mood or automatic maker recall.

## Explicit readiness proposal — 2026-10-08

The preceding paragraph describes PR7's retained frontmatter, not the following
native estimate update. PR7 merged as
`54ae39a598fe813ddea7f19b19cb4122a907d060`; its CodeRabbit and MiMo verdicts
qualified the scope amendment but did not explicitly answer the estimate and
breakdown question. The author now proposes writer 8, parity 5 and aggregate
3+8+5=16. Rheos recorded these values and comments while retaining Incoming;
no hand-edited status or estimate masquerades as native state. The additional
three estimated points were reported against the already selected consumer
prerequisite. No new story or B1/B2/B3 acceptance subset is selected.

### Estimate and breakdown

| Existing story | Proposal | Within-card work |
| --- | --- | --- |
| Portable contract | 3, qualified and Done | Existing pure graph/reference contract and host-parity proof. |
| Canonical writer | 8, Incoming | 3: complete scoped reads and creation/edit/read wiring; 3: lifecycle admission and shared reservation; 2: boundary conversion, classified failures and refusal/no-op evidence. |
| Public-surface parity | 5, Incoming | 2: actual compiled CLI/isolated HTTP/MCP matrix; 2: independent-process races and partial effects; 1: consumed immutable bundle and native admission readback. |

The writer crosses distinct existing mutation paths and needs process-level
coordination after potentially long command gates, so its former five-point
estimate understated integration risk. The parity scope reuses current fixture,
CLI and server seams and excludes browser-client work; five remains the proposal.
These are relative engineering estimates requiring native planning assessment,
not measured execution times or a claim of reviewer agreement. A required
split or extra selected work is reported before selection.

### Proposed lifecycle and publication decisions

1. The owning Promethean FSM names `done` as predecessor success and target
   states `ready`, `todo` and `in_progress` as guarded admission boundaries.
   This is a predecessor predicate, not a Done-to-Ready predecessor transition.
   Other FSMs retain their own explicit policy; callers supply no shadow list.
2. Before each guarded move, use the complete selected-project projection and
   the accepted relationship contract. Refuse missing or ambiguous identities,
   malformed relationships, dependency cycles and an unavailable/incomplete
   read with structured diagnostics naming the affected identity/source/edge.
   On a valid graph, inspect all reachable dependency predecessors; any status
   outside the named success set is an unfinished blocker. Rejected, archived
   and unknown statuses do not imply success. Parent/epic edges retain their
   distinct graph meaning. No dependency does not waive other admission gates.
3. Validate the ordinary FSM edge and WIP decision and run the declared command
   gate. Reacquire or retain the same canonical publication reservation and
   reread/revalidate the complete source revision after the command gate and
   before mutation effects. A command failure or changed predecessor/relationship
   yields a classified refusal or conflict, preserving that attempt's card and
   event bytes. A stale verdict cannot authorize the later write.
4. Creation, relationship editing and guarded transitions share one reservation
   in the owning writer across actual CLI/service processes. Exclusive ownership
   is established by the writer, not a timestamp. A conflicting live owner
   yields a visible conflict; expiry alone does not allow reservation theft.
   Release belongs to the owning attempt's finalization. An unresolved owner
   remains refused until verified termination or an explicit native recovery
   operation establishes authority. Do not fabricate effect cancellation.
5. Public handlers propagate the shared structured refusal/conflict and actual
   effect outcome. A file change followed by failed event append is a visible
   partial-effect failure, never successful admission. Record its attempt and
   actual changed/readback evidence through the existing event/receipt seams;
   retain historical bytes and require native repair instead of rewriting facts
   or automatically replaying an uncertain mutation. A true no-op performs no
   file/event effect. Atomic file-plus-ledger commit is not claimed.
6. Reservation proof covers participating canonical writers. First-class manual
   Markdown remains input; a nonparticipating external editor is not magically
   serialized by a CLI lock. Fixture proof must state its actual writers and
   source-revision observations, and must not generalize cooperative serialization
   into an unverified universal filesystem guarantee. Complete-read failures
   still refuse rather than silently excluding a candidate card.
7. Keep the writer Incoming until this proposal is natively reviewed and it
   lawfully reaches Ready. Its portable-contract predecessor is actually Done.
   Keep parity Incoming until the writer actually satisfies its predecessor
   obligation; simultaneous Ready transitions would not prove that completion.
   The consumer remains blocked until qualified delivered Rheos code proves the
   installed missing/unfinished/cyclic/completed admission matrix.

These decisions require fresh planning review. They add no engine source,
test implementation, installed package or native predecessor-admission proof.
RED must encode their relevant positive/negative laws before domain/adapters
GREEN, including command-gate failures and actual process races. No observation
or successful planning merge completes the accepted character-loop milestone.
