package com.example.a1weektaskproject

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.a1weektaskproject.data.DemoScenario
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.ui.TaskAdapter
import com.example.a1weektaskproject.ui.TaskListUiState
import com.example.a1weektaskproject.ui.TaskListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/**
 * Main Activity for Employee Task Manager rendering all 4 UI states (Loading, Content, Empty, Error).
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: TaskListViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter

    private lateinit var totalCountTextView: TextView
    private lateinit var completedCountTextView: TextView
    private lateinit var pendingCountTextView: TextView

    private lateinit var menuIcon: ImageView
    private lateinit var loadingStateView: View
    private lateinit var emptyStateView: View
    private lateinit var emptyStateAddButton: MaterialButton
    private lateinit var errorStateView: View
    private lateinit var errorMessageTextView: TextView
    private lateinit var retryButton: MaterialButton
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var addFabButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadTasks()
    }

    private fun initViews() {
        menuIcon = findViewById(R.id.menuIcon)
        totalCountTextView = findViewById(R.id.totalCountTextView)
        completedCountTextView = findViewById(R.id.completedCountTextView)
        pendingCountTextView = findViewById(R.id.pendingCountTextView)

        loadingStateView = findViewById(R.id.loadingStateView)
        emptyStateView = findViewById(R.id.emptyStateView)
        emptyStateAddButton = findViewById(R.id.emptyStateAddButton)
        errorStateView = findViewById(R.id.errorStateView)
        errorMessageTextView = findViewById(R.id.errorMessageTextView)
        retryButton = findViewById(R.id.retryButton)
        taskRecyclerView = findViewById(R.id.taskRecyclerView)
        addFabButton = findViewById(R.id.addFabButton)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onToggleComplete = { task ->
                viewModel.completeTask(task.id)
            },
            onItemClick = { task ->
                val intent = Intent(this, TaskDetailsActivity::class.java).apply {
                    putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.id)
                    putExtra(TaskDetailsActivity.EXTRA_TASK_TITLE, task.title)
                    putExtra(TaskDetailsActivity.EXTRA_TASK_DESC, task.description)
                    putExtra(TaskDetailsActivity.EXTRA_TASK_PRIORITY, task.priority.name)
                    putExtra(TaskDetailsActivity.EXTRA_TASK_STATUS, task.isCompleted)
                    putExtra(TaskDetailsActivity.EXTRA_TASK_DUE_DATE, task.dueDate)
                }
                startActivity(intent)
            }
        )
        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = taskAdapter
    }

    private fun setupListeners() {
        menuIcon.setOnClickListener {
            showDemoScenarioBottomSheet()
        }

        addFabButton.setOnClickListener {
            showAddTaskBottomSheet()
        }

        emptyStateAddButton.setOnClickListener {
            showAddTaskBottomSheet()
        }

        retryButton.setOnClickListener {
            viewModel.retryFetch()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TaskListUiState.Loading -> {
                            loadingStateView.visibility = View.VISIBLE
                            emptyStateView.visibility = View.GONE
                            errorStateView.visibility = View.GONE
                            taskRecyclerView.visibility = View.GONE
                        }
                        is TaskListUiState.Content -> {
                            loadingStateView.visibility = View.GONE
                            emptyStateView.visibility = View.GONE
                            errorStateView.visibility = View.GONE
                            taskRecyclerView.visibility = View.VISIBLE

                            taskAdapter.submitList(state.tasks)
                            totalCountTextView.text = state.totalCount.toString()
                            completedCountTextView.text = state.completedCount.toString()
                            pendingCountTextView.text = state.pendingCount.toString()

                            state.newlyCompletedTask?.let {
                                showTaskCompletedDialog()
                            }
                        }
                        is TaskListUiState.Empty -> {
                            loadingStateView.visibility = View.GONE
                            emptyStateView.visibility = View.VISIBLE
                            errorStateView.visibility = View.GONE
                            taskRecyclerView.visibility = View.GONE

                            totalCountTextView.text = "0"
                            completedCountTextView.text = "0"
                            pendingCountTextView.text = "0"
                        }
                        is TaskListUiState.Error -> {
                            loadingStateView.visibility = View.GONE
                            emptyStateView.visibility = View.GONE
                            errorStateView.visibility = View.VISIBLE
                            taskRecyclerView.visibility = View.GONE

                            errorMessageTextView.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun showDemoScenarioBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_demo_scenario, null)
        bottomSheet.setContentView(dialogView)

        val btnNormal = dialogView.findViewById<MaterialButton>(R.id.demoNormalApiButton)
        val btnMalformed = dialogView.findViewById<MaterialButton>(R.id.demoMalformedJsonButton)
        val btnHttp500 = dialogView.findViewById<MaterialButton>(R.id.demoHttp500Button)
        val btnTimeout = dialogView.findViewById<MaterialButton>(R.id.demoTimeoutButton)
        val btnUiSync = dialogView.findViewById<MaterialButton>(R.id.demoUiSyncButton)
        val btnEmpty = dialogView.findViewById<MaterialButton>(R.id.demoEmptyStateButton)

        btnNormal.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.NORMAL_API)
            bottomSheet.dismiss()
        }

        btnMalformed.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.MALFORMED_JSON)
            bottomSheet.dismiss()
        }

        btnHttp500.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.HTTP_500_SERVER_ERROR)
            bottomSheet.dismiss()
        }

        btnTimeout.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.NETWORK_TIMEOUT)
            bottomSheet.dismiss()
        }

        btnUiSync.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.UI_STALE_STATE_SYNC)
            bottomSheet.dismiss()
        }

        btnEmpty.setOnClickListener {
            viewModel.setDemoScenario(DemoScenario.EMPTY_RESPONSE)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun showAddTaskBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_add_task, null)
        bottomSheet.setContentView(dialogView)

        val titleEditText = dialogView.findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        val titleCharCount = dialogView.findViewById<TextView>(R.id.titleCharCount)
        val descriptionCharCount = dialogView.findViewById<TextView>(R.id.descriptionCharCount)

        titleEditText.doOnTextChanged { text, _, _, _ ->
            titleCharCount.text = "${text?.length ?: 0}/100"
        }

        descriptionEditText.doOnTextChanged { text, _, _, _ ->
            descriptionCharCount.text = "${text?.length ?: 0}/250"
        }

        val priorityLow = dialogView.findViewById<TextView>(R.id.priorityLow)
        val priorityMedium = dialogView.findViewById<TextView>(R.id.priorityMedium)
        val priorityHigh = dialogView.findViewById<TextView>(R.id.priorityHigh)
        val submitTaskButton = dialogView.findViewById<MaterialButton>(R.id.submitTaskButton)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)

        var selectedPriority = TaskPriority.MEDIUM

        fun updatePriorityUI(priority: TaskPriority) {
            selectedPriority = priority
            priorityLow.setBackgroundColor(if (priority == TaskPriority.LOW) Color.parseColor("#D63031") else Color.parseColor("#F0EDFF"))
            priorityLow.setTextColor(if (priority == TaskPriority.LOW) Color.WHITE else Color.parseColor("#6C5CE7"))

            priorityMedium.setBackgroundColor(if (priority == TaskPriority.MEDIUM) Color.parseColor("#E65100") else Color.parseColor("#FFF3E0"))
            priorityMedium.setTextColor(if (priority == TaskPriority.MEDIUM) Color.WHITE else Color.parseColor("#E65100"))

            priorityHigh.setBackgroundColor(if (priority == TaskPriority.HIGH) Color.parseColor("#D63031") else Color.parseColor("#FFE5E5"))
            priorityHigh.setTextColor(if (priority == TaskPriority.HIGH) Color.WHITE else Color.parseColor("#D63031"))
        }

        priorityLow.setOnClickListener { updatePriorityUI(TaskPriority.LOW) }
        priorityMedium.setOnClickListener { updatePriorityUI(TaskPriority.MEDIUM) }
        priorityHigh.setOnClickListener { updatePriorityUI(TaskPriority.HIGH) }

        closeButton.setOnClickListener { bottomSheet.dismiss() }

        submitTaskButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()

            if (title.trim().isNotEmpty()) {
                viewModel.addTask(title, description, selectedPriority)
                bottomSheet.dismiss()
            } else {
                titleEditText.error = "Task title cannot be empty"
            }
        }

        bottomSheet.show()
    }

    private fun showTaskCompletedDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_task_completed)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val okButton = dialog.findViewById<MaterialButton>(R.id.dialogOkButton)
        okButton.setOnClickListener {
            viewModel.dismissCompletionDialog()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            viewModel.dismissCompletionDialog()
        }

        dialog.show()
    }
}
