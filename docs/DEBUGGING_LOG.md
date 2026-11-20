# Debugging Log — Week 2 & Week 3 Defect Resolution Framework

This document tracks defect diagnosis and resolution using the 4-step defect debugging framework:
$$\text{Reproduce} \longrightarrow \text{Observe} \longrightarrow \text{Isolate} \longrightarrow \text{Verify}$$

---

## Defect 1: Malformed API Data & JSON Deserialization Crash
- **Symptom / Description:** When remote API returns malformed JSON or HTML error payloads, raw deserialization crashed the app.
- **Isolate & Fix:** Caught `JsonSyntaxException` in `ApiTaskRepository` and added safe DTO fallbacks in `TaskDto.toDomainTask()`.
- **Verify:** Added unit test `getTasks_returnsErrorOnMalformedJson()`. Tests pass.

---

## Defect 2: HTTP 500 Server Error & Network Timeout Exception Handling
- **Symptom / Description:** Screen stuck in loading or threw low-level exceptions during server timeouts or HTTP 500 errors.
- **Isolate & Fix:** Mapped `SocketTimeoutException` and HTTP 500/502/503 errors to user-friendly messages triggering `TaskListUiState.Error` and an actionable **Retry** button.
- **Verify:** Added unit tests `getTasks_returnsErrorOnNetworkTimeout()` and `retryFetch_reloadsTasksAfterError()`. Tests pass.

---

## Defect 3: UI Not Updating After Refresh / Stale State Sync Issue
- **Symptom / Description:** Completing tasks occasionally failed to update UI counters or RecyclerView list items due to identical list reference passing.
- **Isolate & Fix:** Returned distinct list copies (`localTasks.toList()`) and recalculated counters in `TaskListViewModel` emitting a fresh `TaskListUiState.Content` reference.
- **Verify:** Added unit test `completeTask_updatesTaskCompletionAndCelebrationState()`. Tests pass.

---

## Defect 4: Release APK Runtime Crash (`IllegalAccessError`)
- **Symptom / Description:** Release build crashed on physical device startup with `java.lang.IllegalAccessError: Class kotlin.sequences.d extended by class wq is inaccessible` at `MainDispatcherLoader.loadMainDispatcher`.
- **Step 1 — Reproduce:** Built release APK with aggressive R8 `optimization` enabled on `androidx.**`, `kotlin.**`, `kotlinx.**` packages and launched on physical device.
- **Step 2 — Observe:** Logcat captured `IllegalAccessError` during coroutine dispatcher initialization.
- **Step 3 — Isolate:** The crash was observed with release optimization enabled and was not observed after disabling it. This before/after behavior suggests an interaction with release optimization; it does not prove that R8 transformed Kotlin or coroutines metadata.
- **Step 4 — Verify:** Disabled release code optimization (`optimization { enable = false }`) in `app/build.gradle.kts` and rebuilt the release APK. The user manually confirmed installation and launch on a physical device. No automated ADB install/launch verification or independently captured Logcat evidence is recorded here.

---

## Defect 5: Room DAO Instrumentation Test Runner Initialization Error (`InvalidTestClassError`)
- **Symptom / Description:** Running `.\gradlew.bat connectedDebugAndroidTest` on connected OnePlus Nord CE (EB2101, Android 12) threw `RuntimeException: Failed to instantiate test runner class androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner` caused by `InvocationTargetException` / `InvalidTestClassError: Method persistenceAcrossReopeningDatabase() should be void`.
- **Step 1 — Reproduce:** Executed `.\gradlew.bat connectedDebugAndroidTest` with physical device attached. Test runner failed on `TaskDaoTest.initializationError`.
- **Step 2 — Observe:** Detailed test report stack trace revealed JUnit's `ParentRunner.validate()` failed with `Method persistenceAcrossReopeningDatabase() should be void`.
- **Step 3 — Isolate:** `persistenceAcrossReopeningDatabase()` was declared with expression body `= runBlocking { ... }`. Its last statement `context.deleteDatabase(dbName)` returned `Boolean`, causing Kotlin to infer a `Boolean` return type. JUnit 4 strictly requires `@Test` methods to return `void`.
- **Step 4 — Verify:** Refactored all `@Test` methods in `TaskDaoTest.kt` to use explicit block syntax `{ runBlocking { ... } }` returning `Unit` / `void`, and added `androidx.test:core-ktx` & `androidx.test:runner` to `app/build.gradle.kts`. Re-executed `.\gradlew.bat connectedDebugAndroidTest` on OnePlus Nord CE. Result: **6 PASSED, 0 FAILED**.

