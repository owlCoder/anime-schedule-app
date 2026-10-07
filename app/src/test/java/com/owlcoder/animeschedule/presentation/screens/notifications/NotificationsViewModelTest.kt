package com.owlcoder.animeschedule.presentation.screens.notifications

import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @Before fun before() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun after() { Dispatchers.resetMain() }
    private class Repository : NotificationRepository {
        var calls = 0
        var ids = emptyList<Int>()
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override fun getAll() = flowOf(emptyList<AppNotification>())
        override fun getUnreadCount() = flowOf(0)
        override suspend fun markRead(id: Int) = Unit
        override suspend fun markRead(ids: List<Int>) { calls++; this.ids = ids; gate?.await(); if (fail) error("Storage") }
        override suspend fun markAllRead() { calls++; gate?.await() }
        override suspend fun deleteRead() = 0
    }
    @Test fun `filtered mark copies caller IDs and rejects duplicate actions in flight`() = runTest {
        val repo = Repository().apply { gate = CompletableDeferred() }
        val vm = NotificationsViewModel(repo)
        val ids = mutableListOf(1,2,1)
        vm.markVisibleRead(ids); ids.clear(); vm.markAllRead(); runCurrent()
        assertEquals(listOf(1,2),repo.ids); assertEquals(1,repo.calls); assertTrue(vm.marking.value)
        repo.gate!!.complete(Unit); runCurrent(); assertFalse(vm.marking.value)
    }
    @Test fun `mark failure is surfaced and a successful retry resets it`() = runTest {
        val repo = Repository().apply { fail = true }; val vm = NotificationsViewModel(repo)
        vm.markVisibleRead(listOf(1)); runCurrent(); assertTrue(vm.markError.value); assertFalse(vm.marking.value)
        repo.fail = false; vm.markVisibleRead(listOf(1)); runCurrent(); assertFalse(vm.markError.value); assertEquals(2,repo.calls)
    }
    @Test fun `empty selection does no storage work`() = runTest {
        val repo = Repository(); val vm = NotificationsViewModel(repo)
        vm.markVisibleRead(emptyList()); runCurrent(); assertEquals(0,repo.calls); assertFalse(vm.marking.value)
    }
}
