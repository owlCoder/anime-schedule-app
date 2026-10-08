package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MyListRefreshTest {
    @Before fun before() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun `unexpected refresh failure retains cache stops loading and permits retry`() = runTest {
        val cached = listOf(MalListEntry(101, "Cached", status = WatchStatus.WATCHING, episodesWatched = 3, score = 7, totalEpisodes = 12))
        val repo = mockk<MalRepository>()
        val auth = mockk<AuthRepository>()
        every { repo.getUserList() } returns MutableStateFlow(cached)
        every { auth.isLoggedIn } returns flowOf(true)
        var calls = 0
        coEvery { repo.refreshUserList(any()) } coAnswers { if (++calls == 1) throw IllegalStateException("fixture") else true }
        val vm = MyListViewModel(repo, auth)
        val observed = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        try {
            advanceUntilIdle()
            assertEquals(MyListViewModel.UpdateEvent.Error, vm.updateEvent.first())
            // Library derivation uses Default, so wait for its emission as well as Main's refresh.
            val recovered = withContext(Dispatchers.Default) {
                withTimeout(5_000) { vm.uiState.first { !it.isLoading && it.allEntries.isNotEmpty() } }
            }
            assertEquals(cached, recovered.entries)
            vm.refresh(); advanceUntilIdle()
            assertEquals(2, calls); assertFalse(vm.uiState.value.isLoading)
            vm.setSmartFilter(SmartListFilter.COMPLETED_UNRATED)
            val filtered = withContext(Dispatchers.Default) {
                withTimeout(5_000) { vm.uiState.first { it.smartFilter == SmartListFilter.COMPLETED_UNRATED && it.activeFilter == WatchStatus.COMPLETED } }
            }
            assertEquals(WatchStatus.COMPLETED, filtered.activeFilter)
        } finally {
            observed.cancelAndJoin()
            vm.viewModelScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
