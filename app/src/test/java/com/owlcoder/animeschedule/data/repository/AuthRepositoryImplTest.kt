package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthManager
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.domain.model.LoginFailure
import com.owlcoder.animeschedule.domain.model.LoginState
import com.owlcoder.animeschedule.domain.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private lateinit var manager: MalAuthManager
    private lateinit var tokens: SecureTokenStore
    private lateinit var repository: AuthRepositoryImpl

    private var storedVerifier: String? = null
    private var storedState: String? = null

    @Before
    fun setUp() {
        storedVerifier = null
        storedState = null
        manager = mockk {
            every { buildAuthorizationUri(any(), any()) } returns "https://mal.test/authorize"
            coEvery { handleCallback(any(), any()) } returns true
        }
        tokens = mockk(relaxed = true) {
            every { savePkceVerifier(any()) } answers { storedVerifier = firstArg() }
            every { saveOAuthState(any()) } answers { storedState = firstArg() }
            every { getPkceVerifier() } answers { storedVerifier }
            every { getOAuthState() } answers { storedState }
            every { clearPkceVerifier() } answers { storedVerifier = null; storedState = null }
        }
        val prefs = mockk<UserPreferencesDataStore> {
            every { userPreferencesFlow } returns flowOf(UserPreferences())
        }
        repository = AuthRepositoryImpl(manager, tokens, prefs)
    }

    @Test
    fun `beginLogin persists verifier and state and reports progress`() {
        val url = repository.beginLogin()

        assertEquals("https://mal.test/authorize", url)
        assertTrue(storedVerifier!!.length >= 43)
        assertTrue(storedState!!.isNotBlank())
        assertEquals(LoginState.InProgress, repository.loginState.value)
    }

    @Test
    fun `matching state completes the login with the stored verifier`() = runTest {
        repository.beginLogin()
        val verifier = storedVerifier!!
        val state = storedState!!

        val success = repository.completeLogin("code-1", state)

        assertTrue(success)
        coVerify { manager.handleCallback("code-1", verifier) }
        assertEquals(LoginState.Idle, repository.loginState.value)
        verify { tokens.clearPkceVerifier() }
    }

    @Test
    fun `mismatched state is rejected without contacting MAL`() = runTest {
        repository.beginLogin()

        val success = repository.completeLogin("code-1", "forged-state")

        assertFalse(success)
        coVerify(exactly = 0) { manager.handleCallback(any(), any()) }
        assertEquals(LoginState.Failed(LoginFailure.INVALID_SESSION), repository.loginState.value)
    }

    @Test
    fun `missing state or verifier is rejected`() = runTest {
        // No beginLogin(): nothing is stored, as after an unsolicited redirect.
        assertFalse(repository.completeLogin("code", "whatever"))
        repository.beginLogin()
        assertFalse(repository.completeLogin("code", null))
        coVerify(exactly = 0) { manager.handleCallback(any(), any()) }
    }

    @Test
    fun `a rejected code reports an exchange failure`() = runTest {
        coEvery { manager.handleCallback(any(), any()) } returns false
        repository.beginLogin()

        assertFalse(repository.completeLogin("code", storedState))

        assertEquals(LoginState.Failed(LoginFailure.EXCHANGE_FAILED), repository.loginState.value)
    }

    @Test
    fun `declined access is reported and clears the attempt`() {
        repository.beginLogin()

        repository.loginDenied()

        assertEquals(LoginState.Failed(LoginFailure.DENIED), repository.loginState.value)
        assertEquals(null, storedVerifier)
    }

    @Test
    fun `closing the browser without a redirect cancels the pending login`() {
        repository.beginLogin()

        repository.loginAbandoned()

        assertEquals(LoginState.Idle, repository.loginState.value)
    }

    @Test
    fun `resuming while the code exchange runs does not cancel the login`() = runTest {
        repository.beginLogin()
        var stateDuringExchange: LoginState? = null
        coEvery { manager.handleCallback(any(), any()) } coAnswers {
            // First resume consumes the "redirect just arrived" flag, the second is a later one;
            // neither may abort a sign-in whose code is already being exchanged.
            repository.loginAbandoned()
            repository.loginAbandoned()
            stateDuringExchange = repository.loginState.value
            true
        }

        repository.completeLogin("code", storedState)

        assertEquals(LoginState.InProgress, stateDuringExchange)
        assertEquals(LoginState.Idle, repository.loginState.value)
    }
}
