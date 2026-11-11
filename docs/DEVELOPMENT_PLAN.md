# Employee Task Manager — Development & Architecture Plan (Week 2)

## Overall Objective
Evolve the Employee Task Manager application from an in-memory repository to a robust REST API-backed application featuring clean MVVM architecture, full 4 UI state handling (`Loading`, `Content/Success`, `Empty`, `Error`), thorough defect debugging, and unit testing.

---

## Plan by Day

### Day 5: Agent Setup & Handoff Lab (Completed)
- Established agent workflow guidelines in `AGENTS.md` and `.cursor/rules/bootcamp.mdc`.
- Created `docs/AI_HANDOFF.md` documenting architecture, test baseline, and code risks.
- Verified build (`:app:assembleDebug`) and unit tests (7/7 passed).

### Day 6: Plan MVVM + REST API Integration (Current — Planning Only)
- **Goal:** Design API integration and 4 UI state architecture without writing feature code.
- **Deliverables:**
  1. `docs/REQUIREMENTS.md` updated with API user stories, acceptance criteria, and 4 UI states.
  2. `docs/API_CONTRACT.md` created with GET/POST/PUT specs, JSON schemas, headers, status codes, and error bodies.
  3. `docs/ARCHITECTURE.md` updated with data flow diagrams and Mermaid sequence diagrams (`Screen` → `ViewModel` → `Repository` → `API`).
  4. `docs/DECISIONS.md` updated with Day 6 design decisions.
  5. `docs/CURRENT_STATUS.md` updated and ready for Trainer Approval.

### Day 7: REST API Integration (Upcoming Execution)
- **Goal:** Integrate REST API task retrieval and enforce 4 mandatory UI states.
- **Sequence:**
  1. Create feature branch `feature/api-task-list`.
  2. Define network data transfer models (`TaskDto`) matching `docs/API_CONTRACT.md`.
  3. Create `TaskApiService` interface (Retrofit or Mock API Engine).
  4. Implement `ApiTaskRepository` implementing `TaskRepository` interface with `Result` wrapper and error mapping (`Dispatchers.IO`).
  5. Update `TaskListUiState` to sealed class or data class modeling `Loading`, `Success(List<Task>)`, `Empty`, and `Error(String)`.
  6. Update `TaskListViewModel` to execute fetch on `viewModelScope` and publish state via `StateFlow`.
  7. Update `MainActivity` XML layouts and Kotlin collection using `repeatOnLifecycle` to render all 4 UI states cleanly.
  8. Write unit tests for `TaskListViewModel` and `ApiTaskRepository`.

### Day 8: Debug Three Defects (4-Step Framework)
- **Goal:** Diagnose and fix 3 controlled defects using: `Reproduce` → `Observe` → `Isolate` → `Verify`.
  - Bug 1: Malformed JSON or missing required field crash.
  - Bug 2: HTTP 500 / network timeout exception handling.
  - Bug 3: UI not updating after pull-to-refresh or task status change.

### Day 9: Test & Deliver
- **Goal:** Run full unit test suite, generate debug APK, conduct Codex diff review, write `docs/PULL_REQUEST.md`, and submit PR.

---

## File Structure & Map for API Evolution

```
app/src/main/java/com/example/a1weektaskproject/
├── data/
│   ├── model/
│   │   ├── Task.kt              (Domain Model)
│   │   ├── TaskPriority.kt      (Domain Priority Enum)
│   │   └── TaskDto.kt           (API Data Transfer Object)
│   ├── remote/
│   │   ├── TaskApiService.kt    (Retrofit / REST Client contract)
│   │   └── NetworkResult.kt     (Sealed class for Network Result handling)
│   └── repository/
│       ├── TaskRepository.kt    (Repository Interface)
│       ├── ApiTaskRepository.kt (REST API Repository Implementation)
│       └── InMemoryTaskRepository.kt (Fallback / Baseline Repository)
├── ui/
│   ├── state/
│   │   └── TaskListUiState.kt  (Sealed class modeling Loading, Success, Empty, Error)
│   ├── TaskListViewModel.kt    (ViewModel owning UI State & Coroutine execution)
│   ├── TaskAdapter.kt          (ListAdapter with DiffUtil)
│   └── MainActivity.kt         (View rendering 4 UI states)
```

---

## Verification Plan

1. **Static Analysis & Lint:** `./gradlew lint` or IDE code analysis.
2. **Unit Tests:** `./gradlew testDebugUnitTest` verifying `TaskListViewModelTest` and `ApiTaskRepositoryTest` for:
   - Initial `Loading` state emission.
   - Successful task list fetch (`Content/Success`).
   - Empty task list fetch (`Empty`).
   - Network failure & server error handling (`Error`).
   - Title trimming and validation.
3. **Build Verification:** `./gradlew assembleDebug`.
4. **Manual & UI State Verification:**
   - Verify `Loading` spinner visibility on fetch.
   - Verify `Content` state displaying tasks and stat counters.
   - Verify `Empty` state illustration when no tasks exist.
   - Verify `Error` banner and "Retry" button behavior when network is disabled.

---

## Risk Assessment & Mitigations

| Risk | Mitigation |
|---|---|
| Premature coding before trainer approval | Enforce "Day 6 is planning only" rule in `.cursor/rules/bootcamp.mdc` and `AGENTS.md`. |
| Unhandled network exceptions crashing app | Wrap repository network calls in `runCatching` or try-catch block mapping to `Result.Error`. |
| Threading violations (Network on Main thread) | Explicitly dispatch repository coroutines on `Dispatchers.IO`. |
| Memory leaks / state loss across rotation | State published via `StateFlow` in ViewModel; collected using `repeatOnLifecycle`. |
