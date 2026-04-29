package rs.owlcoder.animeschedule.core.network

import okhttp3.Interceptor
import okhttp3.Response

private const val MAL_HOST = "api.myanimelist.net"

class AuthInterceptor(
    private val getAccessToken: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != MAL_HOST) return chain.proceed(request)
        val token = getAccessToken() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
