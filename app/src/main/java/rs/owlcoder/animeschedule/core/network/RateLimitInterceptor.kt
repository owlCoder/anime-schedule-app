package rs.owlcoder.animeschedule.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 60L
            response.close()
            Thread.sleep(TimeUnit.SECONDS.toMillis(retryAfter))
            return chain.proceed(chain.request())
        }
        return response
    }
}
