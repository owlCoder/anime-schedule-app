package com.owlcoder.animeschedule.data.api.jikan

import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApiService {
    @GET("v4/schedules")
    suspend fun getSchedule(
        @Query("filter") dayOfWeek: String,
        @Query("page") page: Int = 1
    ): JikanScheduleResponse
}
