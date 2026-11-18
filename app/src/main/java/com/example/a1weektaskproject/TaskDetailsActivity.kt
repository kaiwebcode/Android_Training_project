package com.example.a1weektaskproject

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.ui.TaskDetailsViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class TaskDetailsActivity : AppCompatActivity() {

    private val repository: TaskRepository by lazy {
        (application as TaskApplication).repository
    }
    private val viewModel: TaskDetailsViewModel by viewModels {
        TaskDetailsViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: ""
        val description = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""
        val priorityName = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: TaskPriority.MEDIUM.name
        val isCompleted = intent.getBooleanExtra(EXTRA_TASK_STATUS, false)
        val dueDate = intent.getStringExtra(EXTRA_TASK_DUE_DATE) ?: "Today"

        val mappedPriority = try {
            TaskPriority.valueOf(priorityName)
        } catch (_: Exception) {
            TaskPriority.MEDIUM
        }

        val initialTask = Task(
            id = taskId,
            title = if (title.isNotEmpty()) title else "Task Details",
            description = description,
            isCompleted = isCompleted,
            priority = mappedPriority,
            dueDate = dueDate
        )
        viewModel.initTask(initialTask)

        val backButton = findViewById<ImageView>(R.id.backButton)
        val detailTitle = findViewById<TextView>(R.id.detailTitle)
        val detailSubtitle = findViewById<TextView>(R.id.detailSubtitle)
        val detailStatusBadge = findViewById<TextView>(R.id.detailStatusBadge)
        val detailDescriptionValue = findViewById<TextView>(R.id.detailDescriptionValue)
        val detailPriorityValue = findViewById<TextView>(R.id.detailPriorityValue)
        val detailDueDateValue = findViewById<TextView>(R.id.detailDueDateValue)
        val markCompleteButton = findViewById<MaterialButton>(R.id.markCompleteButton)

        backButton.setOnClickListener {
            finish()
        }

        markCompleteButton.setOnClickListener {
            viewModel.completeTask()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.task.collect { task ->
                        task?.let {
                            detailTitle.text = it.title
                            detailSubtitle.text = if (it.description.isNotEmpty()) it.description else "Task ID: ${it.id}"
                            detailDescriptionValue.text = if (it.description.isNotEmpty()) it.description else "No additional description provided."
                            detailPriorityValue.text = "● ${it.priority.name.lowercase().replaceFirstChar { char -> char.uppercase() }}"
                            when (it.priority) {
                                TaskPriority.LOW -> detailPriorityValue.setTextColor(android.graphics.Color.parseColor("#6C5CE7"))
                                TaskPriority.MEDIUM -> detailPriorityValue.setTextColor(android.graphics.Color.parseColor("#E65100"))
                                TaskPriority.HIGH -> detailPriorityValue.setTextColor(android.graphics.Color.parseColor("#D63031"))
                            }
                            detailDueDateValue.text = it.dueDate

                            updateStatusUI(it, viewModel.isCompleting.value, detailStatusBadge, markCompleteButton)
                        }
                    }
                }
                launch {
                    viewModel.isCompleting.collect { isCompleting ->
                        viewModel.task.value?.let { task ->
                            updateStatusUI(task, isCompleting, detailStatusBadge, markCompleteButton)
                        }
                    }
                }
            }
        }
    }

    private fun updateStatusUI(task: Task, isCompleting: Boolean, badge: TextView, button: MaterialButton) {
        if (task.isCompleted) {
            badge.text = "Completed"
            badge.setBackgroundResource(R.drawable.bg_status_completed)
            badge.setTextColor(android.graphics.Color.parseColor("#00B894"))
            button.isEnabled = false
            button.text = "Completed"
        } else if (isCompleting) {
            badge.text = "Updating..."
            badge.setBackgroundResource(R.drawable.bg_status_pending)
            badge.setTextColor(android.graphics.Color.parseColor("#FF7675"))
            button.isEnabled = false
            button.text = "⏳ Marking as Complete..."
        } else {
            badge.text = "Pending"
            badge.setBackgroundResource(R.drawable.bg_status_pending)
            badge.setTextColor(android.graphics.Color.parseColor("#FF7675"))
            button.isEnabled = true
            button.text = "✓ Mark as Complete"
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.example.a1weektaskproject.EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "com.example.a1weektaskproject.EXTRA_TASK_TITLE"
        const val EXTRA_TASK_DESC = "com.example.a1weektaskproject.EXTRA_TASK_DESC"
        const val EXTRA_TASK_PRIORITY = "com.example.a1weektaskproject.EXTRA_TASK_PRIORITY"
        const val EXTRA_TASK_STATUS = "com.example.a1weektaskproject.EXTRA_TASK_STATUS"
        const val EXTRA_TASK_DUE_DATE = "com.example.a1weektaskproject.EXTRA_TASK_DUE_DATE"
    }
}
