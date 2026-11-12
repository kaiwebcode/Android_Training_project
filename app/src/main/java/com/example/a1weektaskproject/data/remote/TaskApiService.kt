package com.example.a1weektaskproject.data.remote

import com.example.a1weektaskproject.data.remote.model.GoRestTaskResponse
import com.example.a1weektaskproject.data.remote.model.TaskDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface defining REST endpoints for task retrieval and updates.
 */
interface TaskApiService {

    @GET("public/v3/tasks")
    suspend fun getTasks(
        @Query("page[size]") pageSize: Int = 10
    ): Response<GoRestTaskResponse>

    @POST("public/v3/tasks")
    suspend fun createTask(
        @Body task: TaskDto
    ): Response<TaskDto>

    @PUT("public/v3/tasks/{id}")
    suspend fun updateTaskStatus(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<TaskDto>
}
