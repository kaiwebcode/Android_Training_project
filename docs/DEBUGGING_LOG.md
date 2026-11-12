# Debugging Log — Day 8 Defect Resolution Framework

This document tracks defect diagnosis and resolution using the 4-step defect debugging framework:
$$\text{Reproduce} \longrightarrow \text{Observe} \longrightarrow \text{Isolate} \longrightarrow \text{Verify}$$

---

## Defect 1: Malformed API Data & JSON Deserialization Crash

- **Symptom / Description:** When the remote API server returns malformed JSON syntax, HTML error payloads, or unexpected data types, raw JSON deserialization or unhandled parsing exceptions crashed the application or caused runtime errors.
- **Step 1 — Reproduce:** Injected `JsonSyntaxException` inside `TaskApiService` mock response in unit test suite.
- **Step 2 — Observe:** Observed raw `JsonSyntaxException` escaping repository layer, crashing thread context or producing unhandled state errors.
- **Step 3 — Isolate:** Identified unhandled exception path in `ApiTaskRepository.getTasks()`. Updated `mapExceptionToMessage()` to catch `JsonSyntaxException` and mapped DTO values safely with default fallbacks in `TaskDto.toDomainTask()`.
- **Step 4 — Verify:** Added unit test `getTasks_returnsErrorOnMalformedJson()` in `ApiTaskRepositoryTest.kt`. Executed `./gradlew :app:testDebugUnitTest` — test passed, verifying safe emission of `NetworkResult.Error("Server returned invalid or malformed data.")` without crashing.

---

## Defect 2: HTTP 500 Server Error & Network Timeout Exception Handling

- **Symptom / Description:** When backend returns HTTP 500/502/503 internal server errors or when requests time out (`SocketTimeoutException`), screen remained stuck in loading state or threw low-level system exceptions.
- **Step 1 — Reproduce:** Simulated HTTP 500 status code response and `SocketTimeoutException` network timeout during `getTasks()` execution.
- **Step 2 — Observe:** Logcat and network inspector captured `SocketTimeoutException` and HTTP status 500.
- **Step 3 — Isolate:** Updated `ApiTaskRepository.getTasks()` and `mapExceptionToMessage()` to map `SocketTimeoutException` to `"Server connection timed out. Please try again."` and HTTP 500 to `"Server error encountered (500). Please try again later."`. In `MainActivity`, `TaskListUiState.Error` displays the error message and enables the actionable **"Retry"** button to call `viewModel.retryFetch()`.
- **Step 4 — Verify:** Added unit tests `getTasks_returnsErrorOnNetworkTimeout()` and `retryFetch_reloadsTasksAfterError()`. Executed `./gradlew :app:testDebugUnitTest` — tests passed, confirming safe transition to `TaskListUiState.Error` and successful reload upon retry.

---

## Defect 3: UI Not Updating After Refresh / Stale State Sync Issue

- **Symptom / Description:** Completing a task or refreshing the list occasionally failed to update list items or top stat counter cards (Total, Completed, Pending) in the UI.
- **Step 1 — Reproduce:** Executed `completeTask()` / `toggleComplete()` and observed whether `RecyclerView` / stat counter text updated when `ListAdapter` received list references.
- **Step 2 — Observe:** Observed that `areContentsTheSame` in `DiffUtil` or `StateFlow` updates were skipped if identical mutated list references were emitted.
- **Step 3 — Isolate:** Updated `ApiTaskRepository` to always return a new, distinct list copy (`localTasks.toList()`) upon data mutation. Updated `TaskListViewModel` to recalculate `totalCount`, `completedCount`, and `pendingCount` and emit a fresh `TaskListUiState.Content` object reference.
- **Step 4 — Verify:** Added unit test `completeTask_updatesTaskCompletionAndCelebrationState()` verifying state emission, counter recalculation, and `ListAdapter` diffing. Executed `./gradlew :app:testDebugUnitTest` — test passed (12/12 unit tests passing).

---

## Verification Summary
- **Unit Test Suite:** `./gradlew :app:testDebugUnitTest` — **12 PASSED, 0 FAILED**
- **Debug APK Build:** `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL**
