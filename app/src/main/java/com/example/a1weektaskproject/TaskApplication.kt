package com.example.a1weektaskproject

import android.app.Application
import com.example.a1weektaskproject.data.ApiTaskRepository
import com.example.a1weektaskproject.data.local.AppDatabase

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
        ApiTaskRepository.initialize(database.taskDao())
    }
}
