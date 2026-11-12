package com.example.a1weektaskproject.data.remote.model

import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Data Transfer Object for API tasks matching REST response schemas (e.g., GoRest API / standard task API).
 */
data class TaskDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("completed") val completed: Boolean? = null,
    @SerializedName("priority") val priority: String? = null,
    @SerializedName("due_on") val dueOn: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    /**
     * Maps TaskDto to clean immutable domain Task model.
     */
    fun toDomainTask(): Task {
        val mappedPriority = when (priority?.lowercase()) {
            "high" -> TaskPriority.HIGH
            "low" -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        val isTaskCompleted = completed == true ||
                status.equals("done", ignoreCase = true) ||
                status.equals("completed", ignoreCase = true)

        val nonNullDueOn = dueOn
        val nonNullCreatedAt = createdAt
        val formatDueDate: String = when {
            !nonNullDueOn.isNullOrBlank() -> nonNullDueOn
            !nonNullCreatedAt.isNullOrBlank() -> nonNullCreatedAt.take(10)
            else -> "Today"
        }

        return Task(
            id = id ?: UUID.randomUUID().toString(),
            title = title ?: "Untitled Task",
            description = description ?: "",
            isCompleted = isTaskCompleted,
            priority = mappedPriority,
            dueDate = formatDueDate
        )
    }
}

/**
 * Wrapper for GoRest API task endpoint response: { "data": [...] }
 */
data class GoRestTaskResponse(
    @SerializedName("data") val data: List<TaskDto>? = null
)
