# Restore standalone Rheos CI

This is a proposed implementation plan, awaiting planning review. The
authoritative story is Foresight card
`5eb2ad18-53d7-4412-818d-dfd6a9b42258` (5 points), dependent on source-preservation
card `819717c4-735b-4dd5-b5b7-ba860d3942f9`. Card state and transitions remain
owned by Rheos; this document does not maintain another board state.

The branch starts from source-preservation commit
`dda73f1003f6fc0ba804544d4c2a556eafe246bf`. Its first PR targets
`codex/preserve-markdown-source`, with auto-merge off and an explicit CodeRabbit
planning review because the base is not the default branch.

Read the canonical `~/.agents/skills/pr-flow/SKILL.md` before every PR
interaction. That pack's exit conditions and settlement rules govern both
planning and code review, including the readiness transition below.
The operator must supply the current canonical policy in review requests and
reviewer context. Unavailable policy or remote governance references remain
verification limits; they cannot establish review-loop clearance.

## Problem and outcome

The standalone extraction retained the package commands but omitted the
[donor's Rheos workflow](https://github.com/open-hax/eta-mu/blob/0ed56aa74a53a1d1e9c2e55ce95451817a7f3a90/.github/workflows/rheos.yml).
A full test pass therefore does not establish clean package installation or
successful release compilation. Restore an independently executable CI check
that installs this repository, bootstraps its declared source dependencies, and
runs its existing test, lint, and release commands.

The current stacked base includes `.github/workflows/eta-mu-review.yml` for
native PR review. Its caller repair requires complete-manifest unlocked install,
pinned bootstrap, tests, lint and all four release gates; hosted results must
qualify those commands at the exact head. That App-backed, unlocked review path
does not implement this story's credential-free frozen-install workflow or its
pull-request, main-push and manual lifecycle.

The prior source-preservation qualification passed 166 tests and 858 assertions
using an external prefix containing backend dependencies and shadow-cljs. That
result does not qualify the complete dependency manifest or browser release.

## Scope and dependency policy

- Add a standalone GitHub Actions workflow on pull requests, pushes to `main`,
  and manual dispatch. Remove donor monorepo path filters so standalone source,
  configuration, dependency, and workflow changes receive the same check.
- Use read-only repository permissions, no deployment or review credentials,
  pinned action commit SHAs, an explicit runner version, and a bounded job
  timeout. Install Node `24.14.1`, pnpm `10.15.0`, Java 21 (Temurin), and
  clj-kondo `2025.10.23`. Report their actual versions in job output.
- Establish a standalone `pnpm-lock.yaml` through a successful clean install of
  the actual `package.json`, review the resulting dependency resolution, and
  commit that lockfile. CI then uses `pnpm install --frozen-lockfile`.
  There is currently no tracked lockfile to support the donor's frozen install.
- Preserve declared dependency and package-manager choices. Do not substitute
  an external dependency prefix, ambient workspace package, sibling checkout,
  or reduced dependency manifest to make the CI check pass.
- Run the existing `bash scripts/bootstrap-source-deps.sh` in a fresh checkout.
  It pins protocols to eta-mu commit
  `0ed56aa74a53a1d1e9c2e55ce95451817a7f3a90` and chat-ui to
  `86385532b4f8606946555d0ada8e3fb22f35b4c3`, supplying the source paths already
  declared in `shadow-cljs.edn`. During implementation, require both source
  trees to exist and contain at least one file before reporting `Source deps
  ready`; exit with an error if either tree is missing or empty, including
  after a suppressed copy failure.
- Run `pnpm test`, `pnpm lint:kondo`, and `pnpm build` as separate visible
  steps. Preserve compiler diagnostics on failure, and retain installation and
  bootstrap failures in the normal job log. Correct the README's stale
  monorepo commands and release-target description.

## Acceptance criteria

1. A fresh checkout installs the complete declared dependency manifest without
   `NODE_PATH` or an external tool/dependency prefix, and a subsequent frozen
   install succeeds against the committed lockfile.
2. The existing source bootstrap supplies its pinned protocols and chat-ui
   inputs before compilation. Verify that both `deps/protocols/src` and
   `deps/chat-ui/src` exist and are nonempty before reporting bootstrap success;
   missing or empty trees fail with a nonzero exit. A missing dependency or
   tool fails the job.
3. The actual package test command completes with zero failures or errors, and
   lint succeeds. Compiler warnings remain visible and are assessed before
   claiming release qualification.
4. `pnpm build` releases all four current targets: `server`, `cli`,
   `github-sync`, and browser `app`. Its expected outputs include
   `dist/server.js`, `dist/cli.cjs`, `dist/github-sync.cjs`, and
   `dist/web/js/main.js`.
5. `node dist/cli.cjs --help` exits successfully and renders the existing CLI's
   help. This checks loading the built entry point without mutating a board.
6. The workflow runs the same install/bootstrap/test/lint/build sequence on the
   PR's current head and records a successful check. P0/P1 findings require a
   `Fixed` settlement; disputed P0/P1 findings remain open for user adjudication.
   P2/P3 findings may be fixed, handled, deferred to a named card, or rejected
   with evidence when permitted by the canonical pr-flow settlement policy.
   The final current-head full review must complete, and every finding it raises
   must receive an explicit verified disposition before the PR is described as
   reviewed or reaches the merge gate. A new push requires fresh qualification
   and review evidence for the resulting head.
7. Receipt River qualification records retain the exact revisions, source pins,
   tool and dependency versions, commands and exit results, test/assertion
   counts, compiler diagnostics, findings and review dispositions, warnings,
   and limitations. Append these records to the coordinator's existing
   Foresight `.ημ/receipts.edn`, referencing the authoritative story and this
   PR; preserve historical records and omit credentials.
8. The README uses standalone commands instead of `pnpm -C packages/rheos` and
   describes all four build targets: `server`, `cli`, `github-sync`, and `app`.
9. The workflow has pull-request, `main` push, and manual triggers without
   inherited monorepo path filters; it uses read-only repository permissions,
   pinned action revisions, the declared tool versions, a fixed runner label
   (`ubuntu-24.04`, not a `-latest` alias), and a bounded timeout.
   Actual tool versions are visible, and the job requires no deployment or
   review credentials.
10. Before the next planning-review resubmission, retain the clean
    complete-manifest install and unchanged-source release baseline using the
    existing pinned source bootstrap and `pnpm build`, with their exact revision
    and success or failure diagnostics. This comparison point does not replace
    final qualification or retrospectively qualify earlier planning reviews.
11. Installation/build remediation keeps this story within its five-point
    estimate. Added work that would exceed five points is split into linked
    cards before scope expands, with its own acceptance and verification criteria.

## Verification sequence

Before the next planning-review resubmission, record both the clean
declared-install baseline and the unchanged-source release baseline. Run the
existing pinned source bootstrap and `pnpm build` at the recorded baseline
revision, retaining success or failure diagnostics. Earlier planning reviews
already occurred; these attempts precede resubmission and do not change that
history. After planning review meets the canonical pr-flow exit conditions and
all planning findings are settled, move the authoritative story to `ready`
through Rheos. Then qualify installation and lockfile generation in an isolated
checkout, run `pnpm install --frozen-lockfile` against the committed lockfile,
verify bootstrap success and its missing/empty-tree failure cases, run the three
existing package commands, inspect the release outputs, and run built CLI help.

For any installation/build remediation, record the failing command, cause,
verification, and effect on the five-point estimate before changing scope.
If added work would exceed five points, create linked repair cards and review
their scope and readiness through canonical pr-flow before implementing those
changes. A failed check does not automatically broaden this story.

Before Receipt River cites baseline logs or result manifests, copy or upload
them to a shared durable location and publish immutable links and hashes.
Append the observed results after baseline, local, and hosted qualification,
including the exact revisions and commands, test/assertion
counts and compiler diagnostics, failures or warnings, findings, and scope
limitations. Reference retained logs and review evidence so the authoritative
Foresight card's qualification record is inspectable without treating this
plan or a passing check as a substitute for execution evidence.

Check the README commands and release-target description against the standalone
package and all four build targets. Inspect the workflow's triggers, permissions,
action and tool pins, fixed `ubuntu-24.04` runner label (reject `-latest` aliases),
timeout, and version-reporting step against its scope.

Validate workflow syntax and explicit script names without adding a second
implementation of Rheos semantics. Push the implementation to the personal
fork, inspect the hosted job output, and request one explicit full CodeRabbit
review for the resulting head, alongside the canonical pr-flow review policy.
After the final current-head full review completes, verify and record an
explicit disposition for every finding before reporting the PR as reviewed or
reaching the merge gate. A skipped, rate-limited, stale, or pending review is not
a completed review. A new push requires new qualification, check, and review
evidence.

### Declared-install baseline

On 2026-10-03, a clean worktree at `dda73f1` ran
`env -u NODE_PATH NPM_TOKEN= pnpm install --no-frozen-lockfile` with Node
`24.14.1` and pnpm `10.15.0`. It exited zero, installing all declared packages,
including the pinned protocols Git dependency and the frontend dependencies.
The original full log is published as
[declared-install evidence](https://github.com/riatzukiza/foresight/blob/a167d66e322af6dbedd6339fefe0c9ad9cb8bd85/.%CE%B7%CE%BC/diagnostics/rheos-declared-install-dda73f1-20261003/install.log).

The generated lockfile is baseline output only; it is not introduced by this
planning commit. Current ranges resolved shadow-cljs `3.5.4`, React/ReactDOM
`19.3.0`, marked `18.0.14`, DOMPurify `3.4.16`, and YAML `2.9.1`, among other
dependencies. Review and qualify this resolution rather than treating the
earlier shadow-cljs `3.4.10` test results as evidence for it. The installer
reported deprecated transitive `glob@11.1.0`; it did not report an installation
failure. No source bootstrap, tests, lint, release build, or frozen install was
run as part of this baseline probe.

### Unchanged-source release baseline for resubmission

On 2026-10-03, a fresh isolated checkout at
`4c2ade84ae58fc0d3b86019fb2266fc58efa0c3c` ran the complete declared install,
`bash scripts/bootstrap-source-deps.sh`, and unchanged `pnpm build`; each exited
zero. All commands used `env -u NODE_PATH NPM_TOKEN=` without an external
dependency prefix. Node `24.14.1`, pnpm `10.15.0`, and OpenJDK `21.0.12.1` were
observed. The resolved dependency versions match the earlier install baseline,
including shadow-cljs `3.5.4`; deprecated transitive `glob@11.1.0` remained the
only installer warning. The pinned bootstrap supplied 21 protocols files and
10 chat-ui files. Release compilation passed for `server` (104 files, 45
compiled), `cli` (108/48), `github-sync` (64/16), and `app` (95/49), each with
zero compiler warnings; all four expected outputs exist and are nonempty.

The original logs and result manifest are durably published in Foresight
commit `a167d66e322af6dbedd6339fefe0c9ad9cb8bd85`: [install.log](https://github.com/riatzukiza/foresight/blob/a167d66e322af6dbedd6339fefe0c9ad9cb8bd85/.%CE%B7%CE%BC/diagnostics/rheos-ci-release-baseline-4c2ade8-20261003/install.log),
[bootstrap.log](https://github.com/riatzukiza/foresight/blob/a167d66e322af6dbedd6339fefe0c9ad9cb8bd85/.%CE%B7%CE%BC/diagnostics/rheos-ci-release-baseline-4c2ade8-20261003/bootstrap.log), [build.log](https://github.com/riatzukiza/foresight/blob/a167d66e322af6dbedd6339fefe0c9ad9cb8bd85/.%CE%B7%CE%BC/diagnostics/rheos-ci-release-baseline-4c2ade8-20261003/build.log)
and [baseline.json](https://github.com/riatzukiza/foresight/blob/a167d66e322af6dbedd6339fefe0c9ad9cb8bd85/.%CE%B7%CE%BC/diagnostics/rheos-ci-release-baseline-4c2ade8-20261003/baseline.json). The manifest retains resolved
versions, source/output hashes and command exits; the archived bytes match its
recorded log hashes. These artifacts are published before receipt citation.
Tracked source/configuration files are unchanged; the generated lockfile
remains baseline output only. No frozen install, tests,
lint, CLI help, hosted standalone CI, or runtime interaction was qualified by
this probe. These results support the next planning resubmission; no prior
planning clearance or readiness transition is claimed.

## Risks and bounded follow-on work

The manifest includes frontend and Git dependencies that the previous test
prefix did not install. Registry availability, the pinned protocols Git
dependency, install hooks, and release-only imports may expose extraction gaps.
Do not remove or bypass a failing release target. Keep the failing command and
diagnostics inspectable; repair a small installation/build boundary defect in
this story only when its cause and verification are clear. Record broader
runtime repairs as linked follow-up cards before expanding scope.
If added work would exceed the five-point estimate, split it into linked cards
before expanding the scope.

Several existing adapters use `^:async` metadata and `await`; the standalone
compiler and runtime behavior must be inspected rather than inferred from a
test pass. CLI help checks entry-point loading only. This story does not prove
HTTP/MCP transports, server startup, GitHub issue projection, browser
interaction, deployment, packaging, or the future content-management review
loop. In particular, the GitHub-sync executable has no help branch; do not run
it against live GitHub in this credential-free CI check.

The canonical release command is the current pnpm `build` script. Aligning
Babashka's narrower build task can be a separate change. Hosting, repository
settings, publishing, and board implementation changes are outside this story.
