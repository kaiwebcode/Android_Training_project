# Proposed Pull Request Draft: Week 2 Complete — REST API Integration, Defect Debugging & Testing (Employee Task Manager)

## PR Title
`feat(task-manager): Week 2 Complete — REST API Integration, Defect Debugging & 25 Unit Tests`

## Description
This pull request completes all Week 2 requirements for the **Employee Task Manager** project (Android Bootcamp). It transitions the application from an in-memory task manager to a robust REST API-backed client featuring MVVM architecture, Unidirectional Data Flow (UDF), `StateFlow`, Retrofit/OkHttp networking, comprehensive error handling across the 4 mandatory UI states (`Loading`, `Content`, `Empty`, `Error`), 4-step defect debugging resolution, 25 passing unit tests, and successfully verified Debug & Release APK builds.

---

## Key Features & Architecture
- **Architecture:** MVVM + Unidirectional Data Flow (UDF) powered by Kotlin Coroutines and `StateFlow` collected safely via `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- **Data Layer:** `ApiTaskRepository` implementing `TaskRepository`, integrating Retrofit (`TaskApiService`) with `Dispatchers.IO`, clean DTO mapping (`TaskDto.toDomainTask()`), and local fallback caching.
- **4 Mandatory UI States:** Fully implemented and verified (`TaskListUiState.Loading`, `TaskListUiState.Content`, `TaskListUiState.Empty`, `TaskListUiState.Error`).
- **Defect Resolution (Day 8):** Robust handling for malformed JSON (`JsonSyntaxException`), network timeouts (`SocketTimeoutException`), HTTP server errors (500), and UI state sync refreshing.
- **Testing (Day 9):** Expanded unit test suite to **25 passing unit tests** (`TaskListViewModelTest`: 12, `ApiTaskRepositoryTest`: 13).
- **Delivery & APKs:**
  - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
  - Release APK: `app/build/outputs/apk/release/app-release.apk` (temporarily signed with debug key for local training; release optimization was disabled as a workaround after `IllegalAccessError` appeared in the optimized release build and was no longer observed after the configuration change. The exact root cause has not been proven).

---

## Verification & Test Evidence
- **Unit Tests (`./gradlew :app:testDebugUnitTest`):** **25 PASSED, 0 FAILED**.
- **Debug Build (`./gradlew :app:assembleDebug`):** **BUILD SUCCESSFUL**.
- **Release Build (`./gradlew :app:assembleRelease`):** **BUILD SUCCESSFUL**.
- **Device Installation & Launch:** User manually confirmed successful installation and launch of the release APK on a physical Android phone; this was not automated ADB verification.

---

## Suggested PR Description & Checklist
- [x] MVVM + UDF + StateFlow architecture preserved.
- [x] Retrofit REST API integration complete.
- [x] 4 UI states handled and tested.
- [x] Day 8 defects resolved and logged in `docs/DEBUGGING_LOG.md`.
- [x] 25 unit tests passing.
- [x] Debug and Release APKs built and verified.
- [x] Release crash workaround applied: optimization disabled after the crash was observed with optimization enabled; the suspected cause is not established.
- [x] APK installation and launch confirmed by user.
