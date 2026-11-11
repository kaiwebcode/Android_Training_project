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

## 3. Week 2 Roadmap & Status
Week 2 expands the project from in-memory state to a REST API-backed application with full error handling, defect debugging, and comprehensive testing.

- **Day 5 (Mon):** Agent Setup & Handoff Lab (`AGENTS.md`, `.cursor/rules/bootcamp.mdc`, `AI_HANDOFF.md`, `CURRENT_STATUS.md`) — **COMPLETE**.
- **Day 6 (Tue):** Design MVVM + REST API Integration (**Planning Only** — write `REQUIREMENTS.md`, `DEVELOPMENT_PLAN.md`, `ARCHITECTURE.md`, `docs/API_CONTRACT.md`, and 4 UI states: `Loading`, `Content/Success`, `Empty`, `Error`) — **COMPLETE (Codex Review recorded in `docs/CODEX_REVIEW.md`)**.
- **Day 7 (Wed):** REST API Integration (`feature/api-task-list`, Retrofit/Ktor or Mock API, StateFlow pipeline, 4 UI state handling) — **NEXT (Requires Trainer Approval)**.
- **Day 8 (Thu):** Debug 3 Defects (4-step framework: `Reproduce` → `Observe` → `Isolate` → `Verify` for malformed data, HTTP 500/timeout, UI refresh issues).
- **Day 9 (Fri):** Testing & Delivery (ViewModel & Repository unit tests, APK build, Codex diff review, PR submission).
- **Sat (Nov 14):** Revision & Demo.

---

## 4. Day 6 Planning & Codex Review Artifacts
1. `docs/API_CONTRACT.md`: Complete specification for `GET /tasks`, `POST /tasks`, `PUT /tasks/{id}/complete`, JSON DTOs, HTTP status codes (200, 400, 500, network error), error response body, and UI state mapping.
2. `docs/REQUIREMENTS.md`: User stories, acceptance criteria, and edge cases for REST API integration and 4 UI states (`Loading`, `Content/Success`, `Empty`, `Error`).
3. `docs/ARCHITECTURE.md` & `docs/diagrams/TASK_LIST_ARCHITECTURE.md`: High-level architecture flowcharts and detailed sequence diagrams tracing `Screen` → `ViewModel` → `Repository` → `API`.
4. `docs/DEVELOPMENT_PLAN.md`: Updated roadmap and step-by-step implementation sequence for Day 7 REST API integration.
5. `docs/CODEX_REVIEW.md`: Independent static review challenging 5 architectural assumptions (sealed state hierarchy, one-shot event separation, human-readable exception mapping, test dispatcher injection, offline fallback).
6. `docs/DECISIONS.md`: Day 6 architecture design decisions and trainer sign-off record.
7. `docs/CURRENT_STATUS.md`: Updated to reflect completion of Day 6 planning deliverables.

---

## 5. Instructions for Resuming AI Agents
When starting a new session:
1. Read `AGENTS.md`, `.cursor/rules/bootcamp.mdc`, and `docs/CURRENT_STATUS.md`.
2. Review this document (`docs/AI_HANDOFF.md`), `docs/API_CONTRACT.md`, and `docs/CODEX_REVIEW.md`.
3. Check `docs/DECISIONS.md` to confirm Trainer Approval before writing code for Day 7 (`feature/api-task-list`).
4. On Day 7: Implement `ApiTaskRepository`, `TaskApiService`, sealed `TaskListUiState`, and 4 UI state handling in `MainActivity`.
5. Verify every change with `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`.
