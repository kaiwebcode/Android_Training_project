# Week 3 Day 14 — Delivery and Release Validation

Branch `feature/day14-reliability-tests`. The Friday gate remains open.

| Requirement | Status |
|---|---|
| APK builds and runs | See [README](../README.md) for the output path and launch steps. The [app screenshot](evidence/day14/taskmanager-device.png) shows the app running during an earlier session. A launch tied to the final rebuilt APK is unverified. |
| Offline create, restart, sync, and retry | The live HTTP 504 retry is evidenced. The complete offline restart sequence and a physical network interruption are unverified. |
| No duplicate server task after timeout and retry | The [filtered trace](evidence/day14/http504-retry-log.txt) shows one work ID returning retry and then success for the same task UUID. The [server extract](evidence/day14/server-tasks-final.json) preserves the single matching record from the source snapshot. |
| Test output, screenshots, and logs | JUnit summaries, an app screenshot, the filtered retry trace, and the matching server row are included. The [worker idle log](evidence/day14/workmanager-final-log.txt) shows a successful empty-queue run. Scenario-specific device evidence gaps are listed in the [test report](TEST_REPORT.md#physical-scenarios). |
| Reviewable PR | Review notes and links are below. Submission commit, PR link, and attachment of the APK and reviewed evidence remain pending. |

## Review notes

- The development mock endpoint is configurable for debug builds. The release build has no mock sync endpoint. The task API paths and payloads are unchanged.
- The worker and scheduler tests cover terminal failure, retry, and the actual enqueued work configuration. ViewModel regressions cover repeated Add and Complete actions and initial loading.
- See the [sync diagram](diagrams/offline-sync.mmd), [debugging log](DEBUGGING_LOG.md), and [current status](CURRENT_STATUS.md).
