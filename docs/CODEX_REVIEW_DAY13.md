# Independent Codex Review — Week 3 Day 13

## Confirmed blocker 1 — Legacy origin classification

The audit ran the historical migration SQL on populated SQLite fixtures. The v4 paths could relabel genuine local tasks as GoRest, and the v5 paths could relabel pending GoRest tasks as local. Some direct upgrades also created a column default that differed from the exported Room schema.

Room v6 registers direct upgrades from every supported version, v1 through v5. The v2/v3 paths preserve GoRest labels assigned by the API and quarantine ambiguous local labels. V1 had no origin column. V4/v5 could rewrite either label, so their rows start as UNKNOWN. Pending edits, completion, descriptions, titles, timestamps and IDs survive migration. The v6 schema uses UNKNOWN as the default consistently.

The sync queue claims only verified LOCAL rows. New Room-backed user creates are explicitly LOCAL. GoRest fetches can confirm an UNKNOWN row by an exact remote ID and update only its origin. UNKNOWN rows remain visible and editable. Some historical local tasks may remain UNKNOWN indefinitely because the old schemas lack reliable provenance.

## Confirmed blocker 2 — Stale CREATE replay

A successful CREATE replay can return the original server row even when the retry submitted newer fields. The worker now checks the returned UUID and fields against the submitted task. Room then compares the current row with the submitted snapshot in a transaction. If CREATE is confirmed and newer data remains, Room preserves the latest row as PENDING/UPDATE and the worker returns retry. The next run PATCHes the same UUID. A transport failure before confirmation keeps the CREATE retryable.

The Retrofit regression test exercises HTTP 504 after save, a local completion edit, POST replay of the older server row, PATCH, and final consistency on one UUID. The previously captured device evidence still establishes one final record. A live device 504 sequence was not captured.

See [TEST_REPORT.md](TEST_REPORT.md) for executed tests and the device isolation setup.
