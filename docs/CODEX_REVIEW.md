# Codex Review — Day 5 Agent Setup Exit Gate

## Review scope
Independent static review of the current Week 1 Task List implementation against `docs/REQUIREMENTS.md`, `docs/ARCHITECTURE.md`, and the project conventions. No source code was changed and no build or tests were run for this review.

## Concrete code risks

1. **Completion dialog can be shown more than once for a single completion.** In `TaskListViewModel.refreshTasks()`, `newlyCompletedTask` is retained in the StateFlow until the dialog is dismissed. In `MainActivity.observeUiState()`, every collected state that still contains that value calls `showTaskCompletedDialog()`. Any intervening state emission (including a refresh) can open another dialog while the first is showing. Consider consuming this as a one-shot event or recording that the dialog has already been presented. Relevant code: `app/src/main/java/com/example/a1weektaskproject/ui/TaskListViewModel.kt` (`refreshTasks`, `dismissCompletionDialog`) and `app/src/main/java/com/example/a1weektaskproject/MainActivity.kt` (`observeUiState`, `showTaskCompletedDialog`).

2. **The ViewModel exposes a completion toggle that conflicts with the approved one-way completion decision.** `TaskListViewModel.toggleComplete()` calls `repository.toggleComplete()` and can change a completed task back to incomplete. The current list UI routes taps to `completeTask()`, so this path is not currently exposed there, but it remains callable by other UI or future code and contradicts the one-way rule in `docs/DECISIONS.md`. Remove the unused toggle path or constrain it to the approved behavior. Relevant code: `app/src/main/java/com/example/a1weektaskproject/ui/TaskListViewModel.kt` (`toggleComplete`) and `app/src/main/java/com/example/a1weektaskproject/data/InMemoryTaskRepository.kt` (`toggleComplete`).

3. **Repository snapshots are shallow and its mutable backing list is not synchronized.** `getTasks()` returns `tasksList.toList()`, which copies the collection but not its elements; `Task` is currently a data class and repository updates replace elements, so current ViewModel use is safe in the normal single-threaded flow. However, repository methods mutate a plain `MutableList` without synchronization. Concurrent calls could race or produce inconsistent snapshots if repository work is moved to background dispatchers for the planned API evolution. Keep access on one dispatcher or add an explicit concurrency strategy before introducing asynchronous operations. Relevant code: `app/src/main/java/com/example/a1weektaskproject/data/InMemoryTaskRepository.kt` (`tasksList`, `getTasks`, mutation methods).

## Review status
These are actionable risks from source inspection, not reproduced defects. The first two concern current behavior/design consistency; the third becomes material if repository calls become concurrent. Day 5 review requirement for at least two concrete risks is met by findings 1 and 2. No implementation changes were made.
