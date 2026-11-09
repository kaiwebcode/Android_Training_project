# AI Handoff & Context Document — Week 2 Transition

## Executive Summary
This document serves as the single source of truth for transferring context between AI agent sessions (Cursor, Codex, or IDE agents) working on the **Employee Task Manager** Android application for **Week 2: Build with AI Agents**.

---

## 1. Project Baseline & Week 1 Summary
- **App Name:** Employee Task Manager (`1weektaskproject`)
- **Package:** `com.example.a1weektaskproject`
- **Current Branch:** `main` / `training/agent-context`
- **Tech Stack:** Kotlin 2.1.0, AGP 9.5.0, Android SDK 37 (minSdk 24), Java 25 (JBR).
- **UI Baseline:** View-based XML layouts (`activity_main.xml`, `activity_task_details.xml`, `item_task.xml`, `dialog_add_task_bottom_sheet.xml`, `dialog_task_celebration.xml`) with `AppCompatActivity`, `MaterialComponents`, `RecyclerView`, and `ListAdapter`.
- **Architecture:** MVVM (Model-View-ViewModel) with unidirectional data flow.
  - `Task`: Data class representing a task item (ID, title, priority, completion state, created timestamp).
  - `TaskRepository`: Interface for task data operations.
  - `InMemoryTaskRepository`: Singleton session-only repository implementation.
  - `TaskListViewModel`: ViewModel publishing `TaskListUiState` via `StateFlow` and handling task creation, completion, and counters.
  - `MainActivity`: Hosts task list UI, stat cards, task filtering/counter badges, FAB for task creation, and task completion handling.
  - `TaskDetailsActivity`: Detailed view for inspecting selected tasks.

---

## 2. Verification & Test Status
- **Build Verification:** `:app:assembleDebug` — **BUILD SUCCESSFUL**
- **Unit Tests:** `:app:testDebugUnitTest` — **7/7 PASSED**
  - `TaskListViewModelTest`: Tests initial empty state, valid task addition, title trimming, blank title error emission, state emission, and task completion flow.

---

## 3. Week 2 Roadmap & Objectives
Week 2 expands the project from in-memory state to a REST API-backed application with full error handling, defect debugging, and comprehensive testing.

- **Day 5 (Mon):** Agent Setup & Handoff Lab (`AGENTS.md`, `.cursor/rules/bootcamp.mdc`, `AI_HANDOFF.md`, `CURRENT_STATUS.md`).
- **Day 6 (Tue):** Design MVVM + REST API Integration (**Planning Only** — write `REQUIREMENTS.md`, `DEVELOPMENT_PLAN.md`, `ARCHITECTURE.md`, `docs/API_CONTRACT.md`, and 4 UI states: `Loading`, `Content/Success`, `Empty`, `Error`).
- **Day 7 (Wed):** REST API Integration (`feature/api-task-list`, Retrofit/Ktor or Mock API, StateFlow pipeline, 4 UI state handling).
- **Day 8 (Thu):** Debug 3 Defects (4-step framework: `Reproduce` → `Observe` → `Isolate` → `Verify` for malformed data, HTTP 500/timeout, UI refresh issues).
- **Day 9 (Fri):** Testing & Delivery (ViewModel & Repository unit tests, APK build, Codex diff review, PR submission).
- **Sat (Nov 14):** Revision & Demo.

---

## 4. Identified Code & Architecture Risks for Week 2
1. **Lack of Network Layer & Error Handling:** Currently, `TaskRepository` is synchronously backed by `InMemoryTaskRepository`. Transitioning to a REST API requires introducing async coroutines (`Result` wrapper or `Flow`), HTTP client (Retrofit/Ktor), and handling network errors (HTTP 500, timeouts, malformed JSON).
2. **Missing UI State Representation:** `TaskListUiState` currently represents loaded tasks and creation state, but does not yet explicit represent `Loading` and `Error` states required for Day 6/7 API integration.
3. **Threading & Concurrency:** Updating tasks via API will require background thread execution and proper main-thread UI updates using `viewModelScope` and `StateFlow`.

---

## 5. Instructions for Resuming AI Agents
When starting a new session:
1. Read `README.md`, `AGENTS.md`, `.cursor/rules/bootcamp.mdc`, and `docs/CURRENT_STATUS.md`.
2. Review this document (`docs/AI_HANDOFF.md`) to understand active architecture and risks.
3. On **Day 6 (Planning Day)**: DO NOT generate feature code. Focus exclusively on requirements, architecture diagrams, sequence diagrams, and `docs/API_CONTRACT.md`. Wait for trainer approval before implementing Day 7 code.
4. Verify every change with `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.
