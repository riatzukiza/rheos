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

## Problem and outcome

The standalone extraction retained the package commands but omitted the
[donor's Rheos workflow](https://github.com/open-hax/eta-mu/blob/0ed56aa74a53a1d1e9c2e55ce95451817a7f3a90/.github/workflows/rheos.yml).
A full test pass therefore does not establish clean package installation or
successful release compilation. Restore an independently executable CI check
that installs this repository, bootstraps its declared source dependencies, and
runs its existing test, lint, and release commands.

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
  declared in `shadow-cljs.edn`.
- Run `pnpm test`, `pnpm lint:kondo`, and `pnpm build` as separate visible
  steps. Preserve compiler diagnostics on failure, and retain installation and
  bootstrap failures in the normal job log. Correct the README's stale
  monorepo commands and release-target description.

## Acceptance criteria

1. A fresh checkout installs the complete declared dependency manifest without
   `NODE_PATH` or an external tool/dependency prefix, and a subsequent frozen
   install succeeds against the committed lockfile.
2. The existing source bootstrap supplies its pinned protocols and chat-ui
   inputs before compilation. A missing dependency or tool fails the job.
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
   PR's current head and records a successful check. Every review finding is
   fixed, deferred to a named card, or rejected with evidence before the final
   full review and merge gate.

## Verification sequence

Before implementation, record the clean declared-install baseline and preserve
its log. After the planning review is settled, move the authoritative story to
`ready` through Rheos. Then qualify installation and lockfile generation in an
isolated checkout, run the pinned source bootstrap, run the three existing
package commands, inspect the release outputs, and run built CLI help.

Validate workflow syntax and explicit script names without adding a second
implementation of Rheos semantics. Push the implementation to the personal
fork, inspect the hosted job output, and request one explicit full CodeRabbit
review for the resulting head. A skipped, rate-limited, stale, or pending
review is not a completed review. A new push requires new check and review
evidence.

### Declared-install baseline

On 2026-10-03, a clean worktree at `dda73f1` ran
`env -u NODE_PATH NPM_TOKEN= pnpm install --no-frozen-lockfile` with Node
`24.14.1` and pnpm `10.15.0`. It exited zero, installing all declared packages,
including the pinned protocols Git dependency and the frontend dependencies.
The full log is retained locally at
`/tmp/rheos-standalone-ci-install-baseline-20261003.log`.

The generated lockfile is baseline output only; it is not introduced by this
planning commit. Current ranges resolved shadow-cljs `3.5.4`, React/ReactDOM
`19.3.0`, marked `18.0.14`, DOMPurify `3.4.16`, and YAML `2.9.1`, among other
dependencies. Review and qualify this resolution rather than treating the
earlier shadow-cljs `3.4.10` test results as evidence for it. The installer
reported deprecated transitive `glob@11.1.0`; it did not report an installation
failure. No source bootstrap, tests, lint, release build, or frozen install was
run as part of this baseline probe.

## Risks and bounded follow-on work

The manifest includes frontend and Git dependencies that the previous test
prefix did not install. Registry availability, the pinned protocols Git
dependency, install hooks, and release-only imports may expose extraction gaps.
Do not remove or bypass a failing release target. Keep the failing command and
diagnostics inspectable; repair a small installation/build boundary defect in
this story only when its cause and verification are clear. Record broader
runtime repairs as linked follow-up cards before expanding scope.

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
