package com.example.a1weektaskproject.data.remote

/**
 * Sealed class representing network call results across repository operations.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : NetworkResult<Nothing>()
}
