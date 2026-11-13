# Debugging Log — Day 8 & Day 9 Defect Resolution Framework

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
