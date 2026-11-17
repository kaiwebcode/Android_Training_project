package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.local.SyncOp
import com.example.a1weektaskproject.data.local.SyncState
import com.example.a1weektaskproject.data.local.TaskDao
import com.example.a1weektaskproject.data.local.TaskEntity
import com.example.a1weektaskproject.data.local.toEntity
import com.example.a1weektaskproject.data.remote.ApiClient
import com.example.a1weektaskproject.data.remote.NetworkResult
import com.example.a1weektaskproject.data.remote.TaskApiService
import com.example.a1weektaskproject.data.remote.model.GoRestTaskResponse
import com.example.a1weektaskproject.data.remote.model.TaskDto
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

enum class DemoScenario {
    NORMAL_API,
    MALFORMED_JSON,
    HTTP_500_SERVER_ERROR,
    NETWORK_TIMEOUT,
    UI_STALE_STATE_SYNC,
    EMPTY_RESPONSE
}

/**
 * REST API & Room Database Single Source of Truth Repository implementation.
 */
class ApiTaskRepository(
    val apiService: TaskApiService = ApiClient.taskApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val taskDao: TaskDao? = null
) : TaskRepository {

    private val localInMemoryTasks = mutableListOf<Task>()
    private val _localInMemoryFlow = MutableStateFlow<List<Task>>(emptyList())
    var currentDemoScenario: DemoScenario = DemoScenario.NORMAL_API

    override fun getTasksFlow(): Flow<List<Task>> {
        return taskDao?.getAllTasksFlow()?.map { entities ->
            entities.map { it.toDomainTask() }
        } ?: _localInMemoryFlow.asStateFlow()
    }

    override suspend fun getTasks(): NetworkResult<List<Task>> = withContext(ioDispatcher) {
        val cachedEntities = taskDao?.getAllTasks() ?: emptyList()
        val cachedTasks = if (cachedEntities.isNotEmpty()) {
            cachedEntities.map { it.toDomainTask() }
        } else {
            localInMemoryTasks.toList()
        }

        try {
            when (currentDemoScenario) {
                DemoScenario.MALFORMED_JSON -> {
                    throw JsonSyntaxException("Malformed JSON syntax error in server response")
                }
                DemoScenario.HTTP_500_SERVER_ERROR -> {
                    throw HttpException(retrofit2.Response.error<GoRestTaskResponse>(500, "Internal Server Error".toResponseBody(null)))
                }
                DemoScenario.NETWORK_TIMEOUT -> {
                    throw SocketTimeoutException("Server connection timed out after 15000ms")
                }
                DemoScenario.UI_STALE_STATE_SYNC -> {
                    if (cachedTasks.isEmpty()) {
                        val demoTasks = listOf(
                            Task("DEMO-101", "Review Q4 design documents", "Check latest specs", false, TaskPriority.HIGH, "Nov 12, 2026"),
                            Task("DEMO-102", "Submit weekly status report", "Review performance", true, TaskPriority.MEDIUM, "Nov 12, 2026"),
                            Task("DEMO-103", "Update client onboard documentation", "Check latest changes", false, TaskPriority.LOW, "Nov 12, 2026")
                        )
                        if (taskDao != null) {
                            taskDao.insertTasks(demoTasks.map { it.toEntity(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE) })
                        } else {
                            localInMemoryTasks.addAll(demoTasks)
                            _localInMemoryFlow.value = localInMemoryTasks.toList()
                        }
                    }
                    val currentTasks = taskDao?.getAllTasks()?.map { it.toDomainTask() } ?: localInMemoryTasks.toList()
                    return@withContext NetworkResult.Success(currentTasks)
                }
                DemoScenario.EMPTY_RESPONSE -> {
                    return@withContext NetworkResult.Success(emptyList<Task>())
                }
                DemoScenario.NORMAL_API -> {
                    val response = apiService.getTasks()
                    if (response.isSuccessful) {
                        val dtoList = response.body()?.data ?: emptyList()
                        val remoteTasks = dtoList.map { it.toDomainTask() }

                        if (taskDao != null) {
                            val entitiesToInsert = mutableListOf<TaskEntity>()
                            for (remoteTask in remoteTasks) {
                                val existingEntity = taskDao.getTaskById(remoteTask.id)
                                if (existingEntity != null && (existingEntity.syncState == SyncState.PENDING || existingEntity.syncState == SyncState.SYNCING || isUuid(existingEntity.id))) {
                                    // Protect local pending/syncing changes or local UUID tasks from being overwritten by GoRest API
                                    continue
                                }
                                entitiesToInsert.add(remoteTask.toEntity(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE))
                            }
                            if (entitiesToInsert.isNotEmpty()) {
                                taskDao.insertTasks(entitiesToInsert)
                            }
                        } else {
                            val merged = (localInMemoryTasks + remoteTasks).distinctBy { it.id }
                            localInMemoryTasks.clear()
                            localInMemoryTasks.addAll(merged)
                            _localInMemoryFlow.value = localInMemoryTasks.toList()
                        }

                        val latestTasks = taskDao?.getAllTasks()?.map { it.toDomainTask() } ?: localInMemoryTasks.toList()
                        NetworkResult.Success(latestTasks)
                    } else {
                        val errorMessage = mapHttpErrorCode(response.code())
                        if (cachedTasks.isNotEmpty()) {
                            NetworkResult.Success(cachedTasks)
                        } else {
                            NetworkResult.Error(errorMessage)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val userFriendlyMessage = mapExceptionToMessage(e)
            if (cachedTasks.isNotEmpty() && currentDemoScenario == DemoScenario.NORMAL_API) {
                NetworkResult.Success(cachedTasks)
            } else {
                NetworkResult.Error(userFriendlyMessage, e)
            }
        }
    }

    override suspend fun addTask(
        title: String,
        description: String,
        priority: TaskPriority
    ): NetworkResult<Task> = withContext(ioDispatcher) {
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            isCompleted = false,
            priority = priority
        )

        var entity = newTask.toEntity(
            syncState = SyncState.PENDING,
            syncOp = SyncOp.CREATE,
            updatedAt = System.currentTimeMillis()
        )

        if (taskDao != null) {
            taskDao.insertTask(entity)
        } else {
            localInMemoryTasks.add(0, newTask)
            _localInMemoryFlow.value = localInMemoryTasks.toList()
        }

        try {
            val dto = TaskDto(
                id = newTask.id,
                title = newTask.title,
                description = newTask.description,
                status = "todo",
                completed = false,
                priority = newTask.priority.name.lowercase()
            )
            val response = apiService.createTask(dto)
            if (response.isSuccessful) {
                entity = entity.copy(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE)
                taskDao?.updateTask(entity)
            }
        } catch (_: Exception) {
            // Retain local PENDING state in Room if remote call fails
        }

        NetworkResult.Success(newTask)
    }

    override suspend fun completeTask(id: String): NetworkResult<Task> = withContext(ioDispatcher) {
        val existingEntity = taskDao?.getTaskById(id)
        if (existingEntity != null) {
            val newSyncOp = if (existingEntity.syncOp == SyncOp.CREATE) SyncOp.CREATE else SyncOp.UPDATE
            val updatedEntity = existingEntity.copy(
                completed = true,
                syncState = SyncState.PENDING,
                syncOp = newSyncOp,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.updateTask(updatedEntity)

            try {
                val response = apiService.updateTaskStatus(id, mapOf("status" to "done"))
                if (response.isSuccessful) {
                    val syncedEntity = updatedEntity.copy(syncState = SyncState.SYNCED, syncOp = SyncOp.NONE)
                    taskDao.updateTask(syncedEntity)
                }
            } catch (_: Exception) {
                // Retain local PENDING update in Room
            }

            return@withContext NetworkResult.Success(updatedEntity.toDomainTask())
        }

        val index = localInMemoryTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = localInMemoryTasks[index]
            val updated = existing.copy(isCompleted = true)
            localInMemoryTasks[index] = updated
            _localInMemoryFlow.value = localInMemoryTasks.toList()

            try {
                apiService.updateTaskStatus(id, mapOf("status" to "done"))
            } catch (_: Exception) {
            }

            return@withContext NetworkResult.Success(updated)
        }

        val fallbackTask = Task(id = id, title = "Task $id", isCompleted = true)
        val fallbackEntity = fallbackTask.toEntity(syncState = SyncState.PENDING, syncOp = SyncOp.CREATE)
        if (taskDao != null) {
            taskDao.insertTask(fallbackEntity)
        } else {
            localInMemoryTasks.add(fallbackTask)
            _localInMemoryFlow.value = localInMemoryTasks.toList()
        }
        NetworkResult.Success(fallbackTask)
    }

    override suspend fun toggleComplete(id: String): NetworkResult<Task> = withContext(ioDispatcher) {
        val existingEntity = taskDao?.getTaskById(id)
        if (existingEntity != null) {
            val newSyncOp = if (existingEntity.syncOp == SyncOp.CREATE) SyncOp.CREATE else SyncOp.UPDATE
            val updatedEntity = existingEntity.copy(
                completed = !existingEntity.completed,
                syncState = SyncState.PENDING,
                syncOp = newSyncOp,
                updatedAt = System.currentTimeMillis()
            )
            taskDao.updateTask(updatedEntity)
            return@withContext NetworkResult.Success(updatedEntity.toDomainTask())
        }

        val index = localInMemoryTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = localInMemoryTasks[index]
            val updated = existing.copy(isCompleted = !existing.isCompleted)
            localInMemoryTasks[index] = updated
            _localInMemoryFlow.value = localInMemoryTasks.toList()
            return@withContext NetworkResult.Success(updated)
        }

        NetworkResult.Error("Task with ID $id not found")
    }

    override fun getTaskById(id: String): Task? {
        return if (taskDao != null) {
            runBlocking(ioDispatcher) {
                taskDao.getTaskById(id)?.toDomainTask()
            }
        } else {
            localInMemoryTasks.find { it.id == id }
        }
    }

    private fun mapHttpErrorCode(code: Int): String {
        return when (code) {
            400 -> "Invalid request submitted to server."
            401, 403 -> "Authentication required to access tasks."
            404 -> "Requested task resource not found."
            500, 502, 503 -> "Server error encountered ($code). Please try again later."
            else -> "Failed to load tasks from server (HTTP $code)."
        }
    }

    private fun mapExceptionToMessage(throwable: Throwable): String {
        return when (throwable) {
            is JsonSyntaxException ->
                "Server returned invalid or malformed data."
            is SocketTimeoutException ->
                "Server connection timed out. Please try again."
            is UnknownHostException, is IOException ->
                "No internet connection. Please check your network and try again."
            is HttpException ->
                "Server returned an error (${throwable.code()}). Please try again."
            else ->
                throwable.localizedMessage ?: "An unexpected error occurred while fetching tasks."
        }
    }

    private fun isUuid(id: String): Boolean {
        return try {
            UUID.fromString(id) != null && id.length == 36
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var instance: ApiTaskRepository? = null

        fun initialize(taskDao: TaskDao) {
            synchronized(this) {
                if (instance == null) {
                    instance = ApiTaskRepository(taskDao = taskDao)
                }
            }
        }

        fun getInstance(taskDao: TaskDao? = null): ApiTaskRepository {
            return instance ?: synchronized(this) {
                instance ?: ApiTaskRepository(taskDao = taskDao).also { instance = it }
            }
        }
    }
}
