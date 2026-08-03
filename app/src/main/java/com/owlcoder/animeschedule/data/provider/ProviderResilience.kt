package com.owlcoder.animeschedule.data.provider

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ProviderOperation(
    val providerTimeoutMs: Long,
    val totalBudgetMs: Long
) {
    SEARCH(providerTimeoutMs = 3_000L, totalBudgetMs = 8_000L),
    DETAIL(providerTimeoutMs = 4_000L, totalBudgetMs = 8_000L),
    SEASON(providerTimeoutMs = 5_000L, totalBudgetMs = 10_000L),
    SCHEDULE(providerTimeoutMs = 6_000L, totalBudgetMs = 12_000L)
}

enum class ProviderFailureKind {
    TRANSIENT,
    RATE_LIMITED,
    AUTHENTICATION,
    PERMANENT
}

data class ProviderFailure(
    val provider: String,
    val kind: ProviderFailureKind,
    val statusCode: Int? = null,
    val retryAfterSeconds: Long? = null,
    val message: String? = null
)

/** Use this when an adapter returns AppResult.Error instead of throwing an HTTP exception. */
class ProviderCallException(
    val provider: String,
    val statusCode: Int? = null,
    val retryAfterSeconds: Long? = null,
    message: String? = null
) : IOException(message)

fun <T> AppResult<T>.requireProviderData(provider: String): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> throw ProviderCallException(
        provider = provider,
        statusCode = error.providerStatusCode(),
        retryAfterSeconds = (error as? AppError.RateLimit)?.retryAfterSeconds,
        message = error.toString()
    )
}

private fun AppError.providerStatusCode(): Int? = when (this) {
    AppError.Unauthorized -> 401
    is AppError.RateLimit -> 429
    is AppError.GraphQL -> {
        val normalized = message.lowercase()
        Regex("\\b(401|403|429|5\\d{2})\\b").find(message)?.value?.toIntOrNull()
            ?: when {
                "forbidden" in normalized || "unauthorized" in normalized ||
                    "temporarily disabled" in normalized -> 403
                "rate limit" in normalized || "too many requests" in normalized -> 429
                else -> null
            }
    }
    else -> null
}

fun interface ProviderClock {
    fun nowMillis(): Long
}

@Singleton
class SystemProviderClock @Inject constructor() : ProviderClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/**
 * Small in-memory circuit breaker. It deliberately does not persist provider state: a fresh app
 * process should be able to probe a provider again, while a single bad process must not hammer it.
 */
@Singleton
class ProviderHealthStore @Inject constructor(
    private val clock: ProviderClock
) {
    private data class State(
        var consecutiveTransientFailures: Int = 0,
        var failureWindowStartedAt: Long = -1L,
        var openUntil: Long = 0L,
        var halfOpenProbeInFlight: Boolean = false
    )

    private val states = mutableMapOf<String, State>()

    @Synchronized
    fun tryAcquire(provider: String): Boolean {
        val now = clock.nowMillis()
        val state = states.getOrPut(provider) { State() }
        if (state.openUntil == 0L) return true
        if (now < state.openUntil) return false
        if (state.halfOpenProbeInFlight) return false
        state.halfOpenProbeInFlight = true
        return true
    }

    @Synchronized
    fun recordSuccess(provider: String) {
        states[provider] = State()
    }

    @Synchronized
    fun recordFailure(failure: ProviderFailure) {
        val now = clock.nowMillis()
        val state = states.getOrPut(failure.provider) { State() }
        state.halfOpenProbeInFlight = false

        when (failure.kind) {
            ProviderFailureKind.RATE_LIMITED -> {
                state.openUntil = now + (failure.retryAfterSeconds ?: DEFAULT_RATE_LIMIT_OPEN_SECONDS) * 1_000L
            }
            ProviderFailureKind.AUTHENTICATION -> {
                state.openUntil = now + AUTHENTICATION_OPEN_MS
            }
            ProviderFailureKind.TRANSIENT -> {
                if (state.failureWindowStartedAt < 0L ||
                    now - state.failureWindowStartedAt > TRANSIENT_FAILURE_WINDOW_MS
                ) {
                    state.failureWindowStartedAt = now
                    state.consecutiveTransientFailures = 0
                }
                state.consecutiveTransientFailures++
                if (state.consecutiveTransientFailures >= TRANSIENT_FAILURE_THRESHOLD) {
                    state.openUntil = now + TRANSIENT_OPEN_MS
                }
            }
            ProviderFailureKind.PERMANENT -> Unit
        }
    }

    @Synchronized
    fun isOpen(provider: String): Boolean =
        states[provider]?.let { it.openUntil > clock.nowMillis() } == true

    companion object {
        private const val TRANSIENT_FAILURE_THRESHOLD = 3
        private const val TRANSIENT_FAILURE_WINDOW_MS = 5 * 60 * 1_000L
        private const val TRANSIENT_OPEN_MS = 5 * 60 * 1_000L
        private const val AUTHENTICATION_OPEN_MS = 6 * 60 * 60 * 1_000L
        private const val DEFAULT_RATE_LIMIT_OPEN_SECONDS = 60L
    }
}

