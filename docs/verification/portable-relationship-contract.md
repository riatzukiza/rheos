# Portable relationship contract checkpoint

This is the pure contract story `rheos-issue-3-relationship-contract`, following
its native planning qualification and Ready admission. It is prerequisite work
for the character loop. It supplies no encounter persistence, consumed mood,
automatic recall or installed dependency-admission proof.

## Source boundary

- `backend/law/relationships.cljc` owns the relationship field set and exact
  nonblank identity vocabulary. Whitespace means the explicit Unicode
  White_Space ranges plus BOM on both hosts. It is not either host's implicit
  blank/trim rule. CSV whitespace is representation syntax; singular strings
  and vector members preserve their exact identities.
- `backend/shape/relationships.cljc` supplies named, closed, composable Malli
  schemas and validators. Raw input permits removal; normalized values contain
  no empty relationships. The registry round-trips as ordinary EDN, without
  functions or opaque compiled regexes in its schema bodies.
- `backend/domain/relationships.cljc` normalizes and classifies complete
  snapshots and proposed updates using those authorities. No Node/JVM I/O,
  loader, writer, clock, status transition or event publication lives there.
- The existing descriptive frontmatter law moved byte-identically to `.cljc`.
  Its public whitelist still refuses relationship fields. The writer story
  must connect the new admission contract before a public relationship update
  can succeed.

## Decisions

`inspect-graph` requires a complete selected-project vector of Clojure maps.
Stored UUID-field strings are exact, opaque identities; duplicate identities
are ambiguous. An absent legacy type means an ordinary task, never an inferred
epic. A declared type must be task or epic, and epic references require an
actual epic target. Parent and dependency edges point from a card to its
referenced card and are independently acyclic. There is no combined-edge cycle
constraint. Explicit epic membership must agree with the nearest epic ancestor
or the membership declared by an intervening parent.

Dependencies accept one string, CSV or a vector of strings. Nil, blank strings
and an empty dependency vector remove a relationship. Malformed or duplicate
members fail; successful dependencies are sorted. Reference guesses, title
matching, case folding and inferred cross-project targets are refused.

`admit-update` checks protected and unknown keys before accepting any mixed
batch. Relationship changes validate the complete proposed graph, including
unchanged rows. A classified refusal contains no task, updates or partial
admitted subset. Malformed legacy relationships remain visible; an explicit
valid repair can proceed. Descriptive-only edits retain their existing closed
authority without reinterpreting inherited relationship data.

A semantic relationship no-op returns the original task and empty changes.
The output includes canonical changes for the later writer. A normalized task
projection is not permission to rewrite unrequested fields. The pure function
cannot establish that the caller supplied every card or held a publication
reservation. The writer must reserve, re-read and revalidate the complete
configured projection before effects, and own file/event failure recovery.

## Executed verification

Commands, versions, exact output, source hashes and failed attempts are retained
in the existing `.ημ/verification/rheos-relationships-contract-20261008/` and
`.ημ/review-evidence/` paths and the private review packet named by the receipts.

| Execution | Actual result |
| --- | --- |
| Initial shared fixture matrix on JVM Clojure and compiled Node | Each 22 tests, 483 assertions, 3 failures, 0 errors: empty-string singular removal disagreed with its input schema. |
| Added portable whitespace regression, before repair | JVM: 23 tests, 497 assertions, 13 failures; compiled Node: 23 tests, 497 assertions, 3 failures; both 0 errors. |
| Corrected shared matrix | Each 23 tests, 497 assertions, 0 failures/errors. JVM Clojure 1.12.4 and compiled ClojureScript 1.12.134/Node 22.20.0, Malli 0.16.4. |
| Full existing `pnpm lint:kondo` | 0 errors/warnings; existing information diagnostics remain visible. |
| Full existing `pnpm test` | 173 tests, 1269 assertions, 0 failures/errors; compiled test target 0 warnings. The script's compile autorun and explicit Node invocation each executed this suite. |
| Full existing `pnpm build` | Server, CLI, GitHub sync and browser release builds: 0 compiler warnings. |
| Actual built CLI projection baseline | Raw task has parent/epic/dependency/type; board projection loses all four. Output assertions: 2 tests, 8 assertions, 4 failures, 0 errors, exit 1. This remains RED. |

The common graph matrix uses an independent bounded-reachability oracle for
all 64 directed dependency graphs and 27 parent graphs on three vertices,
plus hierarchy consistency, ordering, no-op, mixed refusal, malformed legacy
repair and a 1200-card hierarchy. These are finite invariant checks, not a
proof of every graph size or every future adapter.

The initial compiled test invocation failed before tests because its generated
bootstrap joined an absolute output directory to the working directory. The
same compiled bytes ran with the corrected working directory; the original
operator failure is retained. An intermediate compile's external-classpath
deprecation warnings are also retained; the final selected compile avoids
them. The projection assertion harness initially printed four failures but
returned zero because `run-tests` does not return its summary; an end-run
reporter corrected that exit to one. No product behavior changed between those
two output-assertion attempts.

Existing NPM_TOKEN interpolation, deprecated dependency, source-map-support
and intentional invalid-transition fixture notices remain in the logs. They
are separate from compiler and clj-kondo warning counts. The repository has no
lockfile at the selected base; this execution used its existing unlocked
installation and pinned source bootstrap, not a dependency reproducibility
proof. Free space was measured before dependency installation and source
bootstrap, with the 20 GiB floor enforced.

## Remaining delivery

The contract awaits current implementation review. Writer and parity stories
remain Incoming; complete raw projection, creation/edit integration, shared
inter-process reservation, persistence/event failure recovery, CLI/API/MCP
parity, qualified release and native positive/negative dependency admission
are still obligations. Earlier planning approval does not qualify this changed
implementation head. No live board, maker, owner, PM2, cloud or publication
service was changed by these code tests.

Process documentation: GPL-3.0-or-later.
