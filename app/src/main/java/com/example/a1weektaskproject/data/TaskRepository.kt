package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.remote.NetworkResult

/**
 * Interface defining data operations for task management.
 */
interface TaskRepository {
    suspend fun getTasks(): NetworkResult<List<Task>>
    suspend fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM): NetworkResult<Task>
    suspend fun completeTask(id: String): NetworkResult<Task>
    suspend fun toggleComplete(id: String): NetworkResult<Task>
    fun getTaskById(id: String): Task?
}
