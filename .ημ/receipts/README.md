# Receipt River segments

Each file here is one immutable segment of the Rheos Receipt River ledger. Name
new files `<UTC timestamp>-<id>.edn`, for example
`2026-10-13T164500123Z-3f9a1c2b7d4e.edn`, and put exactly one EDN receipt in
it, ending with a newline. Never edit, rename or delete a committed segment.

`../receipts.edn` is the frozen legacy first segment. Do not append to it.

`bb verify-receipts` (or `bb scripts/verify_receipts.clj --base <rev> --at <rev>`)
loads the legacy file and every segment and checks the merged ledger as a whole:
no base segment edited or removed, no base record dropped, no record repeated,
no new record in the legacy file (which must stay a regular `100644` file), and
every new record has a valid receipt envelope, including a nonblank `:repo` and
a strict UTC `:ts` spelled with `Z` (`2026-10-13T16:45:00.123Z`). A segment is
one EDN form, so a pretty-printed receipt over several lines is fine. CI runs
`--range`, which checks every commit against each of its real parents and keeps
the supplied base, so a force-push is compared with the pre-push tip. The rules are ported from riatzukiza/foresight#35; eta-mu's
Clio-backed receipt commands will replace this script.
