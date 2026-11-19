package com.example.a1weektaskproject

import android.app.Application
import com.example.a1weektaskproject.data.ApiTaskRepository
import com.example.a1weektaskproject.data.local.AppDatabase
import com.example.a1weektaskproject.sync.WorkManagerScheduler

/**
 * Application subclass initializing Room database singleton and repository instance.
 */
class TaskApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: ApiTaskRepository by lazy {
        ApiTaskRepository.getInstance(database.taskDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ApiTaskRepository.initialize(database.taskDao()) { WorkManagerScheduler.scheduleSync(this) }
        WorkManagerScheduler.scheduleSync(this)
    }

    companion object {
        lateinit var instance: TaskApplication
            private set
    }
}
