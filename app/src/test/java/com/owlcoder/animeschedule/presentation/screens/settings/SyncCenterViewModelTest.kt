package com.owlcoder.animeschedule.presentation.screens.settings

import com.owlcoder.animeschedule.data.network.NetworkMonitor
import com.owlcoder.animeschedule.domain.model.MalSyncState
import com.owlcoder.animeschedule.domain.repository.MalRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SyncCenterViewModelTest {
    @Before fun before() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }
    private val states = MutableStateFlow(MalSyncState(loggedIn = true, lastSuccessEpochMs = 100))
    private val repository = mockk<MalRepository> { every { syncState } returns states }
    private val network = mockk<NetworkMonitor> { every { online } returns flowOf(true) }
    private fun TestScope.observe(): SyncCenterViewModel = SyncCenterViewModel(repository, network).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
    }
    @Test fun `concurrent retries share one call and expose busy state`() = runTest {
        val gate = CompletableDeferred<Boolean>()
        coEvery { repository.retrySync() } coAnswers { gate.await() }
        val vm = observe()
        vm.retry(); vm.retry(); runCurrent()
        coVerify(exactly = 1) { repository.retrySync() }
        assertTrue(vm.state.value.syncing)
        gate.complete(true); runCurrent()
        assertFalse(vm.state.value.syncing)
        assertFalse(vm.state.value.failed)
    }
    @Test fun `unexpected failure is visible and a successful retry recovers`() = runTest {
        coEvery { repository.retrySync() } throws IllegalStateException("fixture failure")
        val vm = observe(); vm.retry(); runCurrent()
        assertTrue(vm.state.value.failed); assertFalse(vm.state.value.syncing)
        coEvery { repository.retrySync() } returns true
        vm.retry(); runCurrent()
        assertFalse(vm.state.value.failed); assertFalse(vm.state.value.syncing)
    }
    @Test fun `a later background success clears a failed manual retry`() = runTest {
        coEvery { repository.retrySync() } returns false
        val vm = observe(); vm.retry(); runCurrent()
        assertTrue(vm.state.value.failed)
        states.value = states.value.copy(lastSuccessEpochMs = 200); runCurrent()
        assertFalse(vm.state.value.failed)
    }
    @Test fun `signing out clears a failure before the next session`() = runTest {
        coEvery { repository.retrySync() } returns false
        val vm = observe(); vm.retry(); runCurrent()
        assertTrue(vm.state.value.failed)
        states.value = states.value.copy(loggedIn = false); runCurrent()
        assertFalse(vm.state.value.failed)
        states.value = states.value.copy(loggedIn = true); runCurrent()
        assertFalse(vm.state.value.failed)
    }

}
