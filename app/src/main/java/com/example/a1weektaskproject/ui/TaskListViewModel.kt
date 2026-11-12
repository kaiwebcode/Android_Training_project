package com.example.a1weektaskproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.a1weektaskproject.data.ApiTaskRepository
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository = ApiTaskRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskListUiState>(TaskListUiState.Loading)
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks() {
        _uiState.value = TaskListUiState.Loading
        viewModelScope.launch {
            when (val result = repository.getTasks()) {
                is NetworkResult.Success -> {
                    val tasks = result.data
                    if (tasks.isEmpty()) {
                        _uiState.value = TaskListUiState.Empty
                    } else {
                        val total = tasks.size
                        val completed = tasks.count { it.isCompleted }
                        val pending = total - completed
                        _uiState.value = TaskListUiState.Content(
                            tasks = tasks,
                            totalCount = total,
                            completedCount = completed,
                            pendingCount = pending
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = TaskListUiState.Error(result.message)
                }
            }
        }
    }

    fun retryFetch() {
        loadTasks()
    }

    fun addTask(rawTitle: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM) {
        val trimmedTitle = rawTitle.trim()

        if (trimmedTitle.isEmpty()) {
            val currentState = _uiState.value
            if (currentState is TaskListUiState.Content) {
                _uiState.value = currentState.copy(validationError = "Task title cannot be empty")
            }
            return
        }

        viewModelScope.launch {
            repository.addTask(trimmedTitle, description.trim(), priority)
            loadTasks()
        }
    }

    fun completeTask(id: String) {
        val taskBefore = repository.getTaskById(id)
        val wasIncomplete = taskBefore != null && !taskBefore.isCompleted

        viewModelScope.launch {
            val result = repository.completeTask(id)
            if (result is NetworkResult.Success) {
                val updatedTask = result.data
                val currentTasksRes = repository.getTasks()
                val currentTasks = if (currentTasksRes is NetworkResult.Success) currentTasksRes.data else emptyList()

                val total = currentTasks.size
                val completed = currentTasks.count { it.isCompleted }
                val pending = total - completed

                _uiState.value = TaskListUiState.Content(
                    tasks = currentTasks,
                    totalCount = total,
                    completedCount = completed,
                    pendingCount = pending,
                    newlyCompletedTask = if (wasIncomplete && updatedTask.isCompleted) updatedTask else null
                )
            }
        }
    }

    fun toggleComplete(id: String) {
        val taskBefore = repository.getTaskById(id)
        val wasIncomplete = taskBefore != null && !taskBefore.isCompleted

        viewModelScope.launch {
            val result = repository.toggleComplete(id)
            if (result is NetworkResult.Success) {
                val updatedTask = result.data
                val currentTasksRes = repository.getTasks()
                val currentTasks = if (currentTasksRes is NetworkResult.Success) currentTasksRes.data else emptyList()

                val total = currentTasks.size
                val completed = currentTasks.count { it.isCompleted }
                val pending = total - completed

                _uiState.value = TaskListUiState.Content(
                    tasks = currentTasks,
                    totalCount = total,
                    completedCount = completed,
                    pendingCount = pending,
                    newlyCompletedTask = if (wasIncomplete && updatedTask.isCompleted) updatedTask else null
                )
            }
        }
    }

    fun dismissCompletionDialog() {
        val currentState = _uiState.value
        if (currentState is TaskListUiState.Content) {
            _uiState.value = currentState.copy(newlyCompletedTask = null)
        }
    }

    fun clearValidationError() {
        val currentState = _uiState.value
        if (currentState is TaskListUiState.Content) {
            _uiState.value = currentState.copy(validationError = null)
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskListViewModel(repository) as T
        }
    }
}
