package com.example.a1weektaskproject.ui

import com.example.a1weektaskproject.data.ApiTaskRepository
import com.example.a1weektaskproject.data.DemoScenario
import com.example.a1weektaskproject.data.InMemoryTaskRepository
import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.data.remote.NetworkResult
import com.example.a1weektaskproject.data.remote.TaskApiService
import com.example.a1weektaskproject.data.remote.model.GoRestTaskResponse
import com.example.a1weektaskproject.data.remote.model.TaskDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryTaskRepository
    private lateinit var viewModel: TaskListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryTaskRepository()
        viewModel = TaskListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTasks_emitsContentStateOnSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is TaskListUiState.Content)
        val content = state as TaskListUiState.Content
        assertEquals(4, content.tasks.size)
        assertEquals(4, content.totalCount)
        assertEquals(1, content.completedCount)
        assertEquals(3, content.pendingCount)
    }

    @Test
    fun loadTasks_emitsEmptyStateWhenNoTasksReturned() = runTest {
        val emptyRepo = object : TaskRepository {
            override suspend fun getTasks() = NetworkResult.Success(emptyList<Task>())
            override suspend fun addTask(title: String, description: String, priority: TaskPriority) = NetworkResult.Success(Task("1", title))
            override suspend fun completeTask(id: String) = NetworkResult.Error("Not found")
            override suspend fun toggleComplete(id: String) = NetworkResult.Error("Not found")
            override fun getTaskById(id: String): Task? = null
        }
        val emptyViewModel = TaskListViewModel(emptyRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = emptyViewModel.uiState.value
        assertTrue(state is TaskListUiState.Empty)
    }

    @Test
    fun loadTasks_emitsErrorStateOnNetworkFailure() = runTest {
        val errorRepo = object : TaskRepository {
            override suspend fun getTasks() = NetworkResult.Error("No internet connection")
            override suspend fun addTask(title: String, description: String, priority: TaskPriority) = NetworkResult.Error("Failed")
            override suspend fun completeTask(id: String) = NetworkResult.Error("Failed")
            override suspend fun toggleComplete(id: String) = NetworkResult.Error("Failed")
            override fun getTaskById(id: String): Task? = null
        }
        val errorViewModel = TaskListViewModel(errorRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = errorViewModel.uiState.value
        assertTrue(state is TaskListUiState.Error)
        assertEquals("No internet connection", (state as TaskListUiState.Error).message)
    }

    @Test
    fun retryFetch_reloadsTasksAfterError() = runTest {
        var callCount = 0
        val toggleRepo = object : TaskRepository {
            override suspend fun getTasks(): NetworkResult<List<Task>> {
                callCount++
                return if (callCount == 1) {
                    NetworkResult.Error("Server connection timed out.")
                } else {
                    NetworkResult.Success(listOf(Task("TASK-101", "Recovered Task")))
                }
            }
            override suspend fun addTask(title: String, description: String, priority: TaskPriority) = NetworkResult.Error("N/A")
            override suspend fun completeTask(id: String) = NetworkResult.Error("N/A")
            override suspend fun toggleComplete(id: String) = NetworkResult.Error("N/A")
            override fun getTaskById(id: String): Task? = null
        }

        val testVm = TaskListViewModel(toggleRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        // 1st load emits Error state
        assertTrue(testVm.uiState.value is TaskListUiState.Error)

        // Tapping Retry re-executes fetch and succeeds
        testVm.retryFetch()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(testVm.uiState.value is TaskListUiState.Content)
        val content = testVm.uiState.value as TaskListUiState.Content
        assertEquals(1, content.tasks.size)
        assertEquals("Recovered Task", content.tasks[0].title)
    }

    @Test
    fun addValidTask_updatesTasksAndCounts() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.addTask("New Task", "Description", TaskPriority.HIGH)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TaskListUiState.Content
        assertEquals(5, state.tasks.size)
    }

    @Test
    fun completeTask_updatesTaskCompletionAndCelebrationState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val pendingTask = (viewModel.uiState.value as TaskListUiState.Content).tasks.first { !it.isCompleted }

        viewModel.completeTask(pendingTask.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TaskListUiState.Content
        val updatedTask = state.tasks.first { it.id == pendingTask.id }
        assertTrue(updatedTask.isCompleted)
        assertEquals(pendingTask.id, state.newlyCompletedTask?.id)
        assertEquals(2, state.completedCount)
        assertEquals(2, state.pendingCount)
    }

    @Test
    fun addTask_withBlankTitle_emitsValidationError() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.addTask("   ", "Description", TaskPriority.MEDIUM)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TaskListUiState.Content)
        val content = state as TaskListUiState.Content
        assertEquals("Task title cannot be empty", content.validationError)
        assertEquals(4, content.tasks.size)
    }

    @Test
    fun addTask_withHighAndLowPriority_preservesPrioritySetting() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.addTask("Urgent Bugfix", "Fix crash", TaskPriority.HIGH)
        viewModel.addTask("Cleanup Code", "Refactor models", TaskPriority.LOW)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as TaskListUiState.Content
        val highTask = state.tasks.first { it.title == "Urgent Bugfix" }
        val lowTask = state.tasks.first { it.title == "Cleanup Code" }

        assertEquals(TaskPriority.HIGH, highTask.priority)
        assertEquals(TaskPriority.LOW, lowTask.priority)
    }

    @Test
    fun toggleComplete_togglesTaskCompletionState() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val initialState = viewModel.uiState.value as TaskListUiState.Content
        val targetTask = initialState.tasks.first { !it.isCompleted }

        // First toggle: Incomplete -> Completed
        viewModel.toggleComplete(targetTask.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val toggledState1 = viewModel.uiState.value as TaskListUiState.Content
        assertTrue(toggledState1.tasks.first { it.id == targetTask.id }.isCompleted)

        // Second toggle: Completed -> Incomplete
        viewModel.toggleComplete(targetTask.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val toggledState2 = viewModel.uiState.value as TaskListUiState.Content
        assertFalse(toggledState2.tasks.first { it.id == targetTask.id }.isCompleted)
    }

    @Test
    fun dismissCompletionDialog_clearsNewlyCompletedTask() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val pendingTask = (viewModel.uiState.value as TaskListUiState.Content).tasks.first { !it.isCompleted }

        viewModel.completeTask(pendingTask.id)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as TaskListUiState.Content
        assertEquals(pendingTask.id, state.newlyCompletedTask?.id)

        viewModel.dismissCompletionDialog()
        state = viewModel.uiState.value as TaskListUiState.Content
        assertNull(state.newlyCompletedTask)
    }

    @Test
    fun clearValidationError_clearsValidationError() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.addTask("  ", "Empty Title")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value as TaskListUiState.Content
        assertEquals("Task title cannot be empty", state.validationError)

        viewModel.clearValidationError()
        state = viewModel.uiState.value as TaskListUiState.Content
        assertNull(state.validationError)
    }

    @Test
    fun setDemoScenario_reloadsTasksWithNewScenario() = runTest {
        val fakeService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int) = Response.success(GoRestTaskResponse(data = emptyList()))
            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id, status = "done"))
        }
        val apiRepo = ApiTaskRepository(fakeService, UnconfinedTestDispatcher())
        val apiVm = TaskListViewModel(apiRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        apiVm.setDemoScenario(DemoScenario.MALFORMED_JSON)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(apiVm.uiState.value is TaskListUiState.Error)
        assertEquals("Server returned invalid or malformed data.", (apiVm.uiState.value as TaskListUiState.Error).message)

        apiVm.setDemoScenario(DemoScenario.EMPTY_RESPONSE)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(apiVm.uiState.value is TaskListUiState.Empty)
    }
}