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

## PR9 review and reservation-discovery repair — 2026-10-08

CodeRabbit review5461949384 on head9b753377a6e7edcd858942d9aecc04c44b8cb108
raised traversal race4223364075 and identity fallback4223364089. Native
reply4223418686 explicitly withdrew the latter after inspecting the accepted
stored-UUID law and planning sources. Canonical settlement recorded Handled
on that exact head. This is a finding withdrawal, not independent rejection,
formal approval or full-review credit. Raw review, inline findings and
withdrawal remain retained under `.ημ/review-evidence/pr9-9b-coderabbit-*`.

A real private fixture entry is removed immediately after actual directory
enumeration. Before repair, disappearance of `.rheos-writer-reservation` refuses
an otherwise complete card load:241tests2219assertions2failures0errors, twice.
The native writer now owns the shared metadata directory name; recursive card
discovery excludes that exact project's metadata path before lstat. Lexical and
real task-root spellings are considered. Other directories, discovered cards
and configured projection roots retain visible incomplete-read refusal. The
regression also verifies actual discovered-card disappearance and missing stored
UUID identity; display compatibility remains read-only. No blanket ENOENT
suppression or graph identity repair was added.

After repair,241tests2219assertions0failures0errors ran twice under Node22.20.0.
Lint has0errors0warnings and the same8existing information diagnostics. Test
compile157files/22compiled/0warnings/9.90s; release server115/10/13.48s,
CLI119/11/7.33s, GitHub sync73/3/4.04s and app95/0/6.42s, all0warnings and
actual exit0. Exact raw RED6787bytes/SHA256
92438f1bcdcbdb9c49b4551a3092c0b1795657c99a7c392d90a0017af9ed15e4;
GREEN6033bytes/SHA256
dddb23b171d4e1621bf5ae2432606aa65cffd4dffd351f9ae155e4452335d238.
Lossless log wrappers preserve every original output byte, including failures.
These are local gates; current-head hosted review and merge remain required.
An initial candidate-hash read used the wrong browser path `web/js/main.js`;
the configured output is `dist/web/js/main.js`. This was a read failure after
successful build, corrected by reading the actual build configuration. It
supplies no build or product-failure claim.

Existing receipts are left intact. New correction or execution evidence is an
append in its owning repository; historical attribution is not rewritten. The
owning Receipt River law still requires `repo` on new rows. No compatibility
implementation, observation-only push, runtime or board mutation follows from
this receipt convention. Writer remains InProgress; parity remains Incoming;
B1/B2/B3 remain unimplemented/unverified.

## PR9 relationship boundary corrections — 2026-10-08

Full native CodeRabbit request6068511179 completed via6068513141 at20:37:33UTC.
Review5462560894 assessed all59 inputs on28b2a9bf11e98b86628f3f5cf75ebbb0adbfab72
and raised three findings:4223884239 (blank creation parent),4223884217
(structured HTTP projection values),4223884206 (CLI error classification).
This finding-bearing COMMENTED review is not an approval. The actual current
MiMo approval5462365551 and hosted run37836132228 apply to that old head;
neither approval transfers to the repair.

RED commit22e15295e2ee15a6400d65a32a290c3f860c7ecd contains four behavioral
regressions plus the expanded stable CLI mapping assertion. Final RED ran
245tests/2261assertions/13failures/0errors twice, with zero compiler warnings
and direct bundle exit1. It verifies real owning creation, the public board
handler's browser-readable JSON, the real CLI dispatcher during a held writer
reservation, and an event failure after the actual file effect.

Creation now supplies the already accepted normalized parent to its existence
decision. Empty, whitespace and null parent inputs produce root cards with no
parent frontmatter; their events retain absence as nil. HTTP converts raw
type/parent/epic values at the JS boundary while retaining the malformed values
and relationship diagnostics. CLI conflict now has explicit refusal exit3;
partial-effect explicitly retains error exit4. The earlier exit4 proof and
its report above remain historical evidence, not current behavior.

GREEN245tests/2261assertions/0failures/0errors ran twice under Node22.20.0.
Full lint has zero errors/warnings and the same eight existing information
diagnostics. Test compile157files/11compiled/0warnings/10.74s. Release server
115/5/13.10s, CLI119/6/7.39s, GitHub sync73/0/4.63s and app95/0/6.42s all
completed exit0 with zero compiler warnings. Raw GREEN6310bytes/SHA256
a9a492b8e4a2f53121898de19ddb294c2b9268911f26d619d2b4ee381490bf40
is retained losslessly. Initial test attempts and four corrected interop
warnings are also retained; their harness-induced500 response is not claimed
as an actual native HTTP failure.

The new independent-process proof uses compiled CLI2236725bytes/SHA256
76251ae169560fb43a2239998ac117297c7f4f208874fe06b8d1463d3f692f3c.
One CLI owner holds a real executable gate; four separate CLI contenders
(frontmatter, create, comment, move) each return conflict/exit3 with exact card,
ledger and ownership-token bytes unchanged. Releasing the gate lets the owner
publish successfully. Returned and independently read frontmatter both have
the new status. Existing event bytes remain an exact prefix, the owned
reservation is released, all processes are reaped and the private fixture is
removed. Raw proof10031bytes/SHA256
8896e2cfa84349535c4c19d8ee2f25b99ab674958f7364576a84825b29be00ff
remains a private candidate execution, not installed consumer enforcement or
the successor's complete CLI/HTTP/MCP parity matrix.

Receipt River rejected one new-row input's `+00:00` UTC spelling before any
ledger mutation; the corrected new input uses `Z`. Complete earlier receipt,
reflection and native-event prefixes remain intact. No historical row was
rewritten. Current-head hosted review, settlement and merge remain required;
the independent canonical withdrawal/head-binding gap is tracked in
riatzukiza/.agents issue25. No runtime or board state changed. The accepted
encounter/mood/automatic-recall slice remains unfinished.
