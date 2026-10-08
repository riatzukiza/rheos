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
