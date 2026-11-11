# Codex Review — Day 6 Plan Review & Risk Analysis

## Review Scope
Independent static review of the Day 6 planning deliverables (`docs/REQUIREMENTS.md`, `docs/API_CONTRACT.md`, `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT_PLAN.md`, `docs/DECISIONS.md`) against current Kotlin implementation (`TaskListViewModel.kt`, `TaskListUiState.kt`, `MainActivity.kt`, `InMemoryTaskRepository.kt`).

---

## Challenged Assumptions & Architectural Risks (Day 6 Planning Exit Gate)

### 1. Risk: Flat Data Class vs. Sealed Hierarchy for 4 Mandatory UI States
- **Finding:** `TaskListUiState` is currently a single flat `data class` containing boolean flags (`isEmptyStateVisible`) and nullable fields (`errorMessage: String?`).
- **Challenge:** Flat state objects allow invalid state combinations during network operations (e.g. `isLoading = true`, `errorMessage != null`, and `tasks` non-empty at the same time).
- **Recommendation:** Refactor `TaskListUiState` into a type-safe `sealed interface` for Day 7:
  ```kotlin
  sealed interface TaskListUiState {
      data object Loading : TaskListUiState
      data class Content(
          val tasks: List<Task>,
          val totalCount: Int,
          val completedCount: Int,
          val pendingCount: Int
      ) : TaskListUiState
      data object Empty : TaskListUiState
      data class Error(val message: String) : TaskListUiState
  }
  ```

### 2. Risk: One-Shot Event Recollection Across Lifecycle Recreation
- **Finding:** In `MainActivity.kt`, `viewModel.uiState` is collected inside `repeatOnLifecycle(Lifecycle.State.STARTED)`.
- **Challenge:** When `errorMessage` or `newlyCompletedTask` is held in state, rotating the screen or navigating away and returning causes `repeatOnLifecycle` to re-collect the last emitted state, repeatedly popping up dialogs or error banners.
- **Recommendation:** Separate persistent screen layout state (`Loading`, `Content`, `Empty`, `Error`) from one-shot transient events (e.g. Completion Celebration Dialog, Validation Toast) using a `Channel` / `SharedFlow` or explicit event consumption logic (`clearErrorMessage()`, `dismissCompletionDialog()`).

### 3. Risk: Direct Network Exception Leakage to UI Layer
- **Finding:** Network calls via OkHttp/Retrofit can throw low-level system exceptions (`java.net.UnknownHostException`, `java.net.SocketTimeoutException`, `retrofit2.HttpException`).
- **Challenge:** Passing exception messages directly to the UI will display unreadable technical text (e.g., `Unable to resolve host "api.employeetasks.internal"`).
- **Recommendation:** `ApiTaskRepository` must catch all `Throwable` instances and map them to human-readable domain error messages:
  - `UnknownHostException` / `IOException` ➔ *"No internet connection. Please check your network and try again."*
  - `SocketTimeoutException` ➔ *"Server connection timed out. Please try again."*
  - `HttpException (500)` ➔ *"Server encountered an internal error. Please try again later."*

### 4. Risk: Unit Test Flakiness with Hardcoded `Dispatchers.IO`
- **Finding:** `ApiTaskRepository` will run network I/O operations on `Dispatchers.IO`.
- **Challenge:** Hardcoding `Dispatchers.IO` inside the repository makes unit testing asynchronous coroutines difficult, leading to test race conditions or skipped assertions in `TaskListViewModelTest`.
- **Recommendation:** Inject `CoroutineDispatcher` into `ApiTaskRepository` (defaulting to `Dispatchers.IO` in production) so unit tests can supply `UnconfinedTestDispatcher()` or `StandardTestDispatcher()`.

### 5. Risk: Missing Local Cache / Offline Retain Strategy
- **Finding:** `docs/API_CONTRACT.md` describes direct network retrieval without local caching.
- **Challenge:** If a user loses connectivity after initially loading tasks, triggering a pull-to-refresh will immediately replace the visible tasks with an `Error` state.
- **Recommendation:** For Day 7, include a graceful fallback: if network fails on refresh, maintain the existing loaded tasks and present an inline error banner/snackbar rather than wiping out the list view completely.

---

## Summary & Review Status
- **Review Requirement Met:** 5 concrete architecture risks and assumptions challenged for Day 6 exit gate.
- **Action Items for Day 7:** Incorporate `sealed interface TaskListUiState`, exception mapping in `ApiTaskRepository`, dispatcher injection for testability, and one-shot event separation during Day 7 REST API integration.
