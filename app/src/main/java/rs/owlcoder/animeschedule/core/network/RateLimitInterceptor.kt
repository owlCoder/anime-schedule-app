package rs.owlcoder.animeschedule.core.network

import okhttp3.Interceptor
import okhttp3.Response

private const val MAX_RETRY_AFTER_SECONDS = 120L

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 429) {
            val retryAfterSeconds = (response.header("Retry-After")?.toLongOrNull() ?: 60L)
                .coerceAtMost(MAX_RETRY_AFTER_SECONDS)
            response.close()
            // OkHttp interceptors run on a background thread — sleep is acceptable here,
            // but we cap Retry-After to avoid indefinitely blocking the connection pool.
            Thread.sleep(retryAfterSeconds * 1000L)
            return chain.proceed(chain.request())
        }
        return response
    }
}
