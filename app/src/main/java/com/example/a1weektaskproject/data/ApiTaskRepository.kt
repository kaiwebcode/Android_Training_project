package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.remote.ApiClient
import com.example.a1weektaskproject.data.remote.NetworkResult
import com.example.a1weektaskproject.data.remote.TaskApiService
import com.example.a1weektaskproject.data.remote.model.TaskDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

/**
 * REST API Repository implementation with exception handling and Dispatcher injection.
 */
class ApiTaskRepository(
    private val apiService: TaskApiService = ApiClient.taskApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    private val localTasks = mutableListOf<Task>()

    override suspend fun getTasks(): NetworkResult<List<Task>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getTasks()
            if (response.isSuccessful) {
                val dtoList = response.body()?.data ?: emptyList()
                val remoteTasks = dtoList.map { it.toDomainTask() }

                // Merge remote tasks with any locally created/updated tasks
                val mergedTasks = (localTasks + remoteTasks).distinctBy { it.id }
                localTasks.clear()
                localTasks.addAll(mergedTasks)

                NetworkResult.Success(localTasks.toList())
            } else {
                val errorMessage = mapHttpErrorCode(response.code())
                if (localTasks.isNotEmpty()) {
                    NetworkResult.Success(localTasks.toList())
                } else {
                    NetworkResult.Error(errorMessage)
                }
            }
        } catch (e: Exception) {
            val userFriendlyMessage = mapExceptionToMessage(e)
            if (localTasks.isNotEmpty()) {
                NetworkResult.Success(localTasks.toList())
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
            id = "TASK-" + UUID.randomUUID().toString().take(6).uppercase(),
            title = title,
            description = description,
            isCompleted = false,
            priority = priority
        )

        localTasks.add(0, newTask)

        try {
            val dto = TaskDto(
                id = newTask.id,
                title = newTask.title,
                description = newTask.description,
                status = "todo",
                completed = false,
                priority = newTask.priority.name.lowercase()
            )
            apiService.createTask(dto)
        } catch (_: Exception) {
            // Retain local creation even if remote POST fails
        }

        NetworkResult.Success(newTask)
    }

    override suspend fun completeTask(id: String): NetworkResult<Task> = withContext(ioDispatcher) {
        val index = localTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = localTasks[index]
            val updated = existing.copy(isCompleted = true)
            localTasks[index] = updated

            try {
                apiService.updateTaskStatus(id, mapOf("status" to "done"))
            } catch (_: Exception) {
                // Retain local completion status
            }

            NetworkResult.Success(updated)
        } else {
            // If not found in memory list yet, add updated completed task
            val updated = Task(id = id, title = "Task $id", isCompleted = true)
            localTasks.add(updated)
            NetworkResult.Success(updated)
        }
    }

    override suspend fun toggleComplete(id: String): NetworkResult<Task> = withContext(ioDispatcher) {
        val index = localTasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = localTasks[index]
            val updated = existing.copy(isCompleted = !existing.isCompleted)
            localTasks[index] = updated
            NetworkResult.Success(updated)
        } else {
            NetworkResult.Error("Task with ID $id not found")
        }
    }

    override fun getTaskById(id: String): Task? {
        return localTasks.find { it.id == id }
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

    companion object {
        @Volatile
        private var instance: ApiTaskRepository? = null

        fun getInstance(): ApiTaskRepository {
            return instance ?: synchronized(this) {
                instance ?: ApiTaskRepository().also { instance = it }
            }
        }
    }
}
