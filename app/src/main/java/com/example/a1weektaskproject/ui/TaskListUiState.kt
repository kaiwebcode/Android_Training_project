package com.example.a1weektaskproject.ui

import com.example.a1weektaskproject.data.Task

/**
 * Type-safe sealed interface modeling the 4 mandatory UI states for API-backed task management:
 * 1. Loading
 * 2. Content (Success)
 * 3. Empty
 * 4. Error
 */
sealed interface TaskListUiState {
    data object Loading : TaskListUiState

    data class Content(
        val tasks: List<Task>,
        val totalCount: Int,
        val completedCount: Int,
        val pendingCount: Int,
        val newlyCompletedTask: Task? = null,
        val validationError: String? = null
    ) : TaskListUiState

    data object Empty : TaskListUiState

    data class Error(
        val message: String
    ) : TaskListUiState
}
