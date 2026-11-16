package com.example.a1weektaskproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.a1weektaskproject.data.ApiTaskRepository
import com.example.a1weektaskproject.data.DemoScenario
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
        observeTasksFlow()
        loadTasks()
    }

    private fun observeTasksFlow() {
        viewModelScope.launch {
            repository.getTasksFlow().collect { tasks ->
                val currentState = _uiState.value
                if (tasks.isNotEmpty()) {
                    val total = tasks.size
                    val completed = tasks.count { it.isCompleted }
                    val pending = total - completed
                    val newlyCompleted = if (currentState is TaskListUiState.Content) currentState.newlyCompletedTask else null
                    val validationErr = if (currentState is TaskListUiState.Content) currentState.validationError else null

                    _uiState.value = TaskListUiState.Content(
                        tasks = tasks,
                        totalCount = total,
                        completedCount = completed,
                        pendingCount = pending,
                        newlyCompletedTask = newlyCompleted,
                        validationError = validationErr
                    )
                } else if (currentState is TaskListUiState.Content) {
                    _uiState.value = TaskListUiState.Empty
                }
            }
        }
    }

    fun loadTasks() {
        if (_uiState.value !is TaskListUiState.Content) {
            _uiState.value = TaskListUiState.Loading
        }
        viewModelScope.launch {
            when (val result = repository.getTasks()) {
                is NetworkResult.Success -> {
                    val tasks = result.data
                    if (tasks.isEmpty() && _uiState.value !is TaskListUiState.Content) {
                        _uiState.value = TaskListUiState.Empty
                    }
                }
                is NetworkResult.Error -> {
                    if (_uiState.value !is TaskListUiState.Content) {
                        _uiState.value = TaskListUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun retryFetch() {
        if (repository is ApiTaskRepository) {
            repository.currentDemoScenario = DemoScenario.NORMAL_API
        }
        loadTasks()
    }

    fun setDemoScenario(scenario: DemoScenario) {
        if (repository is ApiTaskRepository) {
            repository.currentDemoScenario = scenario
        }
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
                val currentState = _uiState.value
                if (currentState is TaskListUiState.Content && wasIncomplete && updatedTask.isCompleted) {
                    _uiState.value = currentState.copy(newlyCompletedTask = updatedTask)
                }
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
                val currentState = _uiState.value
                if (currentState is TaskListUiState.Content && wasIncomplete && updatedTask.isCompleted) {
                    _uiState.value = currentState.copy(newlyCompletedTask = updatedTask)
                }
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
