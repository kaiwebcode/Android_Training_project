package com.example.a1weektaskproject.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.example.a1weektaskproject.data.local.SyncOp
import com.example.a1weektaskproject.data.local.SyncState
import com.example.a1weektaskproject.data.local.TaskDao
import com.example.a1weektaskproject.data.local.TaskEntity
import com.example.a1weektaskproject.data.local.TaskOrigin
import com.example.a1weektaskproject.data.remote.TaskSyncApiService
import com.example.a1weektaskproject.data.remote.TaskSyncDto
import com.example.a1weektaskproject.data.remote.TaskSyncListResponse
import com.example.a1weektaskproject.data.remote.TaskSyncResponse
import com.example.a1weektaskproject.data.remote.TaskSyncUpdateDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

class TaskSyncWorkerTest {

    private lateinit var fakeDao: FakeTaskDao

    @Before
    fun setUp() {
        fakeDao = FakeTaskDao()
    }

    @Test
    fun doWork_returnsFailureWhenTaskDaoIsUnavailable() = runTest {
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(context)
        val worker = TaskSyncWorker(context, mock(WorkerParameters::class.java))

        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    @Test
    fun performSync_syncsPendingTasksSuccessfullyWhenOnline() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Offline WorkManager Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val createdDtos = mutableListOf<TaskSyncDto>()
        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                createdDtos.add(task)
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
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
    fun performSync_syncsPendingUpdateTasksSuccessfullyWithConcreteDto() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Update WorkManager Task",
                completed = true,
                syncState = SyncState.PENDING,
                syncOp = SyncOp.UPDATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val updatedDtos = mutableListOf<TaskSyncUpdateDto>()
        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
                updatedDtos.add(body)
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
        assertEquals(1, updatedDtos.size)
        assertTrue(updatedDtos[0].completed)
    }

    @Test
    fun performSync_realRetrofitPatchRequest_serializesAndDispatchesCorrectPayload() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"success","message":"Updated"}"""))
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val realRetrofitService = retrofit.create(TaskSyncApiService::class.java)

        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Retrofit Real Integration Test Task",
                completed = true,
                syncState = SyncState.PENDING,
                syncOp = SyncOp.UPDATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val result = TaskSyncWorker.performSync(fakeDao, realRetrofitService)
        assertEquals(ListenableWorker.Result.success(), result)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/tasks/$taskId", request.path)
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains("\"completed\":true"))
        assertTrue(requestBody.contains("\"title\":\"Retrofit Real Integration Test Task\""))

        server.shutdown()
    }

    @Test
    fun performSync_ignoresGoRestSampleTasksWithGorestOrigin() = runTest {
        val goRestTaskId = "7654321"
        fakeDao.insertTask(
            TaskEntity(
                id = goRestTaskId,
                title = "GoRest Sample Task",
                completed = true,
                syncState = SyncState.PENDING,
                syncOp = SyncOp.UPDATE,
                origin = TaskOrigin.GOREST
            )
        )

        var apiCallMade = false
        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                apiCallMade = true
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
                apiCallMade = true
                return Response.success(TaskSyncResponse("success", "Updated", false, null))
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                return Response.success(TaskSyncListResponse(0, emptyList()))
            }
        }

        val result = TaskSyncWorker.performSync(fakeDao, fakeSyncApi)
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(!apiCallMade)
    }

    @Test
    fun performSync_revertsSyncingToPendingWhenTaskMutatedLocallyDuringSync() = runTest {
        val taskId = UUID.randomUUID().toString()
        val initialUpdatedAt = 1000L
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Mutated Task",
                updatedAt = initialUpdatedAt,
                syncState = SyncState.PENDING,
                syncOp = SyncOp.UPDATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
                // Simulate local mutation (e.g. user toggles task in UI while sync HTTP request is in-flight)
                val current = fakeDao.getTaskById(clientTaskId)
                if (current != null) {
                    fakeDao.updateTask(current.copy(updatedAt = 2000L, completed = true))
                }
                return Response.success(TaskSyncResponse("success", "Updated", false, null))
            }
            override suspend fun getTasks(): Response<TaskSyncListResponse> {
                return Response.success(TaskSyncListResponse(0, emptyList()))
            }
        }

        val result = TaskSyncWorker.performSync(fakeDao, fakeSyncApi)
        assertEquals(ListenableWorker.Result.retry(), result)

        val revertedEntity = fakeDao.getTaskById(taskId)
        assertNotNull(revertedEntity)
        assertEquals(SyncState.PENDING, revertedEntity?.syncState)
    }

    @Test
    fun performSync_requestsRetryAndRestoresPendingOnNetworkTimeout() = runTest {
        val taskId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = taskId,
                title = "Timeout Task",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val timeoutSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                throw SocketTimeoutException("Connection timeout")
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
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
                syncOp = SyncOp.CREATE,
                origin = TaskOrigin.LOCAL
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
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
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
    fun performSync_staleCreateReplaySendsNewerEditAsPatch() = runTest {
        val taskId = UUID.randomUUID().toString()
        val stored = java.util.concurrent.ConcurrentHashMap<String, JsonObject>()
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val payload = JsonParser.parseString(request.body.clone().readUtf8()).asJsonObject
                    if (request.method == "POST" && request.path == "/tasks") {
                        val id = payload.get("clientTaskId").asString
                        val previous = stored.putIfAbsent(id, payload)
                        return if (previous == null) {
                            MockResponse().setResponseCode(504).setBody("""{"taskSaved":true}""")
                        } else {
                            MockResponse().setBody("""{"duplicateCreated":false,"task":$previous}""")
                        }
                    }
                    if (request.method == "PATCH" && request.path == "/tasks/$taskId") {
                        val previous = stored[taskId] ?: return MockResponse().setResponseCode(404)
                        val patched = previous.deepCopy()
                        payload.entrySet().forEach { (key, value) -> patched.add(key, value) }
                        stored[taskId] = patched
                        return MockResponse().setBody("""{"task":$patched}""")
                    }
                    return MockResponse().setResponseCode(400)
                }
            }
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TaskSyncApiService::class.java)
            val initial = TaskEntity(
                id = taskId,
                title = "Original title",
                syncState = SyncState.PENDING,
                syncOp = SyncOp.CREATE,
                origin = TaskOrigin.LOCAL
            )
            fakeDao.insertTask(initial)

            assertEquals(ListenableWorker.Result.retry(), TaskSyncWorker.performSync(fakeDao, api))
            assertEquals(false, stored.getValue(taskId).get("completed").asBoolean)
            val edited = fakeDao.editCompletion(taskId, completed = true, updatedAt = initial.updatedAt + 1)
            assertNotNull(edited)

            assertEquals(ListenableWorker.Result.retry(), TaskSyncWorker.performSync(fakeDao, api))
            assertEquals(edited?.copy(syncOp = SyncOp.UPDATE), fakeDao.getTaskById(taskId))
            assertEquals(false, stored.getValue(taskId).get("completed").asBoolean)

            assertEquals(ListenableWorker.Result.success(), TaskSyncWorker.performSync(fakeDao, api))
            assertEquals(edited?.copy(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE), fakeDao.getTaskById(taskId))
            assertEquals(setOf(taskId), stored.keys.toSet())
            assertEquals(true, stored.getValue(taskId).get("completed").asBoolean)

            val requests = List(3) { requireNotNull(server.takeRequest(1, TimeUnit.SECONDS)) }
            assertEquals(listOf("POST", "POST", "PATCH"), requests.map { it.method })
            assertEquals(listOf("/tasks", "/tasks", "/tasks/$taskId"), requests.map { it.path })
            for (post in requests.take(2)) {
                val payload = JsonParser.parseString(post.body.readUtf8()).asJsonObject
                assertEquals(taskId, payload.get("clientTaskId").asString)
            }
        }
    }

    @Test
    fun performSync_crashRecoveryResetsStuckSyncingTasks() = runTest {
        val stuckId = UUID.randomUUID().toString()
        fakeDao.insertTask(
            TaskEntity(
                id = stuckId,
                title = "Stuck Task from Crash",
                syncState = SyncState.SYNCING,
                syncOp = SyncOp.CREATE,
                origin = TaskOrigin.LOCAL
            )
        )

        val fakeSyncApi = object : TaskSyncApiService {
            override suspend fun createTask(task: TaskSyncDto): Response<TaskSyncResponse> {
                return Response.success(TaskSyncResponse("success", "Created", false, task))
            }
            override suspend fun updateTaskStatus(clientTaskId: String, body: TaskSyncUpdateDto): Response<TaskSyncResponse> {
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
        override suspend fun confirmGoRestOrigin(id: String) {
            val task = tasks[id] ?: return
            if (task.origin == TaskOrigin.UNKNOWN) tasks[id] = task.copy(origin = TaskOrigin.GOREST)
        }
        override suspend fun deleteTaskById(id: String) { tasks.remove(id) }
        override suspend fun clearAll() { tasks.clear() }

        override suspend fun getPendingSyncTasks(): List<TaskEntity> {
            return tasks.values.filter { it.origin == TaskOrigin.LOCAL && (it.syncState == SyncState.PENDING || it.syncState == SyncState.SYNCING) }
        }

        override suspend fun markTasksSyncing(ids: List<String>): Int {
            var count = 0
            ids.forEach { id ->
                tasks[id]?.takeIf { it.origin == TaskOrigin.LOCAL && it.syncState == SyncState.PENDING }?.let {
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
                    if (it.origin == TaskOrigin.LOCAL && it.syncState == SyncState.SYNCING) {
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
            if (task.origin != TaskOrigin.LOCAL || task.syncState != SyncState.SYNCING) return 0
            tasks[id] = task.copy(syncState = SyncState.PENDING)
            return 1
        }
    }
}
