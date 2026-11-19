package com.example.a1weektaskproject.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database task operations.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    @Transaction
    suspend fun cacheGoRestTask(remoteTask: TaskEntity) {
        val existing = getTaskById(remoteTask.id)
        when {
            existing == null -> insertTask(remoteTask)
            existing.origin == TaskOrigin.UNKNOWN -> confirmGoRestOrigin(remoteTask.id)
            existing.origin == TaskOrigin.GOREST && existing.syncState == SyncState.SYNCED ->
                updateTask(remoteTask)
        }
    }

    @Query("UPDATE tasks SET origin = 'GOREST' WHERE id = :id AND origin = 'UNKNOWN'")
    suspend fun confirmGoRestOrigin(id: String)

    @Transaction
    suspend fun editCompletion(id: String, completed: Boolean?, updatedAt: Long): TaskEntity? {
        val current = getTaskById(id) ?: return null
        val edited = current.copy(
            completed = completed ?: !current.completed,
            syncState = SyncState.PENDING,
            syncOp = if (current.syncOp == SyncOp.CREATE) SyncOp.CREATE else SyncOp.UPDATE,
            updatedAt = updatedAt
        )
        updateTask(edited)
        return edited
    }

    @Query("SELECT * FROM tasks WHERE origin = 'LOCAL' AND syncState = 'PENDING' AND syncOp IN ('CREATE', 'UPDATE') ORDER BY updatedAt ASC")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncState = 'SYNCING' WHERE id IN (:ids) AND origin = 'LOCAL' AND syncState = 'PENDING' AND syncOp IN ('CREATE', 'UPDATE')")
    suspend fun markTasksSyncing(ids: List<String>): Int

    @Transaction
    suspend fun claimForSync(id: String): TaskEntity? {
        if (markTasksSyncing(listOf(id)) == 0) return null
        return getTaskById(id)
    }

    @Query("UPDATE tasks SET syncState = 'PENDING' WHERE origin = 'LOCAL' AND syncState = 'SYNCING'")
    suspend fun resetSyncingToPending(): Int

    @Transaction
    suspend fun acknowledgeMockSync(submitted: TaskEntity, needsPatch: Boolean): Boolean {
        val current = getTaskById(submitted.id) ?: return false
        if (current.origin != TaskOrigin.LOCAL || current.syncState == SyncState.SYNCED) return false

        val unchanged = current == submitted
        // The server has this UUID. Keep any newer local data and send it as a PATCH.
        if (submitted.syncOp == SyncOp.CREATE && current.syncOp == SyncOp.CREATE && (needsPatch || !unchanged)) {
            updateTask(current.copy(syncState = SyncState.PENDING, syncOp = SyncOp.UPDATE))
            return false
        }
        return unchanged && markTaskSyncedConditional(submitted.id, submitted.updatedAt) == 1
    }

    @Query("UPDATE tasks SET syncState = 'SYNCED', syncOp = 'NONE' WHERE id = :id AND updatedAt = :capturedUpdatedAt")
    suspend fun markTaskSyncedConditional(id: String, capturedUpdatedAt: Long): Int

    @Query("UPDATE tasks SET syncState = 'PENDING' WHERE id = :id AND origin = 'LOCAL' AND syncState = 'SYNCING'")
    suspend fun revertTaskSyncingToPending(id: String): Int
}
