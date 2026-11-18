package com.example.a1weektaskproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.data.remote.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskDetailsViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    private val _isCompleting = MutableStateFlow(false)
    val isCompleting: StateFlow<Boolean> = _isCompleting.asStateFlow()

    fun initTask(initialTask: Task) {
        if (_task.value == null) {
            _task.value = repository.getTaskById(initialTask.id) ?: initialTask
        }
    }

    fun completeTask() {
        val current = _task.value ?: return
        if (current.isCompleted) return

        synchronized(this) {
            if (_isCompleting.value) return
            _isCompleting.value = true
        }

        viewModelScope.launch {
            try {
                val result = repository.completeTask(current.id)
                if (result is NetworkResult.Success) {
                    _task.value = result.data
                } else {
                    _task.value = current.copy(isCompleted = true)
                }
            } finally {
                _isCompleting.value = false
            }
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskDetailsViewModel(repository) as T
        }
    }
}