data class ProviderCall<T>(
    val provider: String,
    val isUsable: (T) -> Boolean = { true },
    val block: suspend () -> T
)

sealed class ProviderResult<out T> {
    data class Success<T>(val provider: String, val value: T) : ProviderResult<T>()
    data class Exhausted(val failures: List<ProviderFailure>) : ProviderResult<Nothing>()
}

@Singleton
class ProviderOrchestrator @Inject constructor(
    private val healthStore: ProviderHealthStore,
    private val clock: ProviderClock
) {
    suspend fun <T> firstSuccessful(
        operation: ProviderOperation,
        calls: List<ProviderCall<T>>
    ): ProviderResult<T> {
        val failures = mutableListOf<ProviderFailure>()
        val deadline = clock.nowMillis() + operation.totalBudgetMs

        for (call in calls) {
            if (!healthStore.tryAcquire(call.provider)) {
                failures += ProviderFailure(
                    provider = call.provider,
                    kind = ProviderFailureKind.TRANSIENT,
                    message = "circuit open"
                )
                continue
            }

            val remainingMs = deadline - clock.nowMillis()
            if (remainingMs <= 0L) break

            try {
                val value = withTimeout(minOf(operation.providerTimeoutMs, remainingMs)) {
                    call.block()
                }
                healthStore.recordSuccess(call.provider)
                if (call.isUsable(value)) {
                    return ProviderResult.Success(call.provider, value)
                }
            } catch (e: TimeoutCancellationException) {
                val failure = ProviderFailure(
                    provider = call.provider,
                    kind = ProviderFailureKind.TRANSIENT,
                    message = "provider timeout"
                )
                healthStore.recordFailure(failure)
                failures += failure
            } catch (e: CancellationException) {
                // A user/screen cancellation must always propagate to the caller.
                throw e
            } catch (e: Exception) {
                val failure = classify(call.provider, e)
                healthStore.recordFailure(failure)
                failures += failure
            }
        }
        return ProviderResult.Exhausted(failures)
    }

    private fun classify(provider: String, error: Exception): ProviderFailure {
        val providerError = error as? ProviderCallException
        val statusCode = providerError?.statusCode ?: (error as? HttpException)?.code()
        val retryAfter = providerError?.retryAfterSeconds
            ?: (error as? HttpException)?.response()?.headers()?.get("Retry-After")?.toLongOrNull()
        val kind = when {
            statusCode == 429 -> ProviderFailureKind.RATE_LIMITED
            statusCode == 401 || statusCode == 403 -> ProviderFailureKind.AUTHENTICATION
            statusCode != null && statusCode in 500..599 -> ProviderFailureKind.TRANSIENT
            error is IOException -> ProviderFailureKind.TRANSIENT
            else -> ProviderFailureKind.PERMANENT
        }
        return ProviderFailure(
            provider = provider,
            kind = kind,
            statusCode = statusCode,
            retryAfterSeconds = retryAfter,
            message = error.message
        )
    }
}
