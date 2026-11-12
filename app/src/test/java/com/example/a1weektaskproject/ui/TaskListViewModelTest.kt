package com.example.a1weektaskproject.ui

import com.example.a1weektaskproject.data.InMemoryTaskRepository
import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    }
}
