# Employee Task Manager (Week 2 — Build with AI Agents)

Android Developer Bootcamp — Week 2 (Nov 9–14, 2026)  
Project: **Employee Task Manager** (Kotlin Android).

---

## Week 2 Status Overview

| Day | Focus | Status | Deliverables / Artifacts |
|---|---|---|---|
| **Mon (Day 5)** | Configure AI Agents & Handoff Lab | **Complete** | [`AGENTS.md`](AGENTS.md), [`.cursor/rules/bootcamp.mdc`](.cursor/rules/bootcamp.mdc), [`docs/AI_HANDOFF.md`](docs/AI_HANDOFF.md), [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md) |
| **Tue (Day 6)** | Plan MVVM + REST API Integration | **Complete (Planning Only)** | [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md), [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/diagrams/TASK_LIST_ARCHITECTURE.md`](docs/diagrams/TASK_LIST_ARCHITECTURE.md), [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md), [`docs/CODEX_REVIEW.md`](docs/CODEX_REVIEW.md), [`docs/DECISIONS.md`](docs/DECISIONS.md) |
| **Wed (Day 7)** | REST API Integration | **Next (Awaiting Trainer Approval)** | Retrofit/Mock REST API, `ApiTaskRepository`, 4 UI States (`Loading`, `Content`, `Empty`, `Error`) |
| **Thu (Day 8)** | Debug 3 Defects | Scheduled | 4-step framework (`Reproduce` ➔ `Observe` ➔ `Isolate` ➔ `Verify`) |
| **Fri (Day 9)** | Test & Deliver | Scheduled | ViewModel & Repository unit tests, APK build, Codex diff review, PR submission |
| **Sat (Nov 14)**| Revision & Demo | Scheduled | Live demonstration & oral defense |

---

## Technical Stack & Architecture

- **Language:** Kotlin 2.1.0
- **Build System:** AGP 9.5.0, Android SDK 37 (minSdk 24), Java 25 (JBR)
- **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **UI Framework:** Android Native XML Views, `AppCompatActivity`, `MaterialComponents`, `RecyclerView` with `ListAdapter` & `DiffUtil`
- **State Management:** `ViewModel` publishing `TaskListUiState` via `StateFlow` collected inside `repeatOnLifecycle`
- **Data Layer:** `TaskRepository` interface abstracted from concrete storage (`InMemoryTaskRepository` baseline evolving to `ApiTaskRepository`)

---

## Verification & Test Evidence

- **Build Verification:** `./gradlew assembleDebug` — **BUILD SUCCESSFUL**
- **Unit Test Suite:** `./gradlew testDebugUnitTest` — **7 PASSED, 0 SKIPPED, 0 FAILED**
  - `TaskListViewModelTest`: Tests initial empty state, valid task addition, title trimming, blank title error emission, state emission, and task completion flow.

---

## Key Project Documentation Links

- **Agent Guidance:** [`AGENTS.md`](AGENTS.md) & [`.cursor/rules/bootcamp.mdc`](.cursor/rules/bootcamp.mdc)
- **REST API Specification:** [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md)
- **Requirements & 4 UI States:** [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)
- **Architecture & Sequence Diagrams:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) & [`docs/diagrams/TASK_LIST_ARCHITECTURE.md`](docs/diagrams/TASK_LIST_ARCHITECTURE.md)
- **Development Roadmap:** [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md)
- **Codex Plan Review:** [`docs/CODEX_REVIEW.md`](docs/CODEX_REVIEW.md)
- **Decisions & Sign-off Record:** [`docs/DECISIONS.md`](docs/DECISIONS.md)
- **Current Status Log:** [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md)
- **AI Handoff Context:** [`docs/AI_HANDOFF.md`](docs/AI_HANDOFF.md)
- **Debugging Framework Log:** [`docs/DEBUGGING_LOG.md`](docs/DEBUGGING_LOG.md)
