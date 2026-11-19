package com.example.a1weektaskproject

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class IsolatedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        // Keep TaskApplication from opening the installed task database or scheduling real uploads.
        return super.newApplication(cl, Application::class.java.name, context)
    }
}
