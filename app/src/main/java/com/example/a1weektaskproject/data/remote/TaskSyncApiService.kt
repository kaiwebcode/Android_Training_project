package com.example.a1weektaskproject.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

data class TaskSyncDto(
    @SerializedName("clientTaskId") val clientTaskId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("priority") val priority: String = "MEDIUM",
    @SerializedName("dueDate") val dueDate: String = "Today",
    @SerializedName("version") val version: Int = 1
)

data class TaskSyncResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("duplicateCreated") val duplicateCreated: Boolean?,
    @SerializedName("task") val task: TaskSyncDto?
)

data class TaskSyncListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("tasks") val tasks: List<TaskSyncDto>
)

interface TaskSyncApiService {
    @POST("tasks")
    suspend fun createTask(@Body task: TaskSyncDto): Response<TaskSyncResponse>

    @PATCH("tasks/{clientTaskId}")
    suspend fun updateTaskStatus(
        @Path("clientTaskId") clientTaskId: String,
        @Body body: Map<String, Any>
    ): Response<TaskSyncResponse>

    @GET("tasks")
    suspend fun getTasks(): Response<TaskSyncListResponse>
}
