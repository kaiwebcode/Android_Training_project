# Employee Task Manager (Week 3 — Room Offline Persistence)

Android Developer Bootcamp — Week 3 (Nov 16–21, 2026)
Project: **Employee Task Manager** (Kotlin Android).

---

## Week 3 Status Overview

| Day | Focus | Status | Deliverables / Artifacts |
|---|---|---|---|
| **Day 10** | Room Offline Persistence & SSOT | **Complete** | [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md), [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md), [`docs/evidence/day10/room-persistence-evidence.md`](docs/evidence/day10/room-persistence-evidence.md) |

---

## Technical Stack & Architecture

- **Language:** Kotlin
- **Build System:** AGP 9.5.1, Android SDK 37 (minSdk 24), Gradle 9.6.0
- **Persistence:** **Room 2.8.5** with **KSP** (`2.0.21-1.0.27`) and automated schema export (`exportSchema = true`)
- **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF) & Single Source of Truth (SSOT) Room Database
- **UI Framework:** Android Native XML Views, `AppCompatActivity`, `MaterialComponents`, `RecyclerView` with `ListAdapter` & `DiffUtil`
- **State Management:** `ViewModel` collecting reactive `Flow<List<Task>>` from Room and publishing `TaskListUiState` via `StateFlow`

---

## Verification & Test Evidence

- **Build Verification:** `.\gradlew.bat assembleDebug` — **BUILD SUCCESSFUL**
- **Unit Test Suite:** `.\gradlew.bat testDebugUnitTest` — **27 PASSED, 0 SKIPPED, 0 FAILED**
  - `TaskListViewModelTest`: 12 unit tests verifying UI state observation, task creation, validation, completion, and demo scenario switching.
  - `ApiTaskRepositoryTest`: 14 unit tests verifying offline caching, safe remote merge, network failure recovery, and demo isolation.
- **Instrumentation Test Suite:** `.\gradlew.bat connectedDebugAndroidTest` — **6 PASSED, 0 SKIPPED, 0 FAILED** (executed on physical OnePlus Nord CE EB2101, Android 12).
- **Git Diff Check:** `git diff --check` — **0 issues / clean**.

---

## Key Project Documentation Links

- **Current Status Log:** [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md)
- **Test Report:** [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md)
- **Room Persistence Evidence:** [`docs/evidence/day10/room-persistence-evidence.md`](docs/evidence/day10/room-persistence-evidence.md)
- **Architecture & SSOT:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Debugging Log:** [`docs/DEBUGGING_LOG.md`](docs/DEBUGGING_LOG.md)
