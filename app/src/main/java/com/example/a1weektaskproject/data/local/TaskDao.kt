package com.example.a1weektaskproject.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM tasks WHERE syncState = 'PENDING' OR syncState = 'SYNCING' ORDER BY updatedAt ASC")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET syncState = 'SYNCING' WHERE id IN (:ids) AND (syncState = 'PENDING' OR syncState = 'SYNCING')")
    suspend fun markTasksSyncing(ids: List<String>): Int

    @Query("UPDATE tasks SET syncState = 'PENDING' WHERE syncState = 'SYNCING'")
    suspend fun resetSyncingToPending(): Int

    @Query("UPDATE tasks SET syncState = 'SYNCED', syncOp = 'NONE' WHERE id = :id AND updatedAt = :capturedUpdatedAt")
    suspend fun markTaskSyncedConditional(id: String, capturedUpdatedAt: Long): Int

    @Query("UPDATE tasks SET syncState = 'PENDING' WHERE id = :id AND syncState = 'SYNCING'")
    suspend fun revertTaskSyncingToPending(id: String): Int
}
