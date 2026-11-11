# Employee Task Manager — Requirements & Specification

## Business Request
“Employees must view tasks from a remote REST API, add new tasks, mark tasks complete, and experience reliable loading, empty, and error feedback during network operations.”

---

## Scope & Timeline Overview
- **Day 1–4 (Completed Baseline):** In-memory Task List screen with XML layouts, AppCompat Activity, ListAdapter, ViewModel, and unit test suite.
- **Day 5 (Completed):** AI Agent setup, rules, handoff context, and code risk review.
- **Day 6 (Current — Planning Day Only):** Plan MVVM + REST API integration. Write specifications, API contract (`docs/API_CONTRACT.md`), architecture sequence diagrams (`docs/ARCHITECTURE.md`), decisions (`docs/DECISIONS.md`), and gain trainer approval. **No feature code written on Day 6.**
- **Day 7 (Upcoming Execution):** Implement REST API task retrieval, Retrofit/Ktor integration or Mock API client, and handle 4 mandatory UI states (`Loading`, `Content/Success`, `Empty`, `Error`).

---

## User Stories

1. **View Remote Tasks:** As an employee, I can view my current tasks fetched from a remote REST API so I have up-to-date work items.
2. **Loading Feedback:** As an employee, I see a clear loading indicator while task data is being fetched from the server so I know the app is responsive.
3. **Empty State Awareness:** As an employee, I see a dedicated empty state illustration and prompt when the server returns no tasks (`[]`) so I know there is no pending work.
4. **Network & Server Error Handling:** As an employee, I see an understandable error message and a "Retry" button when network connection fails or the server returns an error code (4xx/5xx).
5. **Add Task:** As an employee, I can add a task with a trimmed non-empty title and priority so new work is posted to the repository.
6. **One-Way Task Completion:** As an employee, I can mark an incomplete task complete so its status is updated remotely and displayed as completed.

---

## Mandatory 4 UI States

All API-backed screens must strictly support and handle the following 4 UI states:

### 1. `Loading`
- **Condition:** Initial screen load, pull-to-refresh, or user-initiated manual retry.
- **UI Presentation:** Displays a central `ProgressBar` / skeleton loader. Disables user input buttons to prevent duplicate network requests.

### 2. `Content / Success`
- **Condition:** API returns HTTP 200 OK with a non-empty list of tasks.
- **UI Presentation:** Displays `RecyclerView` with `TaskAdapter`, stat summary cards (Total, Completed, Pending), priority badges, and FAB add button.

### 3. `Empty`
- **Condition:** API returns HTTP 200 OK with an empty task array `[]`.
- **UI Presentation:** Hides task list; displays Empty State card with title ("No Tasks Assigned"), subtitle ("You're all caught up!"), and an "Add Task" button.

### 4. `Error`
- **Condition:** API returns HTTP 4xx/5xx error status, or network failure occurs (`IOException`, `SocketTimeoutException`, `UnknownHostException`).
- **UI Presentation:** Hides task list; displays an Error card with a clear message ("Network connection failed. Please check your internet connection and try again.") and an actionable "Retry" button.

---

## Acceptance Criteria

- **API Task Retrieval:** `TaskListViewModel` fetches tasks via `TaskRepository` on launch using coroutine `viewModelScope` on `Dispatchers.IO`.
- **4 UI States Rendering:** `MainActivity` collects `TaskListUiState` within `repeatOnLifecycle(Lifecycle.State.STARTED)` and renders exactly one primary UI state container (`Loading`, `Content`, `Empty`, or `Error`) without UI overlap.
- **Retry Mechanism:** Tapping "Retry" on the `Error` view re-triggers task retrieval without crashing or requiring app restart.
- **Title Input Validation:** Blank or whitespace-only titles are rejected with visible error feedback before making an API call. Surrounding whitespace is trimmed.
- **One-Way Completion:** Completing a task marks it as completed (`completed = true`). Repeated taps on a completed task leave it completed.
- **State Retention across Rotation:** ViewModel retains screen state (`TaskListUiState`) across configuration changes (e.g., screen rotation).

---

## Edge Cases & Error Behaviors

| Scenario | Expected Behavior |
|---|---|
| Network connection offline on app launch | Displays `Error` state with "Network connection unavailable" message and "Retry" button. |
| Server returns HTTP 500 Internal Error | Displays `Error` state with "Server error encountered" message and "Retry" button. |
| Server returns empty task array `[]` | Displays `Empty` state card. |
| User taps "Retry" after fixing connection | Transitions from `Error` → `Loading` → `Content/Success`. |
| Rapid repeated taps on "Add" or "Retry" | UI disables action button during `Loading` to prevent duplicate network requests. |
| Malformed JSON or missing required field | Handled by repository error mapping; does not crash app; emits `Error` state. |

---

## Trainer Review & Approval Status
- **Day 4 Scope:** Approved & verified (see `docs/DECISIONS.md`).
- **Day 6 Planning Scope:** API contract (`docs/API_CONTRACT.md`), architecture sequence diagrams (`docs/ARCHITECTURE.md`), user stories, and 4 UI state specs completed for trainer sign-off.
