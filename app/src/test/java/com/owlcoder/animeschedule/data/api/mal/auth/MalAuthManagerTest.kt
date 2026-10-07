package com.owlcoder.animeschedule.data.api.mal.auth

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.*
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.domain.model.UserPreferences
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import io.mockk.*
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Retrofit

/** Real Retrofit forms/responses against a local OAuth endpoint; no user credentials. */
class MalAuthManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var manager: MalAuthManager
    private lateinit var tokens: SecureTokenStore
    private lateinit var prefs: UserPreferencesDataStore
    private lateinit var list: MalListEntryDao
    private lateinit var queue: PendingListUpdateDao
    private lateinit var receipts: NotificationActionDao
    private lateinit var work: WorkScheduler
    private val state = MutableStateFlow(UserPreferences(malUsername = "First"))
    private var pending = emptyList<PendingListUpdateEntity>()
    private var refresh: String? = "fixture-refresh"
    private var expiry = Instant.now().epochSecond + 3600
    private val session = MalSession()
    @Before fun setUp() {
        server = MockWebServer(); server.start()
        val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType())).build()
        tokens = mockk(relaxed = true) {
            every { getMalRefreshToken() } answers { refresh }
            every { getMalTokenExpiresAt() } answers { expiry }
            every { saveMalTokens(any(), any(), any()) } answers { refresh = secondArg(); expiry = thirdArg() }
        }
        prefs = mockk(relaxed = true) {
            every { userPreferencesFlow } returns state
            coEvery { setMalLoggedIn(any(), any(), any()) } coAnswers {
                state.value = state.value.copy(malLoggedIn = firstArg(), malUsername = secondArg(), malAvatarUrl = thirdArg())
            }
        }
        list = mockk(relaxed = true); queue = mockk(relaxed = true) {
            coEvery { getAll() } answers { pending }
            coEvery { deleteAll() } answers { pending = emptyList() }
        }
        receipts = mockk(relaxed = true); work = mockk(relaxed = true)
        manager = MalAuthManager(retrofit.create(MalAuthService::class.java), retrofit.create(MalApiService::class.java),
            tokens, prefs, list, queue, session, receipts, work)
    }
    @After fun close() { server.shutdown() }
    private fun reply(code: Int = 200, body: String) = server.enqueue(MockResponse().setResponseCode(code).setBody(body).setHeader("Content-Type", "application/json"))
    private fun token() = reply(body = """{"access_token":"fixture-access","refresh_token":"fixture-refresh","expires_in":3600}""")

    @Test fun `verified same account login preserves offline edits and schedules recovery`() = runTest {
        pending = listOf(PendingListUpdateEntity(101, "watching", 5, 8, false, 1))
        token(); reply(body = """{"id":1,"name":"FIRST","picture":null}""")
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        val request = server.takeRequest()
        assertEquals("/v1/oauth2/token", request.path)
        val form = request.body.readUtf8()
        assertTrue(form.contains("code=fixture-code")); assertTrue(form.contains("code_verifier=fixture-verifier"))
        assertTrue(form.contains("grant_type=authorization_code"))
        coVerify(exactly = 0) { queue.deleteAll() }
        verify { work.scheduleFlushPendingUpdates() }
        assertTrue(state.value.malLoggedIn)
        assertEquals(1L, session.epoch)
    }
    @Test fun `different account clears old edits and notification receipts before exposing login`() = runTest {
        pending = listOf(PendingListUpdateEntity(101, null, 5, null, false, 1))
        token(); reply(body = """{"id":2,"name":"Second"}""")
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        coVerifyOrder { list.deleteAll(); queue.deleteAll(); receipts.deleteAll(); prefs.setMalLoggedIn(true, "Second", "") }
        assertTrue(pending.isEmpty())
    }
    @Test fun `unverified profile never becomes a session or replays queued edits`() = runTest {
        token(); reply(503, "{}")
        assertFalse(manager.handleCallback("fixture-code", "fixture-verifier"))
        assertFalse(state.value.malLoggedIn)
        verify { tokens.clearMalTokens() }
        verify(exactly = 0) { work.scheduleFlushPendingUpdates() }
    }
    @Test fun `forced refresh contacts server despite future expiry and classifies failures`() = runTest {
        assertEquals(MalAuthManager.RefreshResult.REFRESHED, manager.refreshAccessToken())
        assertEquals(0, server.requestCount)
        token()
        assertEquals(MalAuthManager.RefreshResult.REFRESHED, manager.refreshAccessToken(force = true))
        assertTrue(server.takeRequest().body.readUtf8().contains("grant_type=refresh_token"))
        reply(503, "{}")
        assertEquals(MalAuthManager.RefreshResult.TRANSIENT, manager.refreshAccessToken(force = true))
        reply(400, "{}")
        assertEquals(MalAuthManager.RefreshResult.INVALID, manager.refreshAccessToken(force = true))
    }
    @Test fun `logout waits for active account operation then clears queue and identity`() = runTest {
        session.mutex.lock()
        val logout = async { manager.logout() }
        testScheduler.runCurrent()
        assertFalse(logout.isCompleted)
        coVerify(exactly = 0) { queue.deleteAll() }
        session.mutex.unlock(); logout.await()
        coVerifyOrder { list.deleteAll(); queue.deleteAll(); receipts.deleteAll(); prefs.setMalLoggedIn(false, "", any()) }
        verify { tokens.clearMalTokens() }
        assertEquals(1L, session.epoch)
    }
}
