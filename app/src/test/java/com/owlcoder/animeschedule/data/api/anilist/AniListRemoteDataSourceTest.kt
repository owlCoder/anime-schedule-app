package com.owlcoder.animeschedule.data.api.anilist

import com.apollographql.apollo.ApolloClient
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AniListRemoteDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: AniListRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        dataSource = AniListRemoteDataSource(
            ApolloClient.Builder().serverUrl(server.url("/").toString()).build()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search reports a network failure instead of an empty page`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = dataSource.searchAnime("naruto")

        assertTrue("expected Network error but was $result", result.isNetworkError())
    }

    @Test
    fun `airing schedule reports a network failure instead of an empty list`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = dataSource.getAiringSchedule(from = 0L, to = 100L)

        assertTrue("expected Network error but was $result", result.isNetworkError())
    }

    @Test
    fun `server errors are reported as failures`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val result = dataSource.getSeasonalAnime(
            com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSeason.WINTER,
            2026
        )

        assertTrue("expected an error but was $result", result is AppResult.Error)
    }

    @Test
    fun `search parses a successful page`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"data":{"Page":{"pageInfo":{"hasNextPage":true,"currentPage":1},
                "media":[{"id":1,"idMal":2,"title":{"romaji":"Cowboy Bebop","english":null,"native":null},
                "coverImage":{"large":null,"color":null},"format":"TV","seasonYear":1998,
                "meanScore":86,"episodes":26,"status":"FINISHED"}]}}}"""
            )
        )

        val result = dataSource.searchAnime("bebop") as AppResult.Success

        assertEquals(1, result.data.media.size)
        assertTrue(result.data.hasNextPage)
    }

    private fun AppResult<*>.isNetworkError() =
        this is AppResult.Error && error is AppError.Network
}
