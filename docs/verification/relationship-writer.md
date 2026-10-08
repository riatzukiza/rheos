# Canonical relationship writer checkpoint

## Selected scope and authority

This implements the existing eight-point
`rheos-issue-3-relationship-writer` story in Rheos. Personal planning PR8 reviewed
`fe7c3075f281fb65eb0fe7197b6c5ea3dbddc08e` and merged as
`660a6f880769edcf9e52cc5e21e477a5545133f8`. Native Rheos operations then advanced
this story through Ready to InProgress; the contract predecessor was read as
Done. The five-point parity story and larger epic remain Incoming. Planning
approval does not qualify this implementation or authorize consumer delivery.

## Implementation

- The owning Promethean FSM names Done as successful predecessor status and
  Ready, Todo and InProgress as guarded target states. Named Malli policy data,
  state-closure laws and a separate pure `.cljc` decision consume that policy.
  All reachable dependency predecessors must have successful status. Parent
  and epic membership retain their separate relationship semantics.
- The canonical loader retains raw frontmatter, type and accepted references,
  visible malformed relationship diagnostics and source SHA256. Selected roots
  and source reads fail visibly when unavailable; no successful partial graph
  is supplied. Guarded admission uses stored UUIDs, not display-name fallbacks.
- Creation and mixed frontmatter edits use the existing portable graph contract
  against the complete selected projection. Protected identity, status and
  provenance edits remain refused. Replacement and removal use accepted
  normalization; empty fields are omitted. Semantic relationship no-ops have
  no source or event effect. Descriptive-only edits preserve their existing
  treatment of malformed inherited relationships.
- Creation events retain accepted references. Edit events retain accurate
  old/new values and one batch correlation ID through the existing ledger.
  Reconstruction fixtures consume canonical queried events; this does not add
  a new runtime task-replay materializer.
- Creation, edits, comments and status moves reserve the real project root
  using one exclusive filesystem directory and an exact ownership token. An
  unknown owner is a conflict. No elapsed timeout, PID guess or automatic retry
  transfers ownership. Participating CLI and service writers use this boundary;
  manual editors and other nonparticipating writers are outside serialization.
- Moves retain the original FSM edge, WIP and executable gates, then reread the
  complete graph and source revisions before effects. Creation's explicit
  `force-status` retains its initial-state override and still checks configured
  predecessor admission; it is not a new lifecycle transition command.
- File plus event publication is explicitly non-atomic. Failure reports phase,
  correlation ID and actual file readback. Release failure cannot hide the
  earlier operation's outcome. Changed ownership is left intact for explicit
  repair; there is no historical event rewrite, rollback or success assertion.
- CLI flags/help, HTTP projections/errors and tool/MCP schemas use the owning
  writer. CLI `--json` errors retain classification with a nonzero exit; new
  conflict/partial kinds retain the existing internal exit-code fallback of4.
  Public schemas describe accepted relationship cardinalities and removals.
  Source changes alone do not establish full HTTP/MCP execution parity.

## Executed evidence

Raw command logs are retained as lossless UTF-8 JSON strings in
`.ημ/review-evidence/*.log.json`; each records the original byte count and
SHA256. They reconstruct the original output, including trailing whitespace.
No raw failure was edited to pass diff hygiene.

| Checkpoint | Actual execution |
| --- | --- |
| Initial guarded-writer RED | 227 tests, 2130 assertions, 73 failures, zero errors, twice. |
| Reservation/revalidation RED | 231 tests, 2147 assertions, 76 failures, zero errors, twice. |
| Initial transition GREEN | 231 tests, 2147 assertions, zero failures/errors, twice. |
| Creation/edit RED | 233 tests, 2180 assertions, 26 failures, zero errors, twice. |
| Creation/edit GREEN | Initial five-failure attempt retained; corrected233/2180 passed twice without weakening existing source diagnostics or conflict tests. |
| Actual tool boundary RED → GREEN | 234 tests, 2186 assertions; five failures before wiring, zero failures/errors after wiring, each twice. |
| Comment/cleanup RED → GREEN | 238 tests, 2206 assertions;12 failures before repair, zero failures/errors afterward, each twice. Controlled event failure already retained its actual source readback. |
| Returned retained-source RED | 238 tests, 2207 assertions, one failure, zero errors, twice: accepted move returned the old frontmatter status. |
| Final returned-source GREEN | 238 tests, 2207 assertions, zero failures/errors, twice; full lint0/0 plus eight existing infos; server, CLI, GitHub sync and browser release builds each zero compiler warnings. |

Two operator compile attempts contained extra closing delimiters and supply no
behavioral RED/GREEN credit. The first failed `pnpm test` attempted an implicit
package-manager refresh and aborted before tests; it supplies no test credit.
Subsequent runs use the existing installed compiler and direct Node bundle,
without dependency installation. One retained corrected publication RED ran
compiler autorun under Node24.14.1 and the separate bundle under Node22.20.0;
final gates put Node22.20.0 first on PATH for both executions. Compiler success
alone is never reported as test success: a failing direct bundle exits1.

Full repository lint has zero errors/warnings and eight existing information
messages. Expected invalid-transition/compose fixtures and the missing optional
source-map-support notice remain visible. Existing unlocked dependencies and
pinned source bootstrap are reused; this is not dependency reproducibility.

## Actual independent CLI processes

`writer-native-cli-reservation-20261008.json` preserves actual commands and
stdout/stderr, fixture inputs, gate script, candidate identity and before/after
hashes. The CLI candidate is2235130bytes/SHA256
`9dc10d590fd9d2077edae3a0323da3450baf5cac722ae2e8e7bbb3b2b3a31979`.

A native comment seeds history. One compiled CLI move holds its executable gate.
Four independently spawned CLI contenders attempt a relationship edit,
creation, comment and another move. Each returns `conflict`, exit4, leaving the
complete card and existing ledger bytes identical and the ownership token
unchanged. Releasing the fixture gate lets the owner publish one successful
move. Its ledger retains the complete prior byte prefix; a separate native
read retains the dependency and new status. The exact owned reservation is
released, every process is reaped, and the private fixture is removed.

That proof exposed stale frontmatter in the returned move result while the
independent read was correct. The original proof remains unchanged. The later
RED fixture and writeback correction refresh returned frontmatter and source
revision from the accepted bytes; rebuilding changes the candidate identity.

## Remaining qualification and limits

Current-head code review, hosted deterministic gates and merge remain required.
The writer stays InProgress until its actual delivery criteria qualify. The
successor parity story owns the complete compiled CLI/isolated HTTP/MCP matrix,
reciprocal dependency races, consumed immutable package identity and native
positive/negative consumer admission. This one-gate CLI check supplies neither
that whole matrix nor installed enforcement.

No live board, shared watcher, maker, owner, PM2, cloud, model, credentials or
publication service changed. Free space was17133641728bytes, below the accepted
local20GiB floor; no full checkout or dependency installation was allocated.
Cephalon encounter persistence, consumed mood and automatic graph recall
remain unimplemented/unverified by this prerequisite. Original receipts and
reflections remain append-only; corrections are new rows, never rewritten
historical claims. No spore was created or promoted.

Process documentation: GPL-3.0-or-later.
