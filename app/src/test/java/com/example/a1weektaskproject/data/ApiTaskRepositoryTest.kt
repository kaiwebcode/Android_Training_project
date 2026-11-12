package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.remote.NetworkResult
import com.example.a1weektaskproject.data.remote.TaskApiService
import com.example.a1weektaskproject.data.remote.model.GoRestTaskResponse
import com.example.a1weektaskproject.data.remote.model.TaskDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ApiTaskRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun getTasks_returnsSuccessWhenApiCallSucceeds() = runTest {
        val fakeApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                val dtos = listOf(
                    TaskDto(id = "1", title = "Task 1", status = "todo", priority = "high"),
                    TaskDto(id = "2", title = "Task 2", status = "done", priority = "medium")
                )
                return Response.success(GoRestTaskResponse(data = dtos))
            }

            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id, status = "done"))
        }

        val repository = ApiTaskRepository(fakeApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Success)
        val tasks = (result as NetworkResult.Success).data
        assertEquals(2, tasks.size)
        assertEquals("Task 1", tasks[0].title)
        assertEquals(TaskPriority.HIGH, tasks[0].priority)
        assertEquals(true, tasks[1].isCompleted)
    }

    @Test
    fun getTasks_returnsErrorOnNetworkIOException() = runTest {
        val throwingApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                throw IOException("Network connection drop")
            }

            override suspend fun createTask(task: TaskDto): Response<TaskDto> = throw IOException()
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>): Response<TaskDto> = throw IOException()
        }

        val repository = ApiTaskRepository(throwingApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("No internet connection. Please check your network and try again.", error.message)
    }

    @Test
    fun getTasks_returnsErrorOnHttp500ServerError() = runTest {
        val serverErrorApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                return Response.error(500, "Internal Server Error".toResponseBody(null))
            }

            override suspend fun createTask(task: TaskDto): Response<TaskDto> = Response.error(500, "".toResponseBody(null))
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>): Response<TaskDto> = Response.error(500, "".toResponseBody(null))
        }

        val repository = ApiTaskRepository(serverErrorApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("Server error encountered (500). Please try again later.", error.message)
    }
}
