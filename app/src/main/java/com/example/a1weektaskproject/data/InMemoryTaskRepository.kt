package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.remote.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class InMemoryTaskRepository : TaskRepository {

    private val tasksList = mutableListOf<Task>()
    private val idCounter = AtomicInteger(1)
    private val _tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    init {
        // Pre-populate initial sample tasks matching the UI design specs
        addTaskSync("Prepare weekly report", "Review employee performance", TaskPriority.MEDIUM)
        addTaskSync("Update project documentation", "Check latest changes", TaskPriority.LOW)
        val teamMeeting = addTaskSync("Team meeting notes", "Share summary with team", TaskPriority.MEDIUM)
        completeTaskSync(teamMeeting.id)
        addTaskSync("Fix login issue", "Resolve authentication bug", TaskPriority.HIGH)
    }

    private fun addTaskSync(title: String, description: String, priority: TaskPriority): Task {
        val nextId = String.format(Locale.US, "TASK-%03d", idCounter.getAndIncrement())
        val newTask = Task(
            id = nextId,
            title = title,
            description = description,
            isCompleted = false,
            priority = priority
        )
        tasksList.add(newTask)
        _tasksFlow.value = tasksList.toList()
        return newTask
    }

    private fun completeTaskSync(id: String): Boolean {
        val index = tasksList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = tasksList[index]
            if (!existing.isCompleted) {
                tasksList[index] = existing.copy(isCompleted = true)
                _tasksFlow.value = tasksList.toList()
                return true
            }
        }
        return false
    }

    override fun getTasksFlow(): Flow<List<Task>> = _tasksFlow.asStateFlow()

    override suspend fun getTasks(): NetworkResult<List<Task>> {
        return NetworkResult.Success(tasksList.toList())
    }

    override suspend fun addTask(title: String, description: String, priority: TaskPriority): NetworkResult<Task> {
        val newTask = addTaskSync(title, description, priority)
        return NetworkResult.Success(newTask)
    }

    override suspend fun completeTask(id: String): NetworkResult<Task> {
        val index = tasksList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = tasksList[index]
            val updated = existing.copy(isCompleted = true)
            tasksList[index] = updated
            _tasksFlow.value = tasksList.toList()
            return NetworkResult.Success(updated)
        }
        return NetworkResult.Error("Task not found")
    }

    override suspend fun toggleComplete(id: String): NetworkResult<Task> {
        val index = tasksList.indexOfFirst { it.id == id }
        if (index != -1) {
            val existing = tasksList[index]
            val updated = existing.copy(isCompleted = !existing.isCompleted)
            tasksList[index] = updated
            _tasksFlow.value = tasksList.toList()
            return NetworkResult.Success(updated)
        }
        return NetworkResult.Error("Task not found")
    }

    override fun getTaskById(id: String): Task? {
        return tasksList.find { it.id == id }
    }

    companion object {
        @Volatile
        private var instance: InMemoryTaskRepository? = null

        fun getInstance(): InMemoryTaskRepository {
            return instance ?: synchronized(this) {
                instance ?: InMemoryTaskRepository().also { instance = it }
            }
        }
    }
}
