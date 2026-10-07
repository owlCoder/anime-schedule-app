package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.owlcoder.animeschedule.core.result.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private class Details : AnimeDetailRepository {
        var calls = 0
        var character: suspend (Int) -> AppResult<CharacterDetail> = { AppResult.Success(value(it)) }
        override fun getAnimeDetail(animeId: Int) = flowOf<AppResult<AnimeDetail>>(AppResult.Error(AppError.NoCache))
        override suspend fun getCharacterDetail(characterId: Int): AppResult<CharacterDetail> { calls++; return character(characterId) }
    }
    private fun vm(repo: Details): DetailViewModel {
        val auth = mockk<AuthRepository> { every { isLoggedIn } returns flowOf(false) }
        val sources = mockk<WatchSourceRepository> { every { getAll() } returns flowOf(emptyList()) }
        return DetailViewModel(SavedStateHandle(mapOf("animeId" to 1)), repo, sources, auth, mockk(relaxed = true))
    }
    @Before fun before() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun `reopening a loaded character uses bounded local cache`() = runTest {
        val repo = Details(); val vm = vm(repo)
        vm.openCharacter(1); runCurrent()
        vm.dismissCharacterOverlay(); vm.openCharacter(1); runCurrent()
        assertEquals(1, repo.calls)
        assertEquals(1, vm.characterOverlay.value.detail?.id)
        assertFalse(vm.characterOverlay.value.isLoading)
        for (id in 2..21) { vm.openCharacter(id); runCurrent() }
        vm.openCharacter(1); runCurrent()
        assertEquals(22, repo.calls)
    }
    @Test fun `late noncancellable response cannot reopen dismissed overlay`() = runTest {
        val gate = CompletableDeferred<Unit>(); val repo = Details()
        repo.character = { id -> withContext(NonCancellable) { gate.await(); AppResult.Success(value(id)) } }
        val vm = vm(repo); vm.openCharacter(1); runCurrent()
        vm.openCharacter(1); runCurrent(); assertEquals(1, repo.calls)
        vm.dismissCharacterOverlay(); gate.complete(Unit); runCurrent()
        assertEquals(CharacterOverlayState(), vm.characterOverlay.value)
    }
    @Test fun `late character response cannot replace the new selection`() = runTest {
        val gate = CompletableDeferred<Unit>(); val repo = Details()
        repo.character = { id -> if (id == 1) withContext(NonCancellable) { gate.await(); AppResult.Success(value(id)) } else AppResult.Success(value(id)) }
        val vm = vm(repo); vm.openCharacter(1); runCurrent()
        vm.openCharacter(2); runCurrent(); gate.complete(Unit); runCurrent()
        assertEquals(2, vm.characterOverlay.value.detail?.id)
    }
    @Test fun `retry keeps requested character and failures are not cached`() = runTest {
        val repo = Details().apply { character = { AppResult.Error(AppError.NoCache) } }
        val vm = vm(repo); vm.openCharacter(7); runCurrent()
        assertNotNull(vm.characterOverlay.value.errorRes)
        repo.character = { id -> AppResult.Success(value(id)) }
        vm.retryCharacter(); assertTrue(vm.characterOverlay.value.isLoading); runCurrent()
        assertEquals(7, vm.characterOverlay.value.detail?.id)
        assertNull(vm.characterOverlay.value.errorRes)
        assertEquals(2, repo.calls)
    }
    companion object {
        private fun value(id: Int) = CharacterDetail(id, "Character $id", null, null, "Description $id")
    }
}