---

## Defect 6: WorkManager Worker Test Bytecode Compatibility on Java 25 (`Unsupported class file major version 69`)
- **Symptom / Description:** Robolectric 4.14.1 test runner threw `IllegalArgumentException: Java 25 (69) is not supported by the current version of Byte Buddy` when attempting to instrument Android framework classes.
- **Step 1 — Reproduce:** Executed unit test suite with Robolectric runner on Java 25 JDK.
- **Step 2 — Observe:** ByteBuddy failed to retransform `android.content.Context` because Java 25 class version 69 exceeds ByteBuddy support.
- **Step 3 — Isolate:** Worker background sync algorithm logic in `TaskSyncWorker` was coupled with Android `Context` in constructor.
- **Step 4 — Verify:** Refactored `TaskSyncWorker` to expose static method `performSync(dao, api)` and decoupled worker execution from Android context mocks, allowing pure JVM unit testing via `TaskSyncWorkerTest`. Result: **31 PASSED, 0 FAILED**.

---

## Defect 7: Physical Device Mock Sync Routing & Cleartext HTTP Block (`HTTP 404` / Cleartext Traffic Block)
- **Symptom / Description:** Initial physical device sync calls resulted in `HTTP 404` on `POST /public/v3/tasks` and blocked connections when communicating with the development mock server.
- **Step 1 — Reproduce:** Created tasks offline, restored internet, observed Logcat and mock server logs.
- **Step 2 — Observe:** Logcat showed requests directed to `/public/v3/tasks` (GoRest path) and cleartext HTTP security blocks by Android OS.
- **Step 3 — Isolate:** `TaskSyncWorker` was sharing GoRest `TaskApiService` with `@POST("public/v3/tasks")`, and `AndroidManifest.xml` lacked `android:usesCleartextTraffic="true"`.
- **Step 4 — Verify:** Created dedicated `TaskSyncApiService` and `ApiSyncClient` targeting the development mock server, added `android:usesCleartextTraffic="true"`, and set `ExistingWorkPolicy.REPLACE`. Re-tested live on physical device: Task 1, Task 2, and Task 3 successfully synced to mock server (`SYNCED`, `syncOp = NONE`, zero duplicates).

---

## Defect 8: Duplicate Action Execution & Stale Response Overwrite Race Conditions
- **Symptom / Description:** Rapid repeated taps on "Complete" or "Add Task" dispatches duplicate network requests; slow fetch requests overwrote newer UI state if completed out of order.
- **Step 1 — Reproduce:** Simulated slow API response (7s delay) and rapidly tapped "Complete" button 5 times or tapped "Retry" mid-load.
- **Step 2 — Observe:** Multiple coroutines launched concurrently for the same task ID; obsolete loading requests completed after newer requests and overwrote UI state.
- **Step 3 — Isolate:** `TaskListViewModel` lacked synchronous in-progress guards (`inProgressTaskIds`) and obsolete job cancellation (`fetchJob?.cancel()`), while `ApiTaskRepository` swallowed `CancellationException` in generic catch blocks.
- **Step 4 — Verify:** Added synchronous in-progress set guards with `finally` cleanup, added `fetchJob?.cancel()` in `loadTasks()`, re-threw `CancellationException` in repository catch blocks, and refactored `TaskDetailsActivity` with `TaskDetailsViewModel`. Verified via 36 unit tests including `completeTask_duplicateTaps_executesOnlyOnce` and `loadTasks_obsoleteJobCancelled_onlyLatestResponseUpdatesUi`. Result: **36 PASSED, 0 FAILED**.

---

## Defect 9: Bug A — Duplicate Task After Retry & Idempotent Client UUID Handling
- **Symptom / Description:** Potential duplicate task creation on the development mock server during network retry attempts.
- **Step 1 — Reproduce:** Created local task `"Day 13 bug A"` on OnePlus EB2101 device connected to Node.js mock sync server.
- **Step 2 — Observe:** Inspected Node.js mock server endpoints `GET /tasks` before and after synchronization (`docs/evidence/day13/bugA-before.json` and `docs/evidence/day13/bugA-after.json`). Exactly 1 unique UUID task record (`clientTaskId = "cbe9ffbb-4dcd-45e0-a94c-d7698e7aaeac"`) was created on server (`docs/evidence/day13/bugA-unique-task.json`), and Room held `syncState = SYNCED`, `syncOp = NONE`.
- **Step 3 — Isolate:** Analyzed client-side UUID generation and `TaskSyncWorker` retry execution paths. The client generates a stable UUID `id` (`clientTaskId`) upon task creation.
- **Step 4 — Hypothesize:** If the response is lost after persistence, replaying the same client ID lets the server return the existing record.
- **Step 5 — Fix:** Preserved stable UUID generation in `ApiTaskRepository.addTask()` and idempotent `clientTaskId` transmission in `TaskSyncWorker`. On a successful CREATE replay, compare the returned task to the submitted snapshot and recheck the current Room row. Queue a PATCH for a newer edit under the same UUID.
- **Step 6 — Verify:** The separate Day 14 trace shows HTTP 504, retry with the same UUID, an idempotent acknowledgement, and worker success. The server extract retains one matching record. [Retry trace](evidence/day14/http504-retry-log.txt) · [Server extract](evidence/day14/server-tasks-final.json)

