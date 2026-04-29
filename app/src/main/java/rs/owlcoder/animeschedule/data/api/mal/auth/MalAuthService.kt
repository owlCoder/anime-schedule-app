package rs.owlcoder.animeschedule.data.api.mal.auth

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import rs.owlcoder.animeschedule.data.api.mal.dto.MalTokenResponse

interface MalAuthService {
    @FormUrlEncoded
    @POST("v1/oauth2/token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("grant_type") grantType: String,
        // encoded=true — Retrofit ne sme ponovo da enkoduje već enkodovanu vrednost
        @Field(value = "redirect_uri", encoded = true) redirectUri: String
    ): MalTokenResponse

    @FormUrlEncoded
    @POST("v1/oauth2/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String
    ): MalTokenResponse
}
