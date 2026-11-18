# Employee Task Manager — Stateful Mock Sync Server

This development-only server provides a persistent, stateful mock backend for **Week 3 Day 11 WorkManager Background Syncing** and idempotency testing.

---

## Features
- **Persistent SQLite Storage**: Tasks are saved to `mock_tasks.db` on disk.
- **`clientTaskId` Uniqueness**: Enforces SQLite PRIMARY KEY constraint on `clientTaskId` (36-char UUID).
- **Zero Duplicate Creation**: Repeated identical `POST /tasks` calls with the same `clientTaskId` return HTTP 200 without creating duplicate database rows.
- **Timeout-After-Server-Save Failure Simulation**: Persists tasks to SQLite first, then returns HTTP 504 Gateway Timeout to simulate network response loss. Retries with the same `clientTaskId` return success with 0 duplicate rows.

---

## 1. Quick Start Instructions (Windows PowerShell)

### Run Server:
```powershell
cd tools\mock-sync-server
npm.cmd start
```
The server will start listening on `0.0.0.0:3000`.

### Run Automated Test Suite:
```powershell
cd tools\mock-sync-server
npm.cmd test
```

---

## 2. API Endpoints Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Health check & active simulation status |
| `GET` | `/tasks` | List all persisted tasks & total count |
| `GET` | `/tasks/:clientTaskId` | Inspect specific task by UUID |
| `POST` | `/tasks` | Create task or handle idempotent retry |
| `PATCH` | `/tasks/:clientTaskId` | Update task completed status / title |
| `DELETE` | `/tasks` | Clear all tasks from database (testing cleanup) |
| `POST` | `/simulation/timeout-after-save` | Activate timeout-after-save mode |
| `POST` | `/simulation/reset` | Reset simulation modes to normal |

---

## 3. Example Request & Response Payloads

### POST /tasks (Creation / Idempotent Retry)
```json
// Request Body
{
  "clientTaskId": "a7b8c9d0-1234-5678-9abc-def012345678",
  "title": "Offline Task Created in Airplane Mode",
  "description": "Will upload automatically on network restore",
  "completed": false,
  "priority": "HIGH",
  "dueDate": "Nov 17, 2026"
}

// Success Response (201 Created or 200 OK for retry)
{
  "status": "success",
  "message": "Task created successfully",
  "duplicateCreated": false,
  "task": {
    "clientTaskId": "a7b8c9d0-1234-5678-9abc-def012345678",
    "title": "Offline Task Created in Airplane Mode",
    "description": "Will upload automatically on network restore",
    "completed": false,
    "priority": "HIGH",
    "dueDate": "Nov 17, 2026",
    "version": 1,
    "createdAt": "2026-11-17 12:00:00",
    "updatedAt": "2026-11-17 12:00:00"
  }
}
```

### Timeout-After-Server-Save Demonstration
```powershell
# 1. Enable timeout simulation
Invoke-RestMethod -Uri "http://localhost:3000/simulation/timeout-after-save" -Method POST

# 2. Send POST /tasks -> Task is saved in SQLite, but receives HTTP 504 Timeout
# 3. WorkManager Retries with SAME clientTaskId -> Receives HTTP 200 OK with duplicateCreated: false
```

---

## 4. Local Wi-Fi Access from Android Device (OnePlus Nord CE)

To connect the physical Android device to this test server over local Wi-Fi:

1. **Find Laptop IP Address**:
   Run `ipconfig` in PowerShell and locate IPv4 Address (e.g. `192.168.1.150`).
2. **Network Connection**:
   Ensure both laptop and OnePlus Nord CE are connected to the **same Wi-Fi network**.
3. **Server Base URL**:
   `http://<laptop-ip>:3000/` (e.g., `http://192.168.1.150:3000/`)
4. **Android Cleartext HTTP Configuration**:
   When connecting to cleartext `http://` in future stages, ensure `android:usesCleartextTraffic="true"` is enabled in `app/src/main/AndroidManifest.xml` for debug builds.
5. **Inspect Stored Server Tasks**:
   Open `http://<laptop-ip>:3000/tasks` in Chrome or mobile browser to view all persisted server tasks and verify zero duplicates.
