# Employee Task Manager — Architecture & Data Flow Specification

## 1. System Overview
The Employee Task Manager app follows Android's recommended Unidirectional Data Flow (UDF) pattern within Model-View-ViewModel (MVVM) architecture.

```
┌───────────────────────────┐
│     UI Layer (View)       │  renders TaskListUiState
│       MainActivity        │ ◄──────────────────────┐
└─────────────┬─────────────┘                        │
              │ user actions                         │ StateFlow<TaskListUiState>
              ▼                                      │
┌───────────────────────────┐                        │
│   ViewModel (State Owner) │                        │
│     TaskListViewModel     │────────────────────────┘
└─────────────┬─────────────┘
              │ coroutines (Dispatchers.IO)
              ▼
┌───────────────────────────┐
│     Repository Layer      │  maps TaskDto -> Task
│     ApiTaskRepository     │
└─────────────┬─────────────┘
              │ Retrofit / OkHttp
              ▼
┌───────────────────────────┐
│      Network / API        │  GET /tasks
│  TaskApiService (REST API) │
└───────────────────────────┘
```

---

## 2. High-Level System Flowchart

~~~mermaid
flowchart LR
    U[Employee] -->|Interact / Tap Retry| S[MainActivity / UI]
    S -->|User Intents| V[TaskListViewModel]
    V -->|viewModelScope / Dispatchers.IO| R[ApiTaskRepository]
    R -->|HTTPS GET /tasks| API[TaskApiService / REST API]
    API -->|JSON Response / HTTP Status| R
    R -->|Result.Success / Result.Error| V
    V -->|StateFlow TaskListUiState| S
~~~

---

## 3. End-to-End Sequence Diagram (Tracing 4 UI States)

~~~mermaid
sequenceDiagram
    autonumber
    actor Employee as Employee / UI (MainActivity)
    participant VM as TaskListViewModel
    participant Repo as ApiTaskRepository
    participant API as TaskApiService (REST API)

    Employee->>VM: Screen Launch / Retry Tap / Refresh
    VM->>VM: Emit TaskListUiState.Loading
    VM-->>Employee: Render Loading View (ProgressBar)
    
    VM->>Repo: getTasks() [Dispatchers.IO]
    Repo->>API: GET /tasks
    
    alt Case 1: 200 OK (Non-Empty Task List)
        API-->>Repo: 200 OK [TaskDto...]
        Repo->>Repo: Map TaskDto items -> Domain Task models
        Repo-->>VM: Result.Success(List<Task>)
        VM->>VM: Emit TaskListUiState.Content(tasks, stats)
        VM-->>Employee: Render Task RecyclerView & Summary Cards
        
    else Case 2: 200 OK (Empty Array [])
        API-->>Repo: 200 OK []
        Repo-->>VM: Result.Success(emptyList())
        VM->>VM: Emit TaskListUiState.Empty
        VM-->>Employee: Render Empty State View ("No tasks assigned")
        
    else Case 3: HTTP Error (4xx / 5xx) or Network Exception (Offline/Timeout)
        API-->>Repo: 500 Server Error / SocketTimeoutException / UnknownHostException
        Repo->>Repo: Catch Exception & Map to UserFriendly Error
        Repo-->>VM: Result.Error(Exception)
        VM->>VM: Emit TaskListUiState.Error(errorMessage)
        VM-->>Employee: Render Error View Banner & "Retry" Button
    end
~~~

---

## 4. Components & Responsibilities

### 4.1 UI Layer (`MainActivity`)
- Displays screen layout (`activity_main.xml`).
- Observes `TaskListViewModel.uiState` within `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }`.
- Swaps layout visibility between 4 distinct containers:
  - `loadingLayout` (ProgressBar)
  - `contentLayout` (RecyclerView + Stat Cards)
  - `emptyLayout` (Empty illustration + text)
  - `errorLayout` (Error banner + Retry button)
- Forwards user click events (Add Task, Complete Task, Retry) to ViewModel.

### 4.2 ViewModel (`TaskListViewModel`)
- Retains screen state across configuration changes (e.g. rotation).
- Exposes `val uiState: StateFlow<TaskListUiState>`.
- Executes background data fetching using `viewModelScope.launch(Dispatchers.IO)`.
- Validates task creation inputs (trims titles, checks non-empty).
- Translates Repository `Result` domain models into explicit `TaskListUiState` instances.

### 4.3 Domain State (`TaskListUiState`)
Represented as a sealed interface/class hierarchy:
- `TaskListUiState.Loading`: Indicates an active fetch is in progress.
- `TaskListUiState.Content(val tasks: List<Task>, val totalCount: Int, val completedCount: Int, val pendingCount: Int)`: Indicates successful data retrieval with non-empty list.
- `TaskListUiState.Empty`: Indicates successful retrieval with zero tasks.
- `TaskListUiState.Error(val message: String)`: Indicates fetch failure due to network or server issues.

### 4.4 Repository Layer (`ApiTaskRepository`)
- Implements `TaskRepository` interface.
- Acts as single source of truth for task data.
- Handles threading via Kotlin Coroutines (`withContext(Dispatchers.IO)`).
- Converts DTO network models (`TaskDto`) into domain models (`Task`).
- Catches network/parsing exceptions and wraps outcomes in a sealed `Result<T>` class (`Result.Success` or `Result.Error`).

### 4.5 Network Service (`TaskApiService`)
- Retrofit interface defining REST endpoints (`GET /tasks`, `POST /tasks`, `PUT /tasks/{id}/complete`).
- Serializes/deserializes JSON payloads via Moshi or Kotlinx.Serialization.

---

## 5. Threading & Concurrency Architecture

| Layer | Dispatcher / Scope | Strategy |
|---|---|---|
| View (`MainActivity`) | `lifecycleScope` | Lifecycle-aware flow collection (`repeatOnLifecycle(STARTED)`) |
| ViewModel (`TaskListViewModel`) | `viewModelScope` | Manages async state updates; publishes via `StateFlow` |
| Repository (`ApiTaskRepository`) | `Dispatchers.IO` | Offloads blocking I/O and network requests |
| Network (`TaskApiService`) | Suspend functions | Asynchronous network execution managed by OkHttp/Retrofit thread pool |

---

## 6. Status & Approval
Day 6 Architecture & Sequence Specifications complete. Ready for Trainer Review and Approval recorded in `docs/DECISIONS.md`.
