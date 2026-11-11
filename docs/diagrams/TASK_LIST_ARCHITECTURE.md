# Task List Architecture Diagrams (Day 6 Planning)

## High-Level Architecture Flowchart

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

## Detailed Sequence Diagram (Tracing 4 UI States)

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

This architecture and sequence plan is prepared for Day 6 planning approval. Trainer approval is tracked in `docs/DECISIONS.md`. No feature code is executed on Day 6.
