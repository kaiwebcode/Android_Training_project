package com.example.a1weektaskproject.ui

import com.example.a1weektaskproject.data.Task
import com.example.a1weektaskproject.data.TaskPriority
import com.example.a1weektaskproject.data.TaskRepository
import com.example.a1weektaskproject.data.remote.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completeTask_updatesTaskToCompletedAndResetsCompletingState() = runTest {
        val initialTask = Task("DETAILS-1", "Inspect details", "Description", false, TaskPriority.HIGH)
        var completeCallCount = 0

        val fakeRepo = object : TaskRepository {
            override fun getTasksFlow(): Flow<List<Task>> = flowOf(listOf(initialTask))
            override suspend fun getTasks(): NetworkResult<List<Task>> = NetworkResult.Success(listOf(initialTask))
            override suspend fun addTask(title: String, description: String, priority: TaskPriority) = NetworkResult.Error("N/A")
            override suspend fun completeTask(id: String): NetworkResult<Task> {
                completeCallCount++
                kotlinx.coroutines.delay(500L)
                return NetworkResult.Success(initialTask.copy(isCompleted = true))
            }
            override suspend fun toggleComplete(id: String) = NetworkResult.Error("N/A")
            override fun getTaskById(id: String): Task? = initialTask
        }

        val viewModel = TaskDetailsViewModel(fakeRepo)
        viewModel.initTask(initialTask)

        // Verify initial state
        assertEquals(initialTask, viewModel.task.value)
        assertFalse(viewModel.isCompleting.value)

        // Rapid double tap
        viewModel.completeTask()
        viewModel.completeTask()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, completeCallCount)
        assertTrue(viewModel.task.value?.isCompleted == true)
        assertFalse(viewModel.isCompleting.value)
    }
}
