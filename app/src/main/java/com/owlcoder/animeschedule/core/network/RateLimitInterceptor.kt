package com.owlcoder.animeschedule.core.network

import okhttp3.Interceptor
import okhttp3.Response

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Never sleep in an OkHttp interceptor. A provider can return Retry-After=60 and that
        // would block the connection pool, defeat coroutine timeouts, and keep the app splash
        // visible long after the request is known to have failed. Callers treat 429 as a
        // provider failure and move to the next source or the local cache.
        return chain.proceed(chain.request())
    }
}
