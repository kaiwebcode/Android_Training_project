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
- **Symptom / Description:** Initial physical device sync calls resulted in `HTTP 404` on `POST /public/v3/tasks` and blocked connections when communicating with `http://192.168.1.114:3000`.
- **Step 1 — Reproduce:** Created tasks offline, restored internet, observed Logcat and mock server logs.
- **Step 2 — Observe:** Logcat showed requests directed to `/public/v3/tasks` (GoRest path) and cleartext HTTP security blocks by Android OS.
- **Step 3 — Isolate:** `TaskSyncWorker` was sharing GoRest `TaskApiService` with `@POST("public/v3/tasks")`, and `AndroidManifest.xml` lacked `android:usesCleartextTraffic="true"`.
- **Step 4 — Verify:** Created dedicated `TaskSyncApiService` and `ApiSyncClient` targeting `http://192.168.1.114:3000/tasks`, added `android:usesCleartextTraffic="true"`, and set `ExistingWorkPolicy.REPLACE`. Re-tested live on physical device: Task 1, Task 2, and Task 3 successfully synced to mock server (`SYNCED`, `syncOp = NONE`, zero duplicates).

---

## Defect 8: Duplicate Action Execution & Stale Response Overwrite Race Conditions
- **Symptom / Description:** Rapid repeated taps on "Complete" or "Add Task" dispatches duplicate network requests; slow fetch requests overwrote newer UI state if completed out of order.
- **Step 1 — Reproduce:** Simulated slow API response (7s delay) and rapidly tapped "Complete" button 5 times or tapped "Retry" mid-load.
- **Step 2 — Observe:** Multiple coroutines launched concurrently for the same task ID; obsolete loading requests completed after newer requests and overwrote UI state.
- **Step 3 — Isolate:** `TaskListViewModel` lacked synchronous in-progress guards (`inProgressTaskIds`) and obsolete job cancellation (`fetchJob?.cancel()`), while `ApiTaskRepository` swallowed `CancellationException` in generic catch blocks.
- **Step 4 — Verify:** Added synchronous in-progress set guards with `finally` cleanup, added `fetchJob?.cancel()` in `loadTasks()`, re-threw `CancellationException` in repository catch blocks, and refactored `TaskDetailsActivity` with `TaskDetailsViewModel`. Verified via 36 unit tests including `completeTask_duplicateTaps_executesOnlyOnce` and `loadTasks_obsoleteJobCancelled_onlyLatestResponseUpdatesUi`. Result: **36 PASSED, 0 FAILED**.
