package com.example.a1weektaskproject.sync

import androidx.work.ListenableWorker
import com.example.a1weektaskproject.data.local.SyncOp
import com.example.a1weektaskproject.data.local.SyncState
import com.example.a1weektaskproject.data.local.TaskDao
import com.example.a1weektaskproject.data.local.TaskEntity
import com.example.a1weektaskproject.data.remote.TaskSyncApiService
import com.example.a1weektaskproject.data.remote.TaskSyncDto
import com.example.a1weektaskproject.data.remote.TaskSyncListResponse
import com.example.a1weektaskproject.data.remote.TaskSyncResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.net.SocketTimeoutException
import java.util.UUID

class TaskSyncWorkerTest {

    private lateinit var fakeDao: FakeTaskDao

    @Before
    fun setUp() {
        fakeDao = FakeTaskDao()
    }

    @Test
    fun performSync_syncsPendingTasksSuccessfullyWhenOnline() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Offline WorkManager Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE
            )
        )

        val createdDtos = mutableListOf<TaskSyncDto>()
        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                createdDtos.add(task)
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: Map<String, Any>): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Updated", false, null))
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                return Response.success(TaskSyncListResponse(0, emptyList()))
            }
        }

        val result = TaskSyncWorker.performSync(fakeDao, fakeSyncApi)
        assertEquals(ListenableWorker.Result.success(), result)

        val syncedEntity = fakeDao.getTaskById(taskId)
        assertNotNull(syncedEntity)
        assertEquals(SyncState.SYNCED, syncedEntity?.syncState)
        assertEquals(SyncOp.NONE, syncedEntity?.syncOp)
        assertEquals(1, createdDtos.size)
        assertEquals(taskId, createdDtos[0].clientTaskId)
    }

    @Test
    fun performSync_retriesWithExponentialBackoffOnNetworkTimeout() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Timeout Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE
            )
        )

        val timeoutSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                throw SocketTimeoutException("Connection timeout")
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: Map<String, Any>): Response<TaskSyncResponse> {
                throw SocketTimeoutException("Connection timeout")
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                throw SocketTimeoutException("Connection timeout")
            }
        }

        val result = TaskSyncWorker.performSync(fakeDao, timeoutSyncApi)
        assertEquals(ListenableWorker.Result.retry(), result)

        val revertedEntity = fakeDao.getTaskById(taskId)
        assertEquals(SyncState.PENDING, revertedEntity?.syncState)
    }

    @Test
    fun performSync_handlesTimeoutAfterServerSaveIdempotentlyWithoutDuplicates() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Timeout After Save Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE
            )
        )

        val serverTasks = mutableMapOf<String, TaskSyncDto>()
        var simulateTimeoutOnFirstCall = true

        val mockServerSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                val id = task.clientTaskId
                serverTasks[id] = task

                if (simulateTimeoutOnFirstCall) {
                    simulateTimeoutOnFirstCall = false
                    throw SocketTimeoutException("Simulated connection timeout after server save")
                }
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: Map<String, Any>): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Updated", false, null))
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                return Response.success(TaskSyncListResponse(serverTasks.size, serverTasks.values.toList()))
            }
        }

        // 1st Worker Attempt (Timeout after server save)
        val result1 = TaskSyncWorker.performSync(fakeDao, mockServerSyncApi)
        assertEquals(ListenableWorker.Result.retry(), result1)

        // Verify task WAS saved on server
        assertEquals(1, serverTasks.size)
        assertTrue(serverTasks.containsKey(taskId))

        // 2nd Worker Attempt (WorkManager Retry with SAME stable clientTaskId)
        val result2 = TaskSyncWorker.performSync(fakeDao, mockServerSyncApi)
        assertEquals(ListenableWorker.Result.success(), result2)

        // Verify server tasks count is STILL 1 (0 duplicate tasks!)
        assertEquals(1, serverTasks.size)

        val synced = fakeDao.getTaskById(taskId)
        assertEquals(SyncState.SYNCED, synced?.syncState)
    }

    @Test
    fun performSync_crashRecoveryResetsStuckSyncingTasks() = runTest {
        val stuckId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = stuckId,
                title = "Stuck Task from Crash",
                syncState = SyncState.SYNCING,
                syncOp = SyncOp.CREATE
            )
        )

        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: Map<String, Any>): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Updated", false, null))
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                return Response.success(TaskSyncListResponse(0, emptyList()))
            }
        }

        val result = TaskSyncWorker.performSync(fakeDao, fakeSyncApi)
        assertEquals(ListenableWorker.Result.success(), result)

        val recoveredAndSynced = fakeDao.getTaskById(stuckId)
        assertEquals(SyncState.SYNCED, recoveredAndSynced?.syncState)
    }

    private class FakeTaskDao : TaskDao {
        private val tasks = mutableMapOf<String, TaskEntity>()

        override fun getAllTasksFlow(): Flow<List<TaskEntity>> = flowOf(tasks.values.toList())
        override suspend fun getAllTasks(): List<TaskEntity> = tasks.values.toList()
        override suspend fun getTaskById(id: String): TaskEntity? = tasks[id]
        override suspend fun insertTask(task: TaskEntity) { tasks[task.id] = task }
        override suspend fun insertTasks(tasks: List<TaskEntity>) { tasks.forEach { this.tasks[it.id] = it } }
        override suspend fun updateTask(task: TaskEntity) { tasks[task.id] = task }
        override suspend fun deleteTaskById(id: String) { tasks.remove(id) }
        override suspend fun clearAll() { tasks.clear() }

        override suspend fun getPendingSyncTasks(): List<TaskEntity> {
            return tasks.values.filter { it.syncState == SyncState.PENDING || it.syncState == SyncState.SYNCING }
        }

        override suspend fun markTasksSyncing(ids: List<String>): Int {
            var count = 0
            ids.forEach { id ->
                tasks[id]?.let {
                    tasks[id] = it.copy(syncState = SyncState.SYNCING)
                    count++
                }
            }
            return count
        }

        override suspend fun resetSyncingToPending(): Int {
            var count = 0
            tasks.keys.toList().forEach { id ->
                tasks[id]?.let {
                    if (it.syncState == SyncState.SYNCING) {
                        tasks[id] = it.copy(syncState = SyncState.PENDING)
                        count++
                    }
                }
            }
            return count
        }

        override suspend fun markTaskSyncedConditional(id: String, capturedUpdatedAt: Long): Int {
            val task = tasks[id] ?: return 0
            if (task.updatedAt == capturedUpdatedAt) {
                tasks[id] = task.copy(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE)
                return 1
            }
            return 0
        }

        override suspend fun revertTaskSyncingToPending(id: String): Int {
            val task = tasks[id] ?: return 0
            tasks[id] = task.copy(syncState = SyncState.PENDING)
            return 1
        }
    }
}
