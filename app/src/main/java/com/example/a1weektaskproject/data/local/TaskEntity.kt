package com.example.a1weektaskproject.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority

object SyncState {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
}

object SyncOp {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val NONE = "NONE"
}

/**
 * Room Entity modeling task data with offline synchronization lifecycle state.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val completed: Boolean = false,
    val priority: String = "MEDIUM",
    val dueDate: String = "Today",
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: String = SyncState.SYNCED,
    val syncOp: String = SyncOp.NONE
) {
    /**
     * Maps TaskEntity to immutable domain Task model.
     */
    fun toDomainTask(): Task {
        val mappedPriority = try {
            TaskPriority.valueOf(priority.uppercase())
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }
        return Task(
            id = id,
            title = title,
            description = description,
            isCompleted = completed,
            priority = mappedPriority,
            dueDate = dueDate
        )
    }
}

/**
 * Extension function mapping domain Task model to Room TaskEntity.
 */
fun Task.toEntity(
    syncState: String = SyncState.SYNCED,
    syncOp: String = SyncOp.NONE,
    updatedAt: Long = System.currentTimeMillis()
): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        completed = isCompleted,
        priority = priority.name,
        dueDate = dueDate,
        updatedAt = updatedAt,
        syncState = syncState,
        syncOp = syncOp
    )
}
