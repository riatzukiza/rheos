# @eta-mu/rheos

Kanban board runtime and service shell for the eta-mu workspace. Rheos is a
ClojureScript Fastify + React app that reads markdown kanban cards from disk,
serves a board UI, exposes a kanban HTTP/MCP API, and ships a `rheos` CLI for the
same operations. Status moves are FSM-enforced and ledger-backed so the CLI, the
UI, and the MCP tools cannot diverge on what transitions are legal.

It is an active ClojureScript package — there is no TypeScript source here. See
`AGENTS.md` for CLJS conventions and shadow-cljs construction order before
editing build/test flows.

## Build, test, lint

All commands run from this package directory via pnpm:

```bash
pnpm -C packages/rheos build        # shadow-cljs release server cli  -> dist/server.js, dist/cli.cjs
pnpm -C packages/rheos watch        # shadow-cljs watch server-dev (hot reload, dist-dev)
pnpm -C packages/rheos start        # node dist/server.js  (production build output)
pnpm -C packages/rheos start:dev    # node dist-dev/server.js  (dev build output)
pnpm -C packages/rheos test         # shadow-cljs compile test && node dist/test.cjs
pnpm -C packages/rheos lint         # clj-kondo --lint src test
pnpm -C packages/rheos lint:kondo   # alias of lint
pnpm -C packages/rheos clean        # rm -rf dist dist-dev target
```

`build` releases both the `server` and `cli` shadow-cljs builds. The browser
`app` build is not wired into `build`; it is the `:app` shadow target (see
below) and emits to `dist/web/js`, which the server then serves statically. A
`bb.edn` mirrors `build`/`watch`/`test`/`lint`/`clean` for Babashka users.

> The `test` package script runs `node dist/test.cjs`; the `:test` shadow build
> writes its bundle to `dist/test.cjs` with `:autorun true`.

### Frontmatter source-preservation checks

Frontmatter updates and `write-id` injection patch only requested top-level
values. Unrelated YAML, comments, delimiters, line endings, body fences and
spacing remain in their original source form. Reads share one YAML decoder,
including the card loader. Top-level scalar fields retain their decoded source
strings and empty fields retain `""`; nested mappings and vectors keep YAML
number, boolean and null types. Within structured data, native tagged maps/sets
become Clojure maps/sets, timestamps become ISO strings and binary values become
byte vectors.
Cyclic aliases are refused before recursive read conversion or a write, while
shared acyclic aliases remain supported. Invalid/duplicate-key YAML, incompatible
or unresolved standard YAML tags and invalid updates are refused before status
writeback writes the file. Valid standard tagged collections such as `!!set`,
`!!omap`, and `!!pairs` survive unrelated edits. A replacement must satisfy its
retained standard tag, using the YAML library's resolution. Unrelated
application-specific tags remain preserved.

A projected Markdown candidate that cannot be read or parsed rejects the whole
load with `:kind :refused`, its `:source-path`, and the original diagnostic. Board
composition propagates that refusal rather than reporting a partial board. One
refused card therefore makes that load unavailable until its source is repaired;
no empty-frontmatter fallback creates a substitute identity or status. The CLI
reports the path and reason on stderr with exit code 3, and existing HTTP error
responses report that same message. Valid-load shapes, source bytes, configured
projection exclusions, and non-Markdown discovery exclusions stay unchanged.
Present decoded card titles must be strings; collection-valued titles are
refused on load and before a frontmatter edit writes or emits events. Missing
titles keep their filename fallback, scalar spelling stays compatible, and
structured extension metadata remains supported.

The update contract accepts a block mapping, simple string/keyword field names,
and strings, finite numbers, booleans, nil, or vectors of those values. A missing
frontmatter block is added after any file-leading BOM without reformatting the
body. Comment append also retains the original YAML header before targeted
`write-id` injection. Its body and section rendering, and the general task
serializer, still reconstruct content and do not promise lossless editing.
The status writeback adapter uses a native Promise; the standalone compiler
does not transform the other existing `^:async`/`await` adapters.

The focused tests can run from a clean checkout without installing the missing
sibling source trees. Node, Java (for shadow-cljs), and clj-kondo are required:

```bash
rheos_test_deps="$(mktemp -d)"
npm install --prefix "$rheos_test_deps" --ignore-scripts --no-package-lock \
  nbb@1.3.204 yaml@2.9.1 shadow-cljs@3.4.10
NODE_PATH="$rheos_test_deps/node_modules" "$rheos_test_deps/node_modules/.bin/nbb" \
  -cp src:test -e '(require (quote [cljs.test :as t]) (quote [rheos.backend.shape.content-parser-test]) (quote [rheos.backend.infra.task-writeback-test])) (t/run-tests (quote rheos.backend.shape.content-parser-test) (quote rheos.backend.infra.task-writeback-test))'
NODE_PATH="$rheos_test_deps/node_modules" "$rheos_test_deps/node_modules/.bin/shadow-cljs" \
  compile test --config-merge '{:ns-regexp "rheos.backend.(shape.content-parser|infra.task-writeback)-test$"}'
clj-kondo --lint src test
```

The selected shadow build runs its tests with `:autorun true`. A green focused
run does not establish a full-suite pass. The full suite also needs the pinned
source dependencies in `deps/protocols/src` and `deps/chat-ui/src`; the existing
`bash scripts/bootstrap-source-deps.sh` supplies them. Install the declared npm
runtime/tool dependencies before running `pnpm test` from this package root.
Without that source setup, the first missing namespace is
`open-hax.openplanner-protocols`.

