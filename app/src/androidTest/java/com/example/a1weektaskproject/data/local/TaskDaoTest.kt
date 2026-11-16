package com.example.a1weektaskproject.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Instrumentation test suite for Room TaskDao CRUD, Flow observation, and database reopening persistence.
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetById() {
        runBlocking {
            val uuid = UUID.randomUUID().toString()
            val task = TaskEntity(
                id = uuid,
                title = "Test Task",
                description = "Test Description",
                completed = false,
                priority = "HIGH",
                dueDate = "Tomorrow",
                updatedAt = System.currentTimeMillis(),
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE
            )
            taskDao.insertTask(task)

            val retrieved = taskDao.getTaskById(uuid)
            assertNotNull(retrieved)
            assertEquals("Test Task", retrieved?.title)
            assertEquals(SyncState.PENDING, retrieved?.syncState)
            assertEquals(SyncOp.CREATE, retrieved?.syncOp)
        }
    }

    @Test
    fun updateTask() {
        runBlocking {
            val uuid = UUID.randomUUID().toString()
            val task = TaskEntity(id = uuid, title = "Initial Title", completed = false)
            taskDao.insertTask(task)

            val updatedTask = task.copy(
                title = "Updated Title",
                completed = true,
                syncState = SyncState.PENDING,
                syncOp = SyncOp.UPDATE
            )
            taskDao.updateTask(updatedTask)

            val retrieved = taskDao.getTaskById(uuid)
            assertNotNull(retrieved)
            assertEquals("Updated Title", retrieved?.title)
            assertTrue(retrieved?.completed == true)
            assertEquals(SyncState.PENDING, retrieved?.syncState)
            assertEquals(SyncOp.UPDATE, retrieved?.syncOp)
        }
    }

    @Test
    fun observeTasksFlow() {
        runBlocking {
            val uuid1 = UUID.randomUUID().toString()
            val uuid2 = UUID.randomUUID().toString()
            val task1 = TaskEntity(id = uuid1, title = "Task 1", updatedAt = 1000L)
            val task2 = TaskEntity(id = uuid2, title = "Task 2", updatedAt = 2000L)

            taskDao.insertTasks(listOf(task1, task2))

            val tasksList = taskDao.getAllTasksFlow().first()
            assertEquals(2, tasksList.size)
            // Ordered by updatedAt DESC
            assertEquals("Task 2", tasksList[0].title)
            assertEquals("Task 1", tasksList[1].title)
        }
    }

    @Test
    fun deleteTaskById() {
        runBlocking {
            val uuid = UUID.randomUUID().toString()
            val task = TaskEntity(id = uuid, title = "To Delete")
            taskDao.insertTask(task)

            assertNotNull(taskDao.getTaskById(uuid))
            taskDao.deleteTaskById(uuid)
            assertNull(taskDao.getTaskById(uuid))
        }
    }

    @Test
    fun persistenceAcrossReopeningDatabase() {
        runBlocking {
            val dbName = "test_persistence.db"
            context.deleteDatabase(dbName)

            val db1 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val uuid = UUID.randomUUID().toString()
            val task = TaskEntity(
                id = uuid,
                title = "Persistent Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE
            )
            db1.taskDao().insertTask(task)
            db1.close()

            val db2 = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .allowMainThreadQueries()
                .build()
            val retrieved = db2.taskDao().getTaskById(uuid)
            assertNotNull(retrieved)
            assertEquals("Persistent Task", retrieved?.title)
            assertEquals(SyncState.PENDING, retrieved?.syncState)
            db2.close()

            context.deleteDatabase(dbName)
        }
    }
}
