# Employee Task Manager — Test Report (Week 3 Day 13)

| Check | Result |
|---|---|
| `./gradlew.bat :app:testDebugUnitTest` | 45 passed, 0 skipped, 0 failed |
| `./gradlew.bat :app:assembleDebug` | Successful |
| `./gradlew.bat -Pday13IsolatedTest=true :app:connectedDebugAndroidTest` | 14 passed on OnePlus EB2101 |
| `git diff --check` | Exit 0 |

## Regression coverage

- **JVM tests:** TaskListViewModel 15, TaskDetailsViewModel 1, TaskSyncWorker 9, ApiTaskRepository 19, ExampleUnitTest 1.
- **Mock sync:** Real Retrofit against MockWebServer verifies typed PATCH serialization. The stale CREATE replay test drives HTTP 504 after save, a newer local edit, POST replay, then PATCH. It checks the request sequence, one stable UUID, and the latest value on the server.
- **Room migration:** Five instrumentation tests open seven populated v1–v5 fixtures with Room v6. The tests cover both historical column defaults for v4 and v5. Room validates the upgraded schema, and assertions cover preserved task fields, quarantine, exact GoRest confirmation, and mock-server queue eligibility.
- **Device isolation:** The connected task compiled and ran the instrumentation APK. The Gradle property selects a separate debug app ID, and the test runner avoids starting the production synchronization application. Tests use isolated databases. The installed app and its storage were preserved. No production database or mock-server data was deleted or reset.

The captured Day 13 evidence proves one Bug A server row and reports Room persistence through Bug C rotation. A live Bug A 504 sequence and Bug C after a confirmed refresh remain unverified. The full Node suite was not run against the user's database.