An independent checkout of implementation commit
`6e2a6b5ff52f635a67b123595f38d2e347e3642b` ran that bootstrap and the actual
`pnpm test` command with shadow-cljs 3.4.10 and YAML 2.9.1: 159 tests,
827 assertions, zero failures/errors and zero compiler warnings. This qualifies
the full test suite at that revision, not every build target, installed
transport, browser or deployment. The initial missing-source failure is setup
evidence rather than an unavoidable suite blocker.

## shadow-cljs targets

Defined in `shadow-cljs.edn`. Source paths pull in sibling workspace packages:
`../protocols/src`, `../event-ledger/src`, and `../chat-ui/src`.

| Build | Target | Output | Entry / notes |
|-------|--------|--------|---------------|
| `server` | `:esm` `:node` | `dist/` | init-fn `rheos.backend.infra.http-server/init`; `:optimizations :simple` (bare JS interop, no `:advanced`) |
| `server-dev` | `:esm` `:node` | `dist-dev/` | same init-fn; hot reload via `stop-http-before-load!` / `start-http-after-load!` |
| `cli` | `:node-script` | `dist/cli.cjs` | main `rheos.backend.infra.cli/main`; `.cjs` so node runs it as CommonJS under `"type":"module"` |
| `app` | `:browser` | `dist/web/js` | init-fn `rheos.ui.infra.mount/init`; asset-path `/js` |
| `test` | `:node-test` | `dist/test.cjs` | ns-regexp `-test$`, autorun |

Ports: nREPL `8799`, watch HTTP `9634`, dev-http (`resources/public`) `8800`.
The HTTP server listens per `rheos.backend.infra.config` (config + flags).

## CLI

The package installs a `rheos` bin (`dist/cli.cjs`) that owns a card's whole
lifecycle: `create`, `move`, `comment`, `frontmatter`, plus the read verbs and
`serve`.

**→ [`docs/cli.md`](docs/cli.md) is the reference**: install, agent quickstart,
every verb, the exit-code contract, config resolution, and the
surface-ownership table. A test asserts it covers every verb in the CLI's verb
registry — the same registry `rheos --help` renders from — so a new verb cannot
ship undocumented.

```bash
npm i -g @eta-mu/rheos     # Node 22
rheos --help               # every verb
rheos help create          # one verb's flags and a worked example
```

Mutating verbs route through the same domain chokepoints the HTTP handlers and
MCP tools use — `transition/move-task!` for status, `task-edit` for comments and
frontmatter, `task-create` for creation — so the CLI, server, MCP, and UI cannot
diverge. Every mutation appends to the project ledger and publishes to the SSE
stream. `serve` boots the HTTP server in-process.

Failures exit non-zero: `1` usage, `2` not found, `3` refused by policy (FSM,
WIP, build gate), `4` internal.

Config comes from `--config <path>`, `$KANBAN_CONFIG`, or discovery walking up
from the working directory. **EDN is preferred** (`openhax.kanban.edn`);
`openhax.kanban.json` / `kanban.json` still load with a deprecation warning.

## HTTP / MCP surface

`rheos.backend.infra.http-server` registers a Fastify app (with `@fastify/cors`
and `@fastify/static` serving the built web UI from `dist/web`):

- `GET /api/projects`, `GET /api/boards`, `GET /api/board`, `GET /api/board/compose`
- `GET /api/events`, `GET /api/events/stream` (SSE), `GET /api/drift`
- `GET /api/task/:uuid/content`
- `PATCH /api/task/:uuid/frontmatter`
- `POST /api/task/:uuid/comment`, `POST /api/task/:uuid/status`, `POST /api/task/:uuid/open-editor`
- `POST /mcp` — MCP transport (`rheos.backend.infra.mcp`) exposing the `kanban_*` tools
- `POST /api/chat/start`, `POST /api/chat`, `GET /api/chat/stream` — chat proxy
- `GET /api/health`

## Namespace layout

CLJS source lives under `src/rheos/`, split into a backend service and a browser
UI, each using a domain / law / shape / infra layering:

- `rheos.backend.domain` — `board`, `compose`, `events`, `task-create`,
  `task-edit`, `transition` (the last three are the write chokepoints: creation,
  frontmatter/comments, status)
- `rheos.backend.law` — `frontmatter`, `fsm` (legal-transition rules)
- `rheos.backend.shape` — portable `content-parser` source frames, sections and
  range-based patches; `kanban` (markdown card parsing)
- `rheos.backend.extern` — native YAML decoding/encoding and task-content JS conversion
- `rheos.backend.infra` — `http-server`, `cli`, `mcp`, `config`, `projects`,
  `store` / `task-store` / `view-store`, `ledger`, `watcher`, `task-writeback`,
  `agent-tools`, `chat-proxy`, `content-parser` (validated YAML composition)
- `rheos.ui.domain` — `board`, `filter-bar`, `layout`, `orchestrator`, `sidebar`
- `rheos.ui.law` — `url`
- `rheos.ui.infra` — `mount`, `api`, `chat-session`, `ledger-stream`

The ledger and protocol contracts come from the sibling
`@promethean-os/event-ledger` and `@promethean-os/openplanner-protocols`
packages; the chat UI components come from `@open-hax/chat-ui` (all wired via
shadow-cljs source paths).
