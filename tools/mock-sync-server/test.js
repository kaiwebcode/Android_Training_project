const { test, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs');
const TaskDatabase = require('./db');

const TEST_DB_PATH = path.join(__dirname, 'test_mock_tasks.db');

let db;
let app;

// Helper to make HTTP requests against express app instance
async function httpRequest(appInstance, method, pathUrl, body = null, headers = {}) {
  const http = require('node:http');
  const server = appInstance.listen(0); // Listen on random available port
  const port = server.address().port;

  return new Promise((resolve, reject) => {
    const options = {
      hostname: '127.0.0.1',
      port,
      path: pathUrl,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    };

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        server.close();
        let parsed = null;
        try {
          parsed = JSON.parse(data);
        } catch (_) {
          parsed = data;
        }
        resolve({ statusCode: res.statusCode, body: parsed });
      });
    });

    req.on('error', (err) => {
      server.close();
      reject(err);
    });

    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

before(() => {
  if (fs.existsSync(TEST_DB_PATH)) {
    fs.unlinkSync(TEST_DB_PATH);
  }
  process.env.DB_FILE = TEST_DB_PATH;
  db = new TaskDatabase(TEST_DB_PATH);
  const serverModule = require('./server');
  app = serverModule.app;
});

beforeEach(() => {
  db.clearAllTasks();
});

after(() => {
  try {
    db.close();
  } catch (_) {}
  try {
    if (fs.existsSync(TEST_DB_PATH)) {
      fs.unlinkSync(TEST_DB_PATH);
    }
  } catch (_) {}
});

test('GET /health returns 200 OK', async () => {
  const res = await httpRequest(app, 'GET', '/health');
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.status, 'ok');
  assert.equal(res.body.database, 'connected');
});

test('POST /tasks creates a new task in SQLite database', async () => {
  const payload = {
    clientTaskId: 'uuid-test-001',
    title: 'Test Task 1',
    description: 'Detailed description',
    completed: false,
    priority: 'HIGH',
    dueDate: 'Nov 20, 2026'
  };

  const res = await httpRequest(app, 'POST', '/tasks', payload);
  assert.equal(res.statusCode, 201);
  assert.equal(res.body.status, 'success');
  assert.equal(res.body.duplicateCreated, false);
  assert.equal(res.body.task.clientTaskId, 'uuid-test-001');

  // Verify stored in SQLite
  const stored = db.getTaskById('uuid-test-001');
  assert.notEqual(stored, null);
  assert.equal(stored.title, 'Test Task 1');
  assert.equal(stored.completed, false);
});

test('POST /tasks handles repeated identical request idempotently without duplicate row', async () => {
  const payload = {
    clientTaskId: 'uuid-test-002',
    title: 'Idempotent Task',
    description: 'Test idempotency',
    completed: false,
    priority: 'MEDIUM',
    dueDate: 'Today'
  };

  // First POST
  const res1 = await httpRequest(app, 'POST', '/tasks', payload);
  assert.equal(res1.statusCode, 201);

  // Second identical POST (Retry)
  const res2 = await httpRequest(app, 'POST', '/tasks', payload);
  assert.equal(res2.statusCode, 200);
  assert.equal(res2.body.duplicateCreated, false);
  assert.equal(res2.body.message, 'Task already exists (idempotent retry)');

  // Verify SQLite contains exactly 1 task
  const allTasks = db.getAllTasks();
  assert.equal(allTasks.length, 1);
});

test('POST /tasks rejects invalid payloads', async () => {
  // Missing clientTaskId
  const res1 = await httpRequest(app, 'POST', '/tasks', { title: 'No ID' });
  assert.equal(res1.statusCode, 400);

  // Blank title
  const res2 = await httpRequest(app, 'POST', '/tasks', { clientTaskId: 'uuid-1', title: '   ' });
  assert.equal(res2.statusCode, 400);
});

test('PATCH /tasks/:clientTaskId updates task and returns 404 for missing task', async () => {
  const payload = {
    clientTaskId: 'uuid-patch-001',
    title: 'Initial Title',
    completed: false
  };
  await httpRequest(app, 'POST', '/tasks', payload);

  // Successful PATCH
  const patchRes = await httpRequest(app, 'PATCH', '/tasks/uuid-patch-001', { completed: true, title: 'Updated Title' });
  assert.equal(patchRes.statusCode, 200);
  assert.equal(patchRes.body.task.completed, true);
  assert.equal(patchRes.body.task.title, 'Updated Title');
  assert.equal(patchRes.body.task.version, 2);

  // Missing task PATCH returns 404
  const missingPatch = await httpRequest(app, 'PATCH', '/tasks/non-existent-id', { completed: true });
  assert.equal(missingPatch.statusCode, 404);
});

test('GET /tasks returns task list and correct count', async () => {
  await httpRequest(app, 'POST', '/tasks', { clientTaskId: 'id-1', title: 'Task 1' });
  await httpRequest(app, 'POST', '/tasks', { clientTaskId: 'id-2', title: 'Task 2' });

  const res = await httpRequest(app, 'GET', '/tasks');
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.count, 2);
  assert.equal(res.body.tasks.length, 2);
});

test('GET /tasks/:clientTaskId retrieves specific task or 404', async () => {
  await httpRequest(app, 'POST', '/tasks', { clientTaskId: 'find-me-123', title: 'Find Me' });

  const found = await httpRequest(app, 'GET', '/tasks/find-me-123');
  assert.equal(found.statusCode, 200);
  assert.equal(found.body.task.title, 'Find Me');

  const notFound = await httpRequest(app, 'GET', '/tasks/unknown-id');
  assert.equal(notFound.statusCode, 404);
});

test('SQLite persistence after closing and reopening database', async () => {
  const persistDbPath = path.join(__dirname, 'test_persistence_node.db');
  if (fs.existsSync(persistDbPath)) fs.unlinkSync(persistDbPath);

  // Open DB 1, insert task, close DB 1
  const db1 = new TaskDatabase(persistDbPath);
  db1.insertTask({ clientTaskId: 'persist-id-100', title: 'Persistent Node Task' });
  db1.close();

  // Open DB 2, retrieve task, close DB 2
  const db2 = new TaskDatabase(persistDbPath);
  const retrieved = db2.getTaskById('persist-id-100');
  assert.notEqual(retrieved, null);
  assert.equal(retrieved.title, 'Persistent Node Task');
  db2.close();

  if (fs.existsSync(persistDbPath)) fs.unlinkSync(persistDbPath);
});

test('TIMEOUT-AFTER-SERVER-SAVE scenario and retry duplicate prevention', async () => {
  // Activate timeout simulation mode
  await httpRequest(app, 'POST', '/simulation/timeout-after-save', { httpCode: 504 });

  const clientTaskId = 'timeout-uuid-999';
  const taskPayload = {
    clientTaskId,
    title: 'Timeout Task',
    description: 'Will timeout on 1st call, succeed on retry',
    completed: false,
    priority: 'HIGH'
  };

  // 1st Attempt: Client sends request -> receives 504 Gateway Timeout
  const res1 = await httpRequest(app, 'POST', '/tasks', taskPayload);
  assert.equal(res1.statusCode, 504);
  assert.equal(res1.body.taskSaved, true);

  // Verify task WAS saved in SQLite database despite timeout HTTP response
  const savedInDb = db.getTaskById(clientTaskId);
  assert.notEqual(savedInDb, null);
  assert.equal(savedInDb.title, 'Timeout Task');

  // Verify database count is 1
  const countBeforeRetry = db.getAllTasks().length;
  assert.equal(countBeforeRetry, 1);

  // 2nd Attempt (WorkManager Retry with SAME clientTaskId)
  const res2 = await httpRequest(app, 'POST', '/tasks', taskPayload);
  assert.equal(res2.statusCode, 200);
  assert.equal(res2.body.duplicateCreated, false);
  assert.equal(res2.body.message, 'Task already exists (idempotent retry)');

  // Verify database STILL contains exactly 1 task (0 duplicates created!)
  const countAfterRetry = db.getAllTasks().length;
  assert.equal(countAfterRetry, 1);
});
