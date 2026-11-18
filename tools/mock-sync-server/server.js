const express = require('express');
const TaskDatabase = require('./db');

const app = express();
const PORT = process.env.PORT || 3000;
const db = new TaskDatabase();

let timeoutAfterSaveActive = false;
let timeoutAfterSaveHttpCode = 504;
let timeoutDelayMs = 0;

app.use(express.json());

// CORS & logging middleware
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Content-Type, X-Simulate-Timeout');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }
  next();
});

// GET /health
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'EmployeeTaskManager-MockSyncServer',
    timestamp: new Date().toISOString(),
    database: 'connected',
    simulations: {
      timeoutAfterSave: timeoutAfterSaveActive
    }
  });
});

// GET /tasks - Return all persisted tasks
app.get('/tasks', (req, res) => {
  const tasks = db.getAllTasks();
  res.json({
    count: tasks.length,
    tasks
  });
});

// GET /tasks/:clientTaskId - Inspection endpoint
app.get('/tasks/:clientTaskId', (req, res) => {
  const { clientTaskId } = req.params;
  const task = db.getTaskById(clientTaskId);
  if (!task) {
    return res.status(404).json({ error: `Task with clientTaskId '${clientTaskId}' not found` });
  }
  res.json({ task });
});

// POST /tasks - Create or idempotent retry task
app.post('/tasks', (req, res) => {
  const { clientTaskId, title, description, completed, priority, dueDate } = req.body;

  if (!clientTaskId || typeof clientTaskId !== 'string' || clientTaskId.trim() === '') {
    return res.status(400).json({ error: 'clientTaskId must be a non-empty string' });
  }

  if (!title || typeof title !== 'string' || title.trim() === '') {
    return res.status(400).json({ error: 'title cannot be empty' });
  }

  const existingTask = db.getTaskById(clientTaskId);
  let taskToReturn;

  if (existingTask) {
    taskToReturn = existingTask;
  } else {
    try {
      taskToReturn = db.insertTask({
        clientTaskId: clientTaskId.trim(),
        title: title.trim(),
        description: description ? description.trim() : '',
        completed: Boolean(completed),
        priority: priority || 'MEDIUM',
        dueDate: dueDate || 'Today'
      });
    } catch (err) {
      if (err.message && err.message.includes('UNIQUE constraint failed')) {
        taskToReturn = db.getTaskById(clientTaskId);
      } else {
        return res.status(500).json({ error: `Database error: ${err.message}` });
      }
    }
  }

  const simulateTimeout = timeoutAfterSaveActive || req.headers['x-simulate-timeout'] === 'true';

  if (simulateTimeout) {
    timeoutAfterSaveActive = false; // consume simulation trigger

    const sendErrorResponse = () => {
      res.status(timeoutAfterSaveHttpCode).json({
        error: 'Simulated connection timeout after server save',
        taskSaved: true,
        clientTaskId: taskToReturn.clientTaskId
      });
    };

    if (timeoutDelayMs > 0) {
      setTimeout(sendErrorResponse, timeoutDelayMs);
      return;
    } else {
      return sendErrorResponse();
    }
  }

  const statusCode = existingTask ? 200 : 201;
  return res.status(statusCode).json({
    status: 'success',
    message: existingTask ? 'Task already exists (idempotent retry)' : 'Task created successfully',
    duplicateCreated: false,
    task: taskToReturn
  });
});

// PATCH /tasks/:clientTaskId - Update existing task
app.patch('/tasks/:clientTaskId', (req, res) => {
  const { clientTaskId } = req.params;
  const updates = req.body;

  const existing = db.getTaskById(clientTaskId);
  if (!existing) {
    return res.status(404).json({ error: `Task with clientTaskId '${clientTaskId}' not found` });
  }

  const updatedTask = db.updateTask(clientTaskId, updates);
  res.json({
    status: 'success',
    message: 'Task updated successfully',
    task: updatedTask
  });
});

// DELETE /tasks - Development testing cleanup
app.delete('/tasks', (req, res) => {
  db.clearAllTasks();
  res.json({ status: 'success', message: 'All server tasks cleared' });
});

// POST /simulation/timeout-after-save - Activate simulation mode
app.post('/simulation/timeout-after-save', (req, res) => {
  const { httpCode = 504, delayMs = 0 } = req.body || {};
  timeoutAfterSaveActive = true;
  timeoutAfterSaveHttpCode = httpCode;
  timeoutDelayMs = delayMs;
  res.json({
    status: 'success',
    simulation: 'timeout-after-save',
    active: true,
    httpCode,
    delayMs
  });
});

// POST /simulation/reset - Reset all simulations
app.post('/simulation/reset', (req, res) => {
  timeoutAfterSaveActive = false;
  timeoutAfterSaveHttpCode = 504;
  timeoutDelayMs = 0;
  res.json({ status: 'success', message: 'Simulations reset to normal' });
});

let serverInstance = null;
if (require.main === module) {
  serverInstance = app.listen(PORT, '0.0.0.0', () => {
    console.log(`[MockSyncServer] Server running on http://0.0.0.0:${PORT}`);
    console.log(`[MockSyncServer] SQLite Database initialized.`);
  });
}

module.exports = { app, db, getApp: () => app };
