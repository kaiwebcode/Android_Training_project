package com.example.a1weektaskproject.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.a1weektaskproject.TaskApplication
import com.example.a1weektaskproject.data.local.SyncOp
import com.example.a1weektaskproject.data.local.TaskDao
import com.example.a1weektaskproject.data.remote.ApiSyncClient
import com.example.a1weektaskproject.data.remote.TaskSyncApiService
import com.example.a1weektaskproject.data.remote.TaskSyncDto
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

import com.example.a1weektaskproject.data.remote.TaskSyncUpdateDto

/**
 * WorkManager CoroutineWorker performing reliable background synchronization of pending tasks with mock sync server.
 */
class TaskSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val app = applicationContext as? TaskApplication
    var customTaskDao: TaskDao? = null
    var customSyncApiService: TaskSyncApiService? = null

    private val taskDao: TaskDao?
        get() = customTaskDao ?: app?.database?.taskDao()

    private val syncApiService: TaskSyncApiService?
        get() = customSyncApiService ?: ApiSyncClient.taskSyncApiService

    override suspend fun doWork(): Result {
        logD(TAG, "TaskSyncWorker.doWork() started...")
        val dao = taskDao ?: run {
            logE(TAG, "TaskDao is null in TaskSyncWorker")
            return Result.failure()
        }
        val api = syncApiService ?: run {
            logE(TAG, "TaskSyncApiService is null in TaskSyncWorker")
            return Result.failure()
        }
        return performSync(dao, api)
    }

    companion object {
        private const val TAG = "TaskSyncWorker"

        private fun logD(tag: String, msg: String) {
            try {
                Log.d(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] $msg")
            }
        }

        private fun logE(tag: String, msg: String, tr: Throwable? = null) {
            try {
                Log.e(tag, msg, tr)
            } catch (_: Throwable) {
                println("[$tag ERROR] $msg ${tr?.message ?: ""}")
            }
        }

        suspend fun performSync(dao: TaskDao, api: TaskSyncApiService): Result {
            logD(TAG, "performSync() executing...")
            // 1. Crash Recovery: Revert any stuck SYNCING tasks back to PENDING
            dao.resetSyncingToPending()

            // 2. Fetch all PENDING or SYNCING tasks
            val pendingTasks = dao.getPendingSyncTasks()
            logD(TAG, "Found ${pendingTasks.size} pending tasks to synchronize.")
            if (pendingTasks.isEmpty()) {
                return Result.success()
            }

            var hasTransientFailure = false
            var hasPendingRevertedTasks = false

            for (candidate in pendingTasks) {
                val task = dao.claimForSync(candidate.id) ?: continue
                logD(TAG, "Syncing task clientTaskId=${task.id}, title='${task.title}', syncOp=${task.syncOp}")

                try {
                    val createDto = if (task.syncOp == SyncOp.CREATE) {
                        TaskSyncDto(
                            clientTaskId = task.id,
                            title = task.title.trim(),
                            description = task.description.trim(),
                            completed = task.completed,
                            priority = task.priority.ifEmpty { "MEDIUM" },
                            dueDate = task.dueDate.ifEmpty { "Today" }
                        )
                    } else {
                        null
                    }
                    val response = if (createDto != null) {
                        api.createTask(createDto)
                    } else {
                        val updateDto = TaskSyncUpdateDto(
                            completed = task.completed,
                            title = task.title,
                            description = task.description,
                            priority = task.priority,
                            dueDate = task.dueDate
                        )
                        api.updateTaskStatus(task.id, updateDto)
                    }

                    if (response.isSuccessful) {
                        val acknowledged = response.body()?.task
                        if (createDto != null && acknowledged?.clientTaskId != task.id) {
                            logE(TAG, "Mock CREATE response did not acknowledge clientTaskId=${task.id}")
                            dao.revertTaskSyncingToPending(task.id)
                            hasTransientFailure = true
                            continue
                        }
                        val needsPatch = createDto != null &&
                            acknowledged?.copy(version = createDto.version) != createDto
                        if (dao.acknowledgeMockSync(task, needsPatch)) {
                            logD(TAG, "Successfully synced task clientTaskId=${task.id} with mock server.")
                        } else {
                            logD(TAG, "Retrying unacknowledged changes for clientTaskId=${task.id}.")
                            dao.revertTaskSyncingToPending(task.id)
                            hasPendingRevertedTasks = true
                        }
                    } else {
                        val code = response.code()
                        logE(TAG, "Mock server returned error code $code for clientTaskId=${task.id}")
                        if (code in 500..599 || code == 408) {
                            dao.revertTaskSyncingToPending(task.id)
                            hasTransientFailure = true
                        } else {
                            dao.revertTaskSyncingToPending(task.id)
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logE(TAG, "Exception during sync of clientTaskId=${task.id}: ${e.message}", e)
                    dao.revertTaskSyncingToPending(task.id)
                    when (e) {
                        is SocketTimeoutException, is IOException -> {
                            hasTransientFailure = true
                        }
                        is HttpException -> {
                            if (e.code() in 500..599) {
                                hasTransientFailure = true
                            }
                        }
                        else -> {
                            hasTransientFailure = true
                        }
                    }
                }
            }

            return if (hasTransientFailure || hasPendingRevertedTasks) {
                logD(TAG, "Sync completed with transient failures or pending local edits. Requesting WorkManager retry.")
                Result.retry()
            } else {
                logD(TAG, "Sync completed successfully for all tasks.")
                Result.success()
            }
        }
    }
}
