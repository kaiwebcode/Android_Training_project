# Employee Task Manager (Week 3 — Automated Reliability Tests & Release Validation)

Android Developer Bootcamp — Week 3 (Nov 16–21, 2026)
Project: **Employee Task Manager** (Kotlin Android).

---

## Week 3 Status Overview

| Day | Focus | Status | Deliverables / Artifacts |
|---|---|---|---|
| **Day 10** | Room Offline Persistence & SSOT | **Complete** | [`docs/evidence/day10/room-persistence-evidence.md`](docs/evidence/day10/room-persistence-evidence.md) |
| **Day 11** | WorkManager Background Synchronization | **Complete** | WorkManager offline queue & REST sync against mock server |
| **Day 12** | Edge-Case Defect Fixes & UI Guarding | **Complete** | Synchronous duplicate tap guards & cancellation handling |
| **Day 13** | Schema Migration & Integration Testing | **Complete** | Room v1–v5 to v6 migration & direct schema upgrade tests |
| **Day 14** | Automated Reliability Tests & Delivery | **Friday gate open** | [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md), [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md), [`docs/DAY14_DELIVERY.md`](docs/DAY14_DELIVERY.md) |

---

## Technical Stack & Architecture

- **Language:** Kotlin 2.1.0
- **Build System:** AGP 9.5.1, Android SDK 37 (minSdk 24), Gradle 9.6.0
- **Persistence:** **Room 2.8.5** with **KSP** (`2.0.21-1.0.27`) and automated schema export (`exportSchema = true`)
- **Background Work:** **WorkManager 2.10.0** (`CoroutineWorker`) with `ExistingWorkPolicy.APPEND_OR_REPLACE`
- **Network & Mock API:** Retrofit 2.11.0, OkHttp 4.12.0, Gson 2.10.1, Express + SQLite Mock Sync Server (`tools/mock-sync-server`)
- **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF) & Single Source of Truth (SSOT) Room Database
- **UI Framework:** Android Native XML Views, `AppCompatActivity`, `MaterialComponents`, `RecyclerView` with `ListAdapter` & `DiffUtil`
- **State Management:** `ViewModel` collecting reactive `Flow<List<Task>>` from Room and publishing `TaskListUiState` via `StateFlow`

---

## Build, install, and launch

Run these commands from the project root with an Android SDK and a compatible Gradle JDK configured. For a fresh install, connect a test device or start an emulator with USB debugging enabled. Use the device serial from `adb devices`.

```powershell
.\gradlew.bat :app:assembleDebug
adb devices
adb -s DEVICE_SERIAL install -t app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL shell am start -n com.example.a1weektaskproject/.MainActivity
```

The `-t` option also allows installation of test-only debug APKs. On a test target with a prior debug installation signed by the same key, use `adb -s DEVICE_SERIAL install -r -t app/build/outputs/apk/debug/app-debug.apk` to update the APK while keeping app data. Use the launch command alone when testing the version already installed. For online sync, run the development mock server on port 3000. Debug builds default to `http://10.0.2.2:3000/` for the Android emulator. To test on a connected physical device with ADB reverse, run the following before installing the resulting APK on a test target.

```powershell
adb -s DEVICE_SERIAL reverse tcp:3000 tcp:3000
.\gradlew.bat :app:assembleDebug "-PmockSyncBaseUrl=http://127.0.0.1:3000/"
```

For a physical device on the same LAN, substitute a URL reachable from that device in the local Gradle invocation. Keep workstation-specific endpoints out of Git. Release builds do not configure a development mock endpoint.

## Verification & Test Evidence Summary

- **Build Verification:** `./gradlew.bat :app:assembleDebug` — **BUILD SUCCESSFUL**
- **Debug APK Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **JVM unit tests:** `./gradlew.bat :app:testDebugUnitTest` — **49 passed**.
- **Isolated instrumentation:** `./gradlew.bat -Pday13IsolatedTest=true :app:connectedDebugAndroidTest` — **14 passed** on a physical OnePlus EB2101 (Android 12).
- **JUnit XML total:** **63 passed**, with zero failures, errors, or skips. The [test report](docs/TEST_REPORT.md) records the outstanding manual checks.
- **Git Diff Check:** `git diff --check` — **0 issues / clean**.

---

## Key Project Documentation Links

- **Delivery Summary:** [`docs/DAY14_DELIVERY.md`](docs/DAY14_DELIVERY.md)
- **Current Status Log:** [`docs/CURRENT_STATUS.md`](docs/CURRENT_STATUS.md)
- **Test Report & Reliability Matrix:** [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md)
- **Architecture & Offline Sync Sequence:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/diagrams/offline-sync.mmd`](docs/diagrams/offline-sync.mmd)
- **Debugging & Defect Log:** [`docs/DEBUGGING_LOG.md`](docs/DEBUGGING_LOG.md)
