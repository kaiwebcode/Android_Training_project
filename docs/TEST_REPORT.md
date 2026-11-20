# Employee Task Manager — Week 3 Day 14 Test Report

## Recorded automated results

| Suite | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|
| JVM unit tests | 49 | 0 | 0 | 0 |
| Isolated Android instrumentation, OnePlus EB2101 / Android 12 | 14 | 0 | 0 | 0 |
| **Total** | **63** | **0** | **0** | **0** |

See the [JVM summary](evidence/day14/unit-test-results.txt) and [instrumentation summary](evidence/day14/instrumentation-test-results.txt). Raw XML is generated under `app/build/test-results/testDebugUnitTest/` and `app/build/outputs/androidTest-results/connected/debug/`.

| Category | Automated coverage |
|---|---|
| Room persistence | 8 DAO tests and 5 populated migrations from v1–v5 to v6. |
| Worker outcomes and scheduling | Success, retry and return to `PENDING`, terminal failure on missing DAO, and the enqueued request's connected constraint, unique work policy, and 10-second initial exponential backoff. |
| API error mapping | HTTP 400, 404, 500, timeout, I/O failure, and malformed JSON. |
| Duplicate prevention | Repeated Add and Complete ViewModel actions and stable-ID replay after simulated timeout. |
| ViewModel states | Initial `Loading`, `Content`, `Empty`, `Error`, and retry recovery. |

## Physical scenarios

| Scenario | Evidence |
|---|---|
| Offline create → restart → online sync | DAO reopening and upload pass separately in tests. The claimed offline UUID is absent from the source snapshot and the full device sequence is unverified. |
| Network loss during upload | The device trace verifies HTTP 504 → WorkManager retry → same UUID → success. A physical connectivity loss was not captured. [Filtered trace](evidence/day14/http504-retry-log.txt) |
| Repeated Add and Complete | Automated regression coverage. A post-fix device run has not been evidenced. |
| Rotation during loading | The slow refresh indicator is tested. The Day 13 observation records Room completion retained after rotation. Rotation while loading and the subsequent rendered state are unverified. |
| Process death and recovery | DAO and worker tests recover a seeded `SYNCING` row. No physical in-flight process kill and recovery was captured. |
