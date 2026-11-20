package com.example.a1weektaskproject.sync

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import java.util.concurrent.TimeUnit

class WorkManagerSchedulerTest {

    @Test
    fun scheduleSync_enqueuesUniqueConnectedWorkWithExponentialBackoff() {
        val workManager = mock(WorkManager::class.java)

        WorkManagerScheduler.scheduleSync(workManager)

        val invocation = mockingDetails(workManager).invocations.single()
        assertEquals("enqueueUniqueWork", invocation.method.name)
        assertEquals(WorkManagerScheduler.WORK_NAME, invocation.arguments[0])
        assertEquals(ExistingWorkPolicy.APPEND_OR_REPLACE, invocation.arguments[1])
        val workSpec = (invocation.arguments[2] as OneTimeWorkRequest).workSpec
        assertEquals(TaskSyncWorker::class.java.name, workSpec.workerClassName)
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)
        assertEquals(TimeUnit.SECONDS.toMillis(10), workSpec.backoffDelayDuration)
    }
}
