package com.example.a1weektaskproject.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Singleton helper scheduling unique WorkManager background synchronization tasks.
 */
object WorkManagerScheduler {

    const val WORK_NAME = "EmployeeTaskSyncWorker"

    fun scheduleSync(context: Context) {
        scheduleSync(WorkManager.getInstance(context.applicationContext))
    }

    internal fun scheduleSync(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncWorkRequest
        )
    }
}
