package com.example.a1weektaskproject.data

import com.example.a1weektaskproject.data.remote.NetworkResult
import com.example.a1weektaskproject.data.remote.TaskApiService
import com.example.a1weektaskproject.data.remote.model.GoRestTaskResponse
import com.example.a1weektaskproject.data.remote.model.TaskDto
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

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
    fun getTasks_returnsEmptyListWhenApiDataIsEmpty() = runTest {
        val emptyApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                return Response.success(GoRestTaskResponse(data = emptyList()))
            }

            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id, status = "done"))
        }

        val repository = ApiTaskRepository(emptyApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Success)
        val tasks = (result as NetworkResult.Success).data
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun getTasks_returnsErrorOnMalformedJson() = runTest {
        val malformedApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                throw JsonSyntaxException("Malformed JSON syntax error")
            }

            override suspend fun createTask(task: TaskDto): Response<TaskDto> = throw JsonSyntaxException("Malformed JSON")
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>): Response<TaskDto> = throw JsonSyntaxException("Malformed JSON")
        }

        val repository = ApiTaskRepository(malformedApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("Server returned invalid or malformed data.", error.message)
    }

    @Test
    fun getTasks_returnsErrorOnNetworkTimeout() = runTest {
        val timeoutApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                throw SocketTimeoutException("Connection timeout")
            }

            override suspend fun createTask(task: TaskDto): Response<TaskDto> = throw SocketTimeoutException("Connection timeout")
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>): Response<TaskDto> = throw SocketTimeoutException("Connection timeout")
        }

        val repository = ApiTaskRepository(timeoutApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("Server connection timed out. Please try again.", error.message)
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
    fun getTasks_returnsErrorOnHttp400BadRequest() = runTest {
        val badRequestApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                return Response.error(400, "Bad Request".toResponseBody(null))
            }

            override suspend fun createTask(task: TaskDto) = Response.error<TaskDto>(400, "".toResponseBody(null))
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.error<TaskDto>(400, "".toResponseBody(null))
        }

        val repository = ApiTaskRepository(badRequestApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("Invalid request submitted to server.", error.message)
    }

    @Test
    fun getTasks_returnsErrorOnHttp404NotFound() = runTest {
        val notFoundApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int): Response<GoRestTaskResponse> {
                return Response.error(404, "Not Found".toResponseBody(null))
            }

            override suspend fun createTask(task: TaskDto) = Response.error<TaskDto>(404, "".toResponseBody(null))
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.error<TaskDto>(404, "".toResponseBody(null))
        }

        val repository = ApiTaskRepository(notFoundApiService, testDispatcher)
        val result = repository.getTasks()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("Requested task resource not found.", error.message)
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

    @Test
    fun addTask_addsTaskLocallyAndCallsApi() = runTest {
        var createdTaskDto: TaskDto? = null
        val fakeApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int) = Response.success(GoRestTaskResponse(data = emptyList()))
            override suspend fun createTask(task: TaskDto): Response<TaskDto> {
                createdTaskDto = task
                return Response.success(task)
            }
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id))
        }

        val repository = ApiTaskRepository(fakeApiService, testDispatcher)
        val result = repository.addTask("Write Integration Tests", "Cover all cases", TaskPriority.HIGH)

        assertTrue(result is NetworkResult.Success)
        val createdTask = (result as NetworkResult.Success).data
        assertEquals("Write Integration Tests", createdTask.title)
        assertEquals(TaskPriority.HIGH, createdTask.priority)
        assertNotNull(createdTaskDto)
        assertEquals("Write Integration Tests", createdTaskDto?.title)
        assertEquals("high", createdTaskDto?.priority)

        // Verify task exists in repository cache
        val fetchedTask = repository.getTaskById(createdTask.id)
        assertNotNull(fetchedTask)
        assertEquals("Write Integration Tests", fetchedTask?.title)
    }

    @Test
    fun completeTask_updatesLocalAndCallsApi() = runTest {
        var updatedStatusMap: Map<String, String>? = null
        val fakeApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int) = Response.success(GoRestTaskResponse(data = emptyList()))
            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>): Response<TaskDto> {
                updatedStatusMap = body
                return Response.success(TaskDto(id = id, status = "done"))
            }
        }

        val repository = ApiTaskRepository(fakeApiService, testDispatcher)
        val addResult = repository.addTask("Pending Task", "To be completed", TaskPriority.MEDIUM)
        val taskId = (addResult as NetworkResult.Success).data.id

        val completeResult = repository.completeTask(taskId)
        assertTrue(completeResult is NetworkResult.Success)
        val completedTask = (completeResult as NetworkResult.Success).data
        assertTrue(completedTask.isCompleted)
        assertEquals("done", updatedStatusMap?.get("status"))
    }

    @Test
    fun toggleComplete_togglesStatusAndHandlesMissingId() = runTest {
        val fakeApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int) = Response.success(GoRestTaskResponse(data = emptyList()))
            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id))
        }

        val repository = ApiTaskRepository(fakeApiService, testDispatcher)
        val addResult = repository.addTask("Toggle Task", "Test toggling", TaskPriority.LOW)
        val taskId = (addResult as NetworkResult.Success).data.id

        // First toggle: false -> true
        val toggle1 = repository.toggleComplete(taskId)
        assertTrue(toggle1 is NetworkResult.Success)
        assertTrue((toggle1 as NetworkResult.Success).data.isCompleted)

        // Second toggle: true -> false
        val toggle2 = repository.toggleComplete(taskId)
        assertTrue(toggle2 is NetworkResult.Success)
        assertFalse((toggle2 as NetworkResult.Success).data.isCompleted)

        // Invalid ID toggle returns error
        val invalidToggle = repository.toggleComplete("NON_EXISTENT_ID")
        assertTrue(invalidToggle is NetworkResult.Error)
        assertEquals("Task with ID NON_EXISTENT_ID not found", (invalidToggle as NetworkResult.Error).message)
    }

    @Test
    fun getTaskById_returnsCorrectTaskOrNull() = runTest {
        val fakeApiService = object : TaskApiService {
            override suspend fun getTasks(pageSize: Int) = Response.success(GoRestTaskResponse(data = emptyList()))
            override suspend fun createTask(task: TaskDto) = Response.success(task)
            override suspend fun updateTaskStatus(id: String, body: Map<String, String>) = Response.success(TaskDto(id = id))
        }

        val repository = ApiTaskRepository(fakeApiService, testDispatcher)
        val addResult = repository.addTask("Find Me", "Description", TaskPriority.MEDIUM)
        val taskId = (addResult as NetworkResult.Success).data.id

        val foundTask = repository.getTaskById(taskId)
        assertNotNull(foundTask)
        assertEquals("Find Me", foundTask?.title)

        val notFoundTask = repository.getTaskById("UNKNOWN_ID")
        assertNull(notFoundTask)
    }
}