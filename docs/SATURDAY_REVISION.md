# Day 10 - Saturday Revision
Date: 14 November 2026
Project: Employee Task Manager
Bootcamp: Week 2 - Build with AI Agents

## 1. Project Context and AI Handoff

Reviewed AGENTS.md, docs/CURRENT_STATUS.md,
docs/AI_HANDOFF.md, and project architecture.

Confirmed the project uses Kotlin, MVVM, StateFlow,
Retrofit, OkHttp and Gson.

## 2. MVVM Architecture Review

MainActivity displays the application UI.

TaskListViewModel manages loading, content,
empty and error states using StateFlow.

ApiTaskRepository handles task operations,
API responses, data mapping and network errors.

TaskApiService defines the Retrofit endpoints.

Actual task retrieval:
GET https://gorest.co.in/public/v3/tasks?page%5Bsize%5D=10

The actual implementation uses GoRest instead
of the placeholder API from the original plan.

## 3. UI Demonstration

All four required UI states were demonstrated:

- Loading: Progress indicator during task retrieval
- Content: GoRest task list displays successfully
- Empty: No Tasks Assigned message
- Error: Malformed data and timeout demonstrations

Additional flows demonstrated:
Task creation, task completion, counters,
task details and Manager Demo Scenario Switcher.

The defect scenarios are simulated demonstrations.

## 4. Network and Logcat Evidence

Device: OnePlus EB2101

Network Inspector confirmed GET /public/v3/tasks
returned HTTP 200 with application/json.

Logcat confirmed OkHttp GET requests,
successful HTTP responses and JSON data.

## 5. Debugging Review

Reviewed the four previously documented defects:

1. Malformed JSON handling
2. HTTP 500 and timeout handling
3. UI refresh and state synchronization
4. Release APK IllegalAccessError workaround

Additional observation:
A real PUT request to the GoRest task endpoint
returned HTTP 405 Method Not Allowed.

The API response allows PATCH, not PUT.
The repository retains local task completion
even if the remote update fails.

This is an OPEN ISSUE, not a verified fix.

## 6. Tests and Build

Command:
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug

Result: BUILD SUCCESSFUL

TaskListViewModelTest: 12 passed
ApiTaskRepositoryTest: 12 passed
ExampleUnitTest: 1 passed

Total: 25 passed, 0 failures, 0 errors.

## 7. Three Lessons Learned

1. MVVM separates UI, state management and
   data operations, making code easier to maintain.

2. Network Inspector and Logcat help identify
   actual API responses and debugging issues.

3. AI-generated code must be reviewed and tested.
   A successful UI update does not necessarily
   mean an API operation succeeded.

## 8. Open Questions and Follow-up

1. How should authenticated GoRest updates be
   handled for tasks marked read_only?

2. Should failed server writes be reported to
   users instead of silently retaining local data?

3. What caused the release optimization crash?
   The optimization workaround is not a root-cause fix.

4. Should the original API contract documentation
   be updated to reflect the actual GoRest API?

## 9. Revision Outcome

Architecture and API data flow reviewed.
Four UI states demonstrated.
Network and Logcat evidence inspected.
Previously documented defects reviewed.
Unit tests and debug build verified successfully.

Remaining: Save evidence in Git and complete
the Saturday revision commit.
