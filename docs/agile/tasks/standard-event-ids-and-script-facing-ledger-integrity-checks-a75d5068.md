---
uuid: "af67891c-fbb4-4f40-a6a2-e9bda75d5068"
title: "Standard event IDs and script-facing ledger integrity checks"
status: "review"
type: "task"
priority: "P1"
points: "3"
labels: "cephalon, events, identity, provenance, repair"
category: "tasks"
write-id: "1791835694493-0.xyw3qcthjfgnur8o4u"
created_at: "2026-10-12T15:09:38.874Z"
---

## Context

The human authorized the historical row repair on 2026-10-12: "You can fix the ledger rows. We should probably have a check for that on the scripts, and a standard way of creating and checking the ids". Muse PR6 at immutable cd5c4d5372aede740feeed74554f635a08559b09 had two distinct title/labels events at ledger rows76/77 with one event/id; the separately authorized correction is now committed at8acedb549a93c7a97634f9f40a4844a34e3285e3 with original-row custody. Rheos PR16 already replaced timestamp-only generation with UUID generation, but there is no reusable construction/check contract or script-facing ledger identity check.

## Outcome

One portable Rheos owner module supplies distinct generated-ID and retained-ledger-ID contracts, used by the producer and available to scripts/CI. This owner card can complete after its own source qualification and publication; the dependent consumer rollout has separate acceptance and review gates.

## Scope

