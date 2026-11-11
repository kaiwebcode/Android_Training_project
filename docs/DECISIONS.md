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

## Day 6 Planning Decisions (Design & REST API Architecture)

| Topic | Decision | Status | Rationale / Date |
|---|---|---|---|
| Day 6 Scope | Planning & Specifications Only (No Feature Code) | Approved | Enforce Day 6 design rules per PPT slide 7 & AGENTS.md |
| REST API Contract | Defined GET /tasks, POST /tasks, PUT /tasks/{id}/complete in `docs/API_CONTRACT.md` | Pending Trainer Sign-off | Establishes clear JSON schema, status codes (200, 400, 500), and error payloads |
| Mandatory UI States | Sealed class `TaskListUiState` supporting 4 mandatory states (`Loading`, `Content/Success`, `Empty`, `Error`) | Pending Trainer Sign-off | Ensures complete user feedback for all network lifecycle outcomes |
| Network Architecture | `TaskApiService` -> `ApiTaskRepository` -> `TaskListViewModel` -> `MainActivity` | Pending Trainer Sign-off | Enforces MVVM unidirectional data flow and clean repository abstraction |
| Data Layer Mapping | Repository maps network `TaskDto` to domain `Task` model and wraps in `Result<T>` | Pending Trainer Sign-off | Decouples UI & domain logic from API response structures |
| Threading & Concurrency | `viewModelScope` with `Dispatchers.IO` for background network requests | Pending Trainer Sign-off | Offloads network I/O from Main UI thread safely |
| Retry Mechanism | Actionable "Retry" button on `Error` UI state to re-trigger API call | Pending Trainer Sign-off | Restores application functionality cleanly after network drop |

---

## Session Notes & Trainer Sign-off Record
- **Day 5:** Agent setup, context preservation, handoff lab complete. Codex code risk review recorded in `docs/CODEX_REVIEW.md`.
- **Day 6:** Planning Day deliverables completed:
  - `docs/REQUIREMENTS.md` updated with API user stories and 4 UI states.
  - `docs/API_CONTRACT.md` created with full JSON schema and HTTP status codes.
  - `docs/DEVELOPMENT_PLAN.md` updated with Day 6 design & Day 7 execution sequence.
  - `docs/ARCHITECTURE.md` updated with Mermaid sequence and flow diagrams.
  - `docs/diagrams/TASK_LIST_ARCHITECTURE.md` updated with Mermaid diagrams.
  - Day 6 planning artifacts ready for **Trainer Approval** before writing feature code on Day 7.
