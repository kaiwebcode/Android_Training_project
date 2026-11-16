# Day 10 Evidence — Room Offline Persistence & SSOT Verification

**Date:** 15 November 2026  
**Project:** Employee Task Manager  
**Bootcamp:** Week 3 Day 10 — Room Offline Persistence  
**Branch:** `feature/offline-room`  

---

## 1. Environment & Build Verification

- **Gradle Version:** 9.6.0
- **Android Gradle Plugin (AGP):** 9.5.1
- **Room Version:** 2.8.5
- **KSP Version:** 2.0.21-1.0.27
- **Schema Location:** `app/schemas/com.example.a1weektaskproject.data.local.AppDatabase/1.json`

### Build Commands Executed
```bash
.\gradlew.bat assembleDebug
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat testDebugUnitTest
```

### Build Results
- `:app:assembleDebug`: **BUILD SUCCESSFUL**
- `:app:assembleDebugAndroidTest`: **BUILD SUCCESSFUL**
- `:app:testDebugUnitTest`: **27 PASSED, 0 SKIPPED, 0 FAILED**

---

## 2. Unit & Integration Test Summary (27 Passing Tests)

### `TaskListViewModelTest` (12 Tests Passed)
1. `loadTasks_emitsContentStateOnSuccess` — PASSED
2. `loadTasks_emitsEmptyStateWhenNoTasksReturned` — PASSED
3. `loadTasks_emitsErrorStateOnNetworkFailure` — PASSED
4. `retryFetch_reloadsTasksAfterError` — PASSED
5. `addValidTask_updatesTasksAndCounts` — PASSED
6. `completeTask_updatesTaskCompletionAndCelebrationState` — PASSED
7. `addTask_withBlankTitle_emitsValidationError` — PASSED
8. `addTask_withHighAndLowPriority_preservesPrioritySetting` — PASSED
9. `toggleComplete_togglesTaskCompletionState` — PASSED
10. `dismissCompletionDialog_clearsNewlyCompletedTask` — PASSED
11. `clearValidationError_clearsValidationError` — PASSED
12. `setDemoScenario_reloadsTasksWithNewScenario` — PASSED

### `ApiTaskRepositoryTest` (14 Tests Passed)
1. `getTasks_returnsSuccessWhenApiCallSucceeds` — PASSED
2. `getTasks_returnsEmptyListWhenApiDataIsEmpty` — PASSED
3. `getTasks_returnsErrorOnMalformedJson` — PASSED
4. `getTasks_returnsErrorOnNetworkTimeout` — PASSED
5. `getTasks_returnsErrorOnNetworkIOException` — PASSED
6. `getTasks_returnsErrorOnHttp400BadRequest` — PASSED
7. `getTasks_returnsErrorOnHttp404NotFound` — PASSED
8. `getTasks_returnsErrorOnHttp500ServerError` — PASSED
9. `addTask_addsTaskLocallyAndCallsApi` — PASSED
10. `completeTask_updatesLocalAndCallsApi` — PASSED
11. `toggleComplete_togglesStatusAndHandlesMissingId` — PASSED
12. `getTaskById_returnsCorrectTaskOrNull` — PASSED
13. `getTasks_returnsCachedTasksOnNetworkException` — PASSED
14. `demoScenarios_doNotCorruptInMemoryData` — PASSED

### `ExampleUnitTest` (1 Test Passed)
1. `addition_isCorrect` — PASSED

---

## 3. Room Instrumentation Test Suite (`TaskDaoTest`)

Executed on connected **OnePlus Nord CE (EB2101, Android 12)** via `.\gradlew.bat connectedDebugAndroidTest`.

### Command Execution
```bash
.\gradlew.bat connectedDebugAndroidTest
```

### Result: 6 PASSED, 0 FAILED (100% PASS)
Device ID: `adb-13f6f5c0-EgDxpR._adb-tls-connect._tcp`

1. `ExampleInstrumentedTest.useAppContext` — **PASSED**
2. `TaskDaoTest.insertAndGetById` — **PASSED**
3. `TaskDaoTest.updateTask` — **PASSED**
4. `TaskDaoTest.observeTasksFlow` — **PASSED**
5. `TaskDaoTest.deleteTaskById` — **PASSED**
6. `TaskDaoTest.persistenceAcrossReopeningDatabase` — **PASSED**


---

## 4. Manual Device Verification Protocol (Tests A–F)

When a physical Android device or emulator is connected via ADB, run the following verification protocol:

### Test A — Online Persistence
1. Launch app on device connected to Wi-Fi/Cellular.
2. Verify GET `/public/v3/tasks` loads remote tasks into Room DB.
3. Force stop app via ADB: `adb shell am force-stop com.example.a1weektaskproject`.
4. Re-open app -> verify cached tasks load from Room immediately.

### Test B — Offline Persistence
1. Enable Airplane mode on device.
2. Force stop app: `adb shell am force-stop com.example.a1weektaskproject`.
3. Relaunch app -> verify task list displays cached content from Room without network access.

### Test C — Local Creation
1. While offline in Airplane mode, tap FAB and create task `"Week 3 Offline Test"`.
2. Verify task appears immediately in `Content` state with pending status.
3. Restart app via `adb shell am force-stop com.example.a1weektaskproject`.
4. Confirm task remains present with `syncState = PENDING`.

### Test D — Completion Persistence
1. Complete task `"Week 3 Offline Test"`.
2. Force stop and relaunch app.
3. Verify completion checkmark and badge remain saved in Room.

### Test E — Safe Remote Refresh
1. Disable Airplane mode / reconnect internet.
2. Pull to refresh or trigger `loadTasks()`.
3. Verify remote refresh fetches server tasks and merges without overwriting local task `"Week 3 Offline Test"` (`syncState == PENDING`).

### Test F — Demo Mode Isolation
1. Open Manager Demo Scenario Switcher.
2. Select `HTTP 500` or `Malformed JSON` scenario.
3. Verify error card displays as expected for demo demonstration without clearing or corrupting real Room database entities.

---

## 5. Logcat & Database Verification Output
- Database File Name: `task_database.db`
- Room Version: `1`
- Schema Hash: `c2b5e0d3adc8466767ecbd605406113a`
