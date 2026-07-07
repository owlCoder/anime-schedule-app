package com.owlcoder.animeschedule.core.result

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()
}

sealed class AppError {
    data class Network(val message: String? = null) : AppError()
    data class RateLimit(val retryAfterSeconds: Long = 60) : AppError()
    data class GraphQL(val message: String) : AppError()
    data object Unauthorized : AppError()
    data object NoCache : AppError()
    data class Unknown(val message: String? = null) : AppError()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}

inline fun <T, R> AppResult<T>.mapResult(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
