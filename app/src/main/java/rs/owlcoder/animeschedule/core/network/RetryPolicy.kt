package rs.owlcoder.animeschedule.core.network

import kotlinx.coroutines.delay

suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block()
}