Extract the existing board/task/UUID composition into a deterministic portable constructor accepting explicit board ID, task ID and UUID entropy. Board/task components preserve the exact nonblank identity strings already allowed by configured board IDs and stored task references, including legacy opaque punctuation, embedded whitespace and Unicode; do not apply new-card filename-safe or RFC UUID restrictions to those components or trim/normalize them. Reject blank or non-string components. Keep entropy generation at the runtime edge. Name separate generated-ID validation (the producer's prefix plus canonical UUID suffix) and ledger-admission validation (compatible nonblank opaque IDs, with uniqueness checked over all physical records). Add a read-only canonical CLI check that reads every physical ledger line, refuses incomplete or extra forms and invalid/non-map records, and reports duplicate IDs with exact one-based row locations. Dispatch this explicit-path check before general CLI board/config/view bootstrap, so unrelated configuration cannot decide its ledger-specific result. Event IDs identify records; write IDs remain correlation values shared by a multi-field operation.

Export the checker through the owning CLI/script boundary and document invocation and revision binding. The separate two-point consumer card `adopt-qualified-event-id-checks-in-the-muse-consumer` depends on owner UUID `af67891c-fbb4-4f40-a6a2-e9bda75d5068` on this same coordinating board; its implementation and PR belong in personal Muse. This three-point owner plus the two-point rollout retain the original five-point total provisionally. The dependent consumer card covers the already authorized Muse correction, its original-row archive and consumer CI pin/adoption after this owner implementation is qualified. Owner release and consumer adoption do not transfer approval or completion to each other; no Muse/Foresight validator is introduced.

## Non-goals

No runtime restart, deployment, live publication, unrelated board transitions, deduplication/deletion, broad historical migration, causal-root/write-ID changes, dependency upgrade, alternate identity authority, general-purpose ledger repair protocol or extra review-policy waiver.

## Acceptance criteria

1. Identical explicit constructor inputs return the identical generated ID; changing valid UUID entropy changes the ID. Blank/non-string board/task components and invalid UUID entropy refuse; nonblank opaque board/task strings retain exactly the existing emission identity domain, including `literal, id`, punctuation, internal whitespace and Unicode. Generated-ID validation checks the prefix against explicit expected board/task components and canonical UUID entropy, without an ambiguous hyphen split or new-card filename restriction. Constructor and actual emission adapter controls include those legacy opaque board/task inputs and unchanged exact components. The production adapter obtains fresh entropy for each emission before calling the constructor; controlled adapter tests observe that boundary. Frozen time and two independent real writers retain distinct event IDs, unchanged non-ID fields and persistence-before-publication.
2. Ledger admission independently accepts unique compatible opaque strings, including timestamp-based legacy IDs, without applying generated-ID UUID restrictions. It refuses identical and different-payload duplicates, missing/null/boolean/blank/non-string IDs and non-map records with exact physical row numbers. An ID check does not claim full event-schema admission.
3. Each physical line must contain exactly one complete EDN form. Two forms on one line, a form spanning lines, trailing forms/junk and blank or comment-only physical lines refuse. A single terminal LF or CRLF is a record terminator rather than another blank row; extra blank lines refuse. A zero-byte file refuses rather than passing as an empty scan. No malformed line is silently skipped or partial scan treated as success.
4. CLI outcomes bind the existing exit contract: unique nonempty input exits0, missing path exits2, malformed/invalid/duplicate/empty input exits3, unreadable input or other I/O failure exits4, and missing command arguments exit1. Every failure exits nonzero. The explicit-path check bypasses board configuration discovery/loading/resolution and view-store bootstrap before dispatch. Real process controls invoke it from a directory with no board config and with an unrelated malformed or unreadable `KANBAN_CONFIG`; those unrelated inputs do not alter its ledger-specific outcome. Preserve fixture bytes and prove no board/config/card/ledger creation or write on any check path, including missing/unreadable input.
5. The public owner invocation accepts an explicit active-ledger path, has stable documented result fields and uses the same portable ID rules as production. Owner script/CLI tests include a duplicate negative and unique compatible positive. Full source gates and exact-head review qualify the owner revision before the separate consumer pins/adopts it; the owner can complete independently.

## Verification

Planning review and native Ready precede source implementation. Commit meaningful failing law/CLI tests first, then green portable semantics and outer adapters. Run full existing lint/test/build gates with zero compiler/lint warnings. Exercise constructor determinism and adapter entropy, strict per-line EOF/blank/empty controls, actual process exits and unchanged fixture bytes. Record exact input/output/source hashes and actual review IDs. Consumer ledger validation and adoption remain separately gated; no owner test/source plan supplies live character proof.

## Risks

A syntax reader can accept only a prefix unless whole-row consumption is checked. A uniqueness-only check must not claim complete event-schema validity. Random UUIDs are collision-resistant, so the check remains necessary. Changing original identifiers may affect an external ID-based reference; the correction manifest must retain original identity and exact row binding. The two original events shared a write ID legitimately; changing it would destroy correlation. Existing malformed history outside the authorized pair must be reported separately rather than silently rewritten.

Process documentation: GPL-3.0-or-later.


---
Current owner-body revision snapshot; original task-created body remains immutable history. At source 0fa2c8fa6b81ec04280f0311ab8a035865d72f69, owner af67891c-fbb4-4f40-a6a2-e9bda75d5068 is Incoming/3points and consumer adopt-qualified-event-id-checks-in-the-muse-consumer is Incoming/2points with this owner dependency. The initial combined five-point body is superseded as current scope by the separately reviewed Markdown revision below. This canonical comment captures that complete current body in the append-only native history. It supplies no new replay engine, automatic body projection, planning approval, Ready transition or implementation. Snapshot UTF8 bytes=7304; SHA256=946f94dc53b495cfe59264f1a3067155ab67cea1390c5ad30ab76a6752fde836.

BEGIN CURRENT OWNER BODY
## Context

The human authorized the historical row repair on 2026-10-12: "You can fix the ledger rows. We should probably have a check for that on the scripts, and a standard way of creating and checking the ids". Muse PR6 at immutable cd5c4d5372aede740feeed74554f635a08559b09 had two distinct title/labels events at ledger rows76/77 with one event/id; the separately authorized correction is now committed at8acedb549a93c7a97634f9f40a4844a34e3285e3 with original-row custody. Rheos PR16 already replaced timestamp-only generation with UUID generation, but there is no reusable construction/check contract or script-facing ledger identity check.

## Outcome

One portable Rheos owner module supplies distinct generated-ID and retained-ledger-ID contracts, used by the producer and available to scripts/CI. This owner card can complete after its own source qualification and publication; the dependent consumer rollout has separate acceptance and review gates.

## Scope

Extract the existing board/task/UUID composition into a deterministic portable constructor accepting explicit board ID, task ID and UUID entropy. Board/task components preserve the exact nonblank identity strings already allowed by configured board IDs and stored task references, including legacy opaque punctuation, embedded whitespace and Unicode; do not apply new-card filename-safe or RFC UUID restrictions to those components or trim/normalize them. Reject blank or non-string components. Keep entropy generation at the runtime edge. Name separate generated-ID validation (the producer's prefix plus canonical UUID suffix) and ledger-admission validation (compatible nonblank opaque IDs, with uniqueness checked over all physical records). Add a read-only canonical CLI check that reads every physical ledger line, refuses incomplete or extra forms and invalid/non-map records, and reports duplicate IDs with exact one-based row locations. Dispatch this explicit-path check before general CLI board/config/view bootstrap, so unrelated configuration cannot decide its ledger-specific result. Event IDs identify records; write IDs remain correlation values shared by a multi-field operation.

Export the checker through the owning CLI/script boundary and document invocation and revision binding. The separate two-point consumer card `adopt-qualified-event-id-checks-in-the-muse-consumer` depends on owner UUID `af67891c-fbb4-4f40-a6a2-e9bda75d5068` on this same coordinating board; its implementation and PR belong in personal Muse. This three-point owner plus the two-point rollout retain the original five-point total provisionally. The dependent consumer card covers the already authorized Muse correction, its original-row archive and consumer CI pin/adoption after this owner implementation is qualified. Owner release and consumer adoption do not transfer approval or completion to each other; no Muse/Foresight validator is introduced.

## Non-goals

No runtime restart, deployment, live publication, unrelated board transitions, deduplication/deletion, broad historical migration, causal-root/write-ID changes, dependency upgrade, alternate identity authority, general-purpose ledger repair protocol or extra review-policy waiver.

## Acceptance criteria

1. Identical explicit constructor inputs return the identical generated ID; changing valid UUID entropy changes the ID. Blank/non-string board/task components and invalid UUID entropy refuse; nonblank opaque board/task strings retain exactly the existing emission identity domain, including `literal, id`, punctuation, internal whitespace and Unicode. Generated-ID validation checks the prefix against explicit expected board/task components and canonical UUID entropy, without an ambiguous hyphen split or new-card filename restriction. Constructor and actual emission adapter controls include those legacy opaque board/task inputs and unchanged exact components. The production adapter obtains fresh entropy for each emission before calling the constructor; controlled adapter tests observe that boundary. Frozen time and two independent real writers retain distinct event IDs, unchanged non-ID fields and persistence-before-publication.
2. Ledger admission independently accepts unique compatible opaque strings, including timestamp-based legacy IDs, without applying generated-ID UUID restrictions. It refuses identical and different-payload duplicates, missing/null/boolean/blank/non-string IDs and non-map records with exact physical row numbers. An ID check does not claim full event-schema admission.
3. Each physical line must contain exactly one complete EDN form. Two forms on one line, a form spanning lines, trailing forms/junk and blank or comment-only physical lines refuse. A single terminal LF or CRLF is a record terminator rather than another blank row; extra blank lines refuse. A zero-byte file refuses rather than passing as an empty scan. No malformed line is silently skipped or partial scan treated as success.
4. CLI outcomes bind the existing exit contract: unique nonempty input exits0, missing path exits2, malformed/invalid/duplicate/empty input exits3, unreadable input or other I/O failure exits4, and missing command arguments exit1. Every failure exits nonzero. The explicit-path check bypasses board configuration discovery/loading/resolution and view-store bootstrap before dispatch. Real process controls invoke it from a directory with no board config and with an unrelated malformed or unreadable `KANBAN_CONFIG`; those unrelated inputs do not alter its ledger-specific outcome. Preserve fixture bytes and prove no board/config/card/ledger creation or write on any check path, including missing/unreadable input.
5. The public owner invocation accepts an explicit active-ledger path, has stable documented result fields and uses the same portable ID rules as production. Owner script/CLI tests include a duplicate negative and unique compatible positive. Full source gates and exact-head review qualify the owner revision before the separate consumer pins/adopts it; the owner can complete independently.

## Verification

Planning review and native Ready precede source implementation. Commit meaningful failing law/CLI tests first, then green portable semantics and outer adapters. Run full existing lint/test/build gates with zero compiler/lint warnings. Exercise constructor determinism and adapter entropy, strict per-line EOF/blank/empty controls, actual process exits and unchanged fixture bytes. Record exact input/output/source hashes and actual review IDs. Consumer ledger validation and adoption remain separately gated; no owner test/source plan supplies live character proof.

## Risks

A syntax reader can accept only a prefix unless whole-row consumption is checked. A uniqueness-only check must not claim complete event-schema validity. Random UUIDs are collision-resistant, so the check remains necessary. Changing original identifiers may affect an external ID-based reference; the correction manifest must retain original identity and exact row binding. The two original events shared a write ID legitimately; changing it would destroy correlation. Existing malformed history outside the authorized pair must be reported separately rather than silently rewritten.

Process documentation: GPL-3.0-or-later.
END CURRENT OWNER BODY

Planning is qualified and merged in personal Rheos PR18 at 14913881d7ae025b982d5cf583bfeefdcbec9f64, preserving exact reviewed head 220acdc3ce8c4abf8df74a0da6a85fc09dfcb944. The independently reviewed three-point owner will now proceed through native readiness and RED before portable identity and explicit-path read-only checker implementation. Consumer adoption remains dependent and unimplemented. No live cephalon, model, inference credential or runtime change.

Owner RED now exercised portable generated/opaque ID laws, explicit constructor/checker stubs, malformed physical-row/duplicate controls, real no-config CLI exits, production constructor-call boundary and two independent frozen-time ledger writers. Actual pnpm test exits1:268tests2710assertions58failures1error. The constructor error is the explicit unimplemented function, row/identity failures are unimplemented checker outputs, and real CLI failures are unknown check-event-ledger verb. Existing producer/storage/publish tests remain. Raw owned log45592bytes/SHA2562d4f18695ecd69d26a11153240f12b8cc648a611e1e35de93a6f0971ad701d76. No successful checker/source adoption is claimed. GREEN will implement portable decisions and read-only boundary before full gates.

GREEN source prepared: portable constructor and strict physical-row identity checker; producer uses fresh UUID entropy through the constructor. Actual compiled explicit-path CLI bypasses config/bootstrap and preserves fixtures; duplicates, split/extra/blank rows, empty input, malformed UTF-8 and missing/unreadable files refuse with documented exits. Fresh absent-dist control rebuilt CLI then passed 268 tests / 2851 assertions; JVM portable rules 3 tests / 111 assertions pass. Qualified predecessor CLI85369213 native InProgress -> Review actually ran pnpm build/lint/test, all four targets zero compiler warnings, lint zero errors/warnings, 268/2851 zero failures/errors. Host missing-NPM_TOKEN notices and external-path JVM deprecation notices remain separate. Complete raw logs retained privately; no source qualification/consumer adoption/deployment credit yet.

---