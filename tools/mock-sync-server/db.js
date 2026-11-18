const { DatabaseSync } = require('node:sqlite');
const path = require('node:path');

/**
 * Encapsulates SQLite Database connection and queries for MockSyncServer.
 */
class TaskDatabase {
  constructor(dbPath = null) {
    const finalPath = dbPath || process.env.DB_FILE || path.join(__dirname, 'mock_tasks.db');
    this.db = new DatabaseSync(finalPath);
    this.initSchema();
  }

  initSchema() {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS tasks (
        clientTaskId TEXT PRIMARY KEY NOT NULL,
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        completed INTEGER NOT NULL DEFAULT 0,
        priority TEXT NOT NULL DEFAULT 'MEDIUM',
        dueDate TEXT NOT NULL DEFAULT 'Today',
        version INTEGER NOT NULL DEFAULT 1,
        createdAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );
    `);
  }

  getAllTasks() {
    const query = this.db.prepare('SELECT * FROM tasks ORDER BY createdAt DESC');
    const rows = query.all();
    return rows.map(r => this.mapRow(r));
  }

  getTaskById(clientTaskId) {
    const query = this.db.prepare('SELECT * FROM tasks WHERE clientTaskId = ?');
    const row = query.get(clientTaskId);
    return row ? this.mapRow(row) : null;
  }

  insertTask(task) {
    const stmt = this.db.prepare(`
      INSERT INTO tasks (clientTaskId, title, description, completed, priority, dueDate, version, createdAt, updatedAt)
      VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    `);
    const completedInt = task.completed ? 1 : 0;
    const version = task.version || 1;
    stmt.run(
      task.clientTaskId,
      task.title,
      task.description || '',
      completedInt,
      task.priority || 'MEDIUM',
      task.dueDate || 'Today',
      version
    );
    return this.getTaskById(task.clientTaskId);
  }

  updateTask(clientTaskId, updates) {
    const existing = this.getTaskById(clientTaskId);
    if (!existing) return null;

    const title = updates.title !== undefined ? updates.title : existing.title;
    const description = updates.description !== undefined ? updates.description : existing.description;
    const completedInt = updates.completed !== undefined ? (updates.completed ? 1 : 0) : (existing.completed ? 1 : 0);
    const priority = updates.priority !== undefined ? updates.priority : existing.priority;
    const dueDate = updates.dueDate !== undefined ? updates.dueDate : existing.dueDate;
    const newVersion = existing.version + 1;

    const stmt = this.db.prepare(`
      UPDATE tasks
      SET title = ?, description = ?, completed = ?, priority = ?, dueDate = ?, version = ?, updatedAt = CURRENT_TIMESTAMP
      WHERE clientTaskId = ?
    `);
    stmt.run(title, description, completedInt, priority, dueDate, newVersion, clientTaskId);
    return this.getTaskById(clientTaskId);
  }

  clearAllTasks() {
    this.db.exec('DELETE FROM tasks');
  }

  close() {
    this.db.close();
  }

  mapRow(row) {
    return {
      clientTaskId: row.clientTaskId,
      title: row.title,
      description: row.description,
      completed: Boolean(row.completed),
      priority: row.priority,
      dueDate: row.dueDate,
      version: row.version,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }
}

module.exports = TaskDatabase;