---

## Defect 10: Bug B — Queue Stuck in SYNCING (PATCH and origin isolation)
- **Reproduce:** Run WorkManager with the nine legacy GoRest tasks marked PENDING/UPDATE.
- **Observe:** The captured worker log recorded Retrofit rejecting `Map<String, ?>` before making a PATCH request. The later logs show one successful local CREATE and one run with an empty queue.
- **Isolate:** The separate mock client requires a concrete PATCH body. Older databases also contain origin labels that the v4 and v5 migrations could assign from sync metadata alone.
- **Hypothesize:** A typed PATCH body, verified local origin at claim time, and a retry after a stale acknowledgment address the failures without discarding any task edits.
- **Fix:** Use `TaskSyncUpdateDto` and migrate Room v1–v5 directly to v6. Keep verified GoRest labels from v2/v3. Set unclear legacy origins to UNKNOWN. Match exact GoRest IDs to confirm UNKNOWN rows through an origin-only update. Claim only eligible LOCAL rows. Recover interrupted SYNCING rows and retry a failed acknowledgment.
- **Verify:** See the current build and device results in [TEST_REPORT.md](TEST_REPORT.md). The existing device logs establish CREATE and empty-queue behavior.

---

## Defect 11: Bug C — Task Completion Reversal Across Screen Rotation
- **Symptom / Description:** Potential reversal of task completion status (`completed = true` -> `completed = false`) upon device screen rotation.
- **Step 1 — Reproduce:** Marked task `"Day 13 Rotation C"` completed (`completed = true`) on OnePlus EB2101 device. Rotated device screen (configuration change).
- **Step 2 — Observe:** Inspected Room database after rotation. Task `"Day 13 Rotation C"` remained `completed = 1` (`true`), `origin = LOCAL`, `syncState = SYNCED`, `syncOp = NONE`.
- **Step 3 — Isolate:** Analyzed `TaskDetailsViewModel` and `TaskListViewModel` state preservation across rotation. `TaskDetailsViewModel` retains task state in `StateFlow` across Activity re-creation, and local tasks (`origin = LOCAL`) are protected in `ApiTaskRepository.getTasks()` from being overwritten by remote GoRest responses.
- **Step 4 — Hypothesize:** Preserving ViewModel state across rotation and protecting local/synced tasks from remote overwrite prevents completion reversal.
- **Step 5 — Fix:** Maintained ViewModel state flow lifecycle and protected local tasks in `ApiTaskRepository.getTasks()`.
- **Step 6 — Verify:** Screen rotation verified on OnePlus EB2101 device; task `"Day 13 Rotation C"` retained `completed = 1` (`true`), `origin = LOCAL`, `syncState = SYNCED`, `syncOp = NONE` in Room database after rotation.

---

## Defect 12: Day 14 Reliability Verification

- **Reproduce:** Trigger an HTTP 504 after a mock-server save. The automated regressions separately exercise missing DAO, rapid taps, and recovery from a seeded `SYNCING` row.
- **Observe:** The captured retry preserves the work ID and client UUID. The server reports an idempotent replay and the worker returns success.
- **Isolate:** The stable UUID links the retry and the server record. ViewModel guards and DAO startup recovery are covered by automated assertions.
- **Verify:** The [filtered trace](evidence/day14/http504-retry-log.txt) and [server extract](evidence/day14/server-tasks-final.json) establish the live retry outcome. A device network drop, post-fix repeated taps, rotation during loading, offline restart sequence, and in-flight process kill remain unverified. Defect 11 covers only the earlier Room retention observation.

---

## Known Limitations & Disclosures
1. **Legacy provenance:** A row stays UNKNOWN until its origin can be verified. Its content and pending edits stay in Room while uploads are paused.
2. **Device Storage Threshold:** Device installation requires at least ~500 MB free storage on `/data/user/0` to allow package installation.
