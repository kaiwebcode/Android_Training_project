# AI Handoff & Context Document — Week 2 Transition

## Executive Summary
This document serves as the single source of truth for transferring context between AI agent sessions working on the **Employee Task Manager** Android application for **Week 2: Build with AI Agents**.

---

## 1. Project Baseline & Architecture Summary
- **App Name:** Employee Task Manager (`1weektaskproject`)
- **Package:** `com.example.a1weektaskproject`
- **Current Branch:** `main` / `feature/api-task-list`
- **Tech Stack:** Kotlin 2.1.0, AGP 9.5.0, Android SDK 37 (minSdk 24), Java 25 (JBR), Retrofit 2.11.0, OkHttp 4.12.0, Gson 2.10.1.
- **UI Baseline:** View-based XML layouts (`activity_main.xml`, `activity_task_details.xml`, `item_task.xml`, `bottom_sheet_add_task.xml`, `dialog_task_completed.xml`) with `AppCompatActivity`, `MaterialComponents`, `RecyclerView`, and `ListAdapter`.
- **Architecture:** MVVM (Model-View-ViewModel) with unidirectional data flow.
  - `Task`: Data class representing a task item (ID, title, description, completion status, priority, due date).
  - `TaskDto`: Network DTO with clean `toDomainTask()` mapping.
  - `TaskRepository`: Interface for task data operations.
  - `ApiTaskRepository`: REST API repository implementation with coroutine error handling & exception mapping.
  - `TaskListViewModel`: ViewModel publishing sealed `TaskListUiState` via `StateFlow`.
  - `MainActivity`: Hosts 4 UI state containers (`Loading`, `Content`, `Empty`, `Error`) with actionable Retry button.
  - `TaskDetailsActivity`: Detailed view for inspecting and completing selected tasks.

---

## 2. Verification & Test Status
- **Build Verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- **Unit Tests:** `./gradlew testDebugUnitTest` — **9/9 PASSED**
  - `TaskListViewModelTest`: Tests initial loading, content success state, empty state emission, network failure error state, task addition, and task completion.
  - `ApiTaskRepositoryTest`: Tests API success mapping, network `IOException` mapping, and HTTP 500 error mapping.

---

## 3. Week 2 Roadmap & Status
Week 2 expands the project from in-memory state to a REST API-backed application with full error handling, defect debugging, and comprehensive testing.

- **Day 5 (Mon):** Agent Setup & Handoff Lab (`AGENTS.md`, `.cursor/rules/bootcamp.mdc`, `AI_HANDOFF.md`, `CURRENT_STATUS.md`) — **COMPLETE**.
- **Day 6 (Tue):** Design MVVM + REST API Integration (Planning Only — `REQUIREMENTS.md`, `DEVELOPMENT_PLAN.md`, `ARCHITECTURE.md`, `docs/API_CONTRACT.md`, `docs/CODEX_REVIEW.md`, `docs/DECISIONS.md`) — **COMPLETE**.
- **Day 7 (Wed):** REST API Integration (`feature/api-task-list`, Retrofit/OkHttp REST API client, `ApiTaskRepository`, StateFlow pipeline, 4 UI state handling in `MainActivity`, `TaskDetailsActivity` integration, 9 unit tests) — **COMPLETE**.
- **Day 8 (Thu):** Debug 3 Defects (4-step framework: `Reproduce` → `Observe` → `Isolate` → `Verify` for malformed data, HTTP 500/timeout, UI refresh issues) — **NEXT**.
- **Day 9 (Fri):** Testing & Delivery (ViewModel & Repository unit tests, APK build, Codex diff review, PR submission).
- **Sat (Nov 14):** Revision & Demo.

---

## 4. Instructions for Resuming AI Agents
When starting a new session:
1. Read `AGENTS.md`, `.cursor/rules/bootcamp.mdc`, and `docs/CURRENT_STATUS.md`.
2. Review this document (`docs/AI_HANDOFF.md`), `docs/API_CONTRACT.md`, and `docs/CODEX_REVIEW.md`.
3. On Day 8: Apply 4-step defect debugging framework (`Reproduce` → `Observe` → `Isolate` → `Verify`) to diagnose and fix controlled bugs, logging findings in `docs/DEBUGGING_LOG.md`.
4. Verify every change with `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.
