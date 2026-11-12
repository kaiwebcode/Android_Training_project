# Decisions and Assumptions

## Approved Decisions (Day 1–4 Baseline)

| Topic | Decision | Status | Rationale / Date |
|---|---|---|---|
| First feature scope | Task List screen only | Approved | Focus scope on single screen per Day 4 plan (Nov 2026) |
| Task fields | In-memory ID (String), required title (String), completion status (Boolean) | Approved | Simple data model matching requirements |
| Title validation | Trim title; reject blank or whitespace-only input with visible error message | Approved | Prevents invalid task creation with clear UI feedback |
| Completion | One-way completion (incomplete -> complete) | Approved | Simple completion lifecycle for first iteration |
| Storage | In-memory repository; no persistence/backend | Approved | Keep storage in-memory for Day 4 scope |
| State ownership | ViewModel owns UI state (`TaskListUiState`) and handles UI events | Approved | Follows MVVM architecture with unidirectional data flow |
| UI toolkit | XML Views + RecyclerView + ViewModel (Native AppCompat baseline) | Approved | Preserves existing app architecture without unapproved UI framework migration |
| Completed tasks | Keep visible in main list with completed badge/strikethrough | Approved | Simple unified list UI showing full context |

---

## Day 6 & Day 7 Architecture & REST API Integration Decisions

| Topic | Decision | Status | Rationale / Date |
|---|---|---|---|
| REST API Contract | Defined GET /tasks, POST /tasks, PUT /tasks/{id}/complete in `docs/API_CONTRACT.md` and integrated endpoint `https://gorest.co.in/public/v3/tasks` | Approved | Establishes clear JSON schema, status codes (200, 400, 500), and error payloads |
| Mandatory UI States | Sealed interface `TaskListUiState` supporting 4 mandatory states (`Loading`, `Content/Success`, `Empty`, `Error`) | Approved | Ensures complete user feedback for all network lifecycle outcomes |
| Network Architecture | `TaskApiService` -> `ApiTaskRepository` -> `TaskListViewModel` -> `MainActivity` | Approved | Enforces MVVM unidirectional data flow and clean repository abstraction |
| Data Layer Mapping | Repository maps network `TaskDto` to domain `Task` model and wraps in `NetworkResult<T>` | Approved | Decouples UI & domain logic from API response structures |
| Threading & Concurrency | `viewModelScope` with `Dispatchers.IO` for background network requests and test dispatcher injection | Approved | Offloads network I/O from Main UI thread safely and allows fast deterministic testing |
| Exception Mapping | Human-readable error messages for `IOException`, `SocketTimeoutException`, `UnknownHostException`, `HttpException` | Approved | Prevents technical stack trace leaks to user interface |
| Retry Mechanism | Actionable "Retry" button on `Error` UI state to re-trigger API call | Approved | Restores application functionality cleanly after network drop |

---

## Session Notes & Trainer Sign-off Record
- **Day 5:** Agent setup, context preservation, handoff lab complete. Codex code risk review recorded in `docs/CODEX_REVIEW.md`.
- **Day 6:** Planning Day deliverables completed (`docs/REQUIREMENTS.md`, `docs/API_CONTRACT.md`, `docs/DEVELOPMENT_PLAN.md`, `docs/ARCHITECTURE.md`, `docs/CODEX_REVIEW.md`).
- **Day 7:** REST API Integration Complete:
  - Retrofit + Gson + OkHttp network client implemented.
  - `TaskDto` -> `Task` mapping implemented.
  - `ApiTaskRepository` with error mapping & dispatcher injection implemented.
  - Sealed `TaskListUiState` modeling `Loading`, `Content`, `Empty`, `Error` implemented.
  - `MainActivity` rendering all 4 UI states with retry action implemented.
  - Unit tests updated and 9/9 tests passed. Build verified.
