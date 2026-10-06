# Direct blocker reporting: planning evidence

Inventory date: 2026-10-06. Related issue:
[open-hax/rheos#4](https://github.com/open-hax/rheos/issues/4).

## Repository and source boundary

The accepted Foresight mapping is retained in personal sync PR 3, exact head
`96a6dca24cb7a14b041bdd6e3e7922c568238da9`,
[`config/dev-origins.edn`](https://github.com/riatzukiza/foresight/blob/96a6dca24cb7a14b041bdd6e3e7922c568238da9/config/dev-origins.edn).
Its row binds source path `rheos`, upstream/network root `open-hax/rheos` and
development origin `riatzukiza/rheos`. The mapping's previously merged personal
history is preserved by that sync head; it is not present on origin main and is
not described as an activated review/deployment controller.

GitHub identifies `riatzukiza/rheos` as a fork whose parent and source are
`open-hax/rheos`; both default branches are `main`. At inspection, personal main
was `11811264a308d406cb612aefa1dad40818675e5e` and upstream main was
`ef3c4abf1ea75199486f693e9470df3fec88dd49`. The personal fork had zero unique
commits and lagged by five accepted upstream commits. This documentation branch
starts at personal main; it does not synchronize, reset, merge or change either
default branch. Existing personal Rheos PRs 1 and 2 retain their separate owners.
The personal repository reports `allow_auto_merge: false`; this slice leaves
that setting unchanged. No workflow files are present at the chosen base, so an
active workflow registration by itself proves no review execution on this head.

Neither inspected Git tree contains an `AGENTS.md` or project-local `SKILL.md`,
despite the README's AGENTS pointer. The applicable global contract and canonical
pr-flow/pr-sprint-planning skills govern this slice. No build convention was
invented to fill that documentation gap.

## Canonical code inspected

- [`src/rheos/backend/law/fsm.cljs`](../../src/rheos/backend/law/fsm.cljs):
  `promethean-fsm` permits only `breakdown -> blocked`; it retains
  `blocked -> breakdown|ready`. The blocked limit is 15, but structural WIP
  admission occurs only when the matching edge uses `:wip-available`.
- [`src/rheos/backend/domain/transition.cljs`](../../src/rheos/backend/domain/transition.cljs):
  `decide-move` resolves the configured canonical FSM and current task counts.
- [`src/rheos/backend/infra/transition.cljs`](../../src/rheos/backend/infra/transition.cljs):
  `move-task!` rejects before writeback, runs admitted command gates, writes the
  status and emits the existing event; same-status operations are no-ops.
- [`src/rheos/backend/infra/cli.cljs`](../../src/rheos/backend/infra/cli.cljs):
  `cmd-move` calls that writer; `cmd-status-update` routes through
  `kanban_update_status`. Refusals retain the documented nonzero CLI contract.
- [`src/rheos/backend/domain/events.cljs`](../../src/rheos/backend/domain/events.cljs):
  `emit-status-change!` emits `kanban.status-change` through the existing append
  and publication chokepoint, with source/from/to/write identity.
- [`test/rheos/backend/law/fsm_test.cljs`](../../test/rheos/backend/law/fsm_test.cljs):
  existing tests reject multi-hop completion and unknown statuses and retain the
  `in_progress -> review` build-check identity.

The FSM, domain transition, infrastructure transition and CLI files are
byte-identical between the two inspected main revisions. Their source evidence
applies to both; this plan imports none of the five unrelated upstream commits.

## Historical refusal evidence

Issue 4 records the parent's actual installed canonical CLI attempts against an
isolated Foresight checkout at `fcfc2d1`, configured with the Promethean FSM:

```text
eta-mu kanban move e1-11-donor-retirement-after-cutover --to blocked
REJECTED ... No transition from 'todo' to 'blocked'

eta-mu kanban status-update e1-11-donor-retirement-after-cutover --to blocked
Error: transition rejected: No transition from 'todo' to 'blocked'

eta-mu kanban move codex-cloud-fork-selection-projection --to blocked
REJECTED ... No transition from 'incoming' to 'blocked'
```

The issue reports no tracked changes from these refusals. This planning slice
reads that evidence and independently confirms the matching canonical source;
it performs no new live-board move and claims no fresh CLI reproduction.

## Ledger and qualification boundary

The base's tracked `docs/agile/tasks/.events/ledger.edn` is empty, SHA-256
`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
It remains byte-for-byte unchanged by this plan. Execution receipts belong only
under the owned checkout's `.ημ/`; append-only helpers with ambiguous Git-worktree
root discovery are not used.

The proposed card is incoming Markdown. No active task projection/configuration,
Rheos transition, installed CLI upgrade, package release, native planning
approval, WIP decision acceptance, merge, controller or deployment is claimed.
Implementation begins only after planning review and a lawful ready admission.
