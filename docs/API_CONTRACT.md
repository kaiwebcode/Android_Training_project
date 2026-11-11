# REST API Contract — Employee Task Manager

## Overview
This document specifies the REST API contract for the **Employee Task Manager** application. It serves as the formal specification for Day 6 planning and Day 7 API integration.

- **Base URL:** `https://api.employeetasks.internal/v1` (Mock Endpoint: `https://mock.api/v1`)
- **Protocol:** HTTPS
- **Content-Type:** `application/json`

---

## 1. Endpoints

### 1.1 `GET /tasks`
Retrieves the list of employee tasks assigned to the current user.

#### Request
- **Method:** `GET`
- **Path:** `/tasks`
- **Headers:**
  - `Accept: application/json`
  - `Authorization: Bearer <token>` *(Optional / Future)*

#### Response — 200 OK (Content / Success)
Returned when tasks are successfully retrieved and the list is non-empty.

```json
[
  {
    "id": "TASK-101",
    "title": "Review Q4 design documents",
    "priority": "HIGH",
    "completed": false,
    "createdAt": "2026-11-10T09:00:00Z"
  },
  {
    "id": "TASK-102",
    "title": "Submit weekly status report",
    "priority": "MEDIUM",
    "completed": true,
    "createdAt": "2026-11-10T10:30:00Z"
  },
  {
    "id": "TASK-103",
    "title": "Update client onboard documentation",
    "priority": "LOW",
    "completed": false,
    "createdAt": "2026-11-10T11:15:00Z"
  }
]
```

#### Response — 200 OK (Empty List)
Returned when no tasks exist for the employee.

```json
[]
```

#### Response — 400 Bad Request
Returned when request parameters are malformed.

```json
{
  "errorCode": "BAD_REQUEST",
  "message": "Invalid query parameters or request headers."
}
```

#### Response — 500 Internal Server Error
Returned when the backend database or service encounters an internal error.

```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "Internal database processing failure."
}
```

#### Network Exceptions (Offline / Timeout)
- **IOException / SocketTimeoutException / UnknownHostException**: No JSON payload returned. Handled in Repository layer and mapped to `Result.Error(NetworkException)` triggering the `Error` UI state.

---

### 1.2 `POST /tasks`
Creates a new task.

#### Request
- **Method:** `POST`
- **Path:** `/tasks`
- **Headers:** `Content-Type: application/json`
- **Body:**

```json
{
  "title": "Fix login screen edge case bug",
  "priority": "HIGH"
}
```

#### Response — 201 Created
```json
{
  "id": "TASK-104",
  "title": "Fix login screen edge case bug",
  "priority": "HIGH",
  "completed": false,
  "createdAt": "2026-11-10T14:00:00Z"
}
```

#### Response — 400 Bad Request (Validation Error)
```json
{
  "errorCode": "VALIDATION_FAILED",
  "message": "Task title cannot be blank."
}
```

---

### 1.3 `PUT /tasks/{id}/complete`
Marks a specific task as completed (one-way completion).

#### Request
- **Method:** `PUT`
- **Path:** `/tasks/{id}/complete`

#### Response — 200 OK
```json
{
  "id": "TASK-101",
  "title": "Review Q4 design documents",
  "priority": "HIGH",
  "completed": true,
  "createdAt": "2026-11-10T09:00:00Z"
}
```

#### Response — 404 Not Found
```json
{
  "errorCode": "TASK_NOT_FOUND",
  "message": "Task with ID TASK-999 was not found."
}
```

---

## 2. DTO & Data Schema Definitions

### TaskDto
| Field | Type | Description | Required |
|---|---|---|---|
| `id` | String | Unique task identifier | Yes |
| `title` | String | Description/title of the task | Yes |
| `priority` | String | Enum: `"HIGH"`, `"MEDIUM"`, `"LOW"` | Yes |
| `completed` | Boolean | Completion status | Yes |
| `createdAt` | String | ISO-8601 timestamp | Yes |

### ErrorResponseBody
| Field | Type | Description |
|---|---|---|
| `errorCode` | String | Machine-readable error code |
| `message` | String | Human-readable error message |

---

## 3. UI State Mapping (4 Mandatory UI States)

| UI State | Trigger Condition | Screen Manifestation / User Experience |
|---|---|---|
| `Loading` | API request initiated (Initial fetch or Refresh) | Displays `ProgressBar` / Shimmer skeleton loader. Action buttons disabled. |
| `Content / Success` | HTTP 200 OK with non-empty list `[TaskDto...]` | Displays `RecyclerView` list, priority badges, stat counter cards (Total, Completed, Pending). |
| `Empty` | HTTP 200 OK with empty array `[]` | Hides list; displays Empty State illustration, message ("No tasks assigned"), and "Add Task" action prompt. |
| `Error` | HTTP 4xx/5xx or `IOException` (Timeout / Offline) | Hides list; displays Error banner with user-friendly message and a "Retry" button to re-trigger fetch. |

---

## 4. Error Handling Pipeline & Code Integration Strategy

```
[REST API]
   │
   ├──> 200 OK (List > 0) ──> ApiTaskRepository ──> TaskListUiState.Content(tasks)
   ├──> 200 OK (List == 0) ─> ApiTaskRepository ──> TaskListUiState.Empty
   ├──> 4xx / 5xx HTTP ─────> ApiTaskRepository ──> TaskListUiState.Error(message)
   └──> Network Failure ────> ApiTaskRepository ──> TaskListUiState.Error("Network offline")
```

1. **Retrofit Interface (`TaskApiService`):** Returns `Response<List<TaskDto>>` or directly suspends.
2. **Repository (`ApiTaskRepository`):**
   - Executes API calls on `Dispatchers.IO`.
   - Maps DTO (`TaskDto`) to domain model (`Task`).
   - Wraps result in sealed `Result<List<Task>>`.
3. **ViewModel (`TaskListViewModel`):**
   - Manages state via `StateFlow<TaskListUiState>`.
   - Translates domain `Result` into one of the 4 mandatory UI states.
4. **View (`MainActivity`):**
   - Collects `TaskListUiState` using `repeatOnLifecycle(Lifecycle.State.STARTED)`.
   - Selectively renders Loading view, RecyclerView, Empty view, or Error view.
