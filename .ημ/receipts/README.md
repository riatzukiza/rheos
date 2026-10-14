# Receipt River segments

Each file here is one immutable segment of the Rheos Receipt River ledger. Name
new files `<UTC timestamp>-<id>.edn`, for example
`2026-10-13T164500123Z-3f9a1c2b7d4e.edn`, and put exactly one EDN receipt in
it, ending with a newline. Never edit, rename or delete a committed segment.

`../receipts.edn` is the frozen legacy first segment. Do not append to it.

`bb verify-receipts` (or `bb scripts/verify_receipts.clj --base <rev> --at <rev>`)
loads the legacy file and every segment and checks the merged ledger as a whole:
no base segment edited or removed, no base record dropped, no record repeated,
no new record in the legacy file, and every new record has a valid receipt
envelope. The rules are ported from riatzukiza/foresight#35; eta-mu's
Clio-backed receipt commands will replace this script.
