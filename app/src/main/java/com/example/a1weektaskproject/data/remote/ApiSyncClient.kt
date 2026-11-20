package com.example.a1weektaskproject.data.remote

import com.example.a1weektaskproject.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Retrofit client for the development mock sync server. */
object ApiSyncClient {
    val taskSyncApiService: TaskSyncApiService? by lazy {
        val baseUrl = BuildConfig.MOCK_SYNC_BASE_URL
        if (baseUrl.isEmpty()) return@lazy null

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskSyncApiService::class.java)
    }
}
