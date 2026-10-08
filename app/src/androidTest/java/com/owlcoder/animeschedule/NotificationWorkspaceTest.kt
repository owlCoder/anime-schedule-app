package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import com.owlcoder.animeschedule.data.local.db.NotificationEntity
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import com.owlcoder.animeschedule.presentation.screens.notifications.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NotificationWorkspaceTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    @After fun cleanup() { instrumentation.runOnMainSync { models.clear() } }

    private class Repository : NotificationRepository {
        val rows = MutableStateFlow(listOf(
            AppNotification(1,101,"Alpha Adventure",5,null,0,false,1_791_360_000),
            AppNotification(2,102,"Beta Journey",3,null,0,false,1_791_360_000),
            AppNotification(3,101,"Alpha Adventure",4,null,0,true,1_791_273_600),
            AppNotification(4,103,"Gamma Sunset",2,null,0,true,1_791_273_600)))
        var fail = false
        var deletes = 0
        override fun getAll() = rows
        override fun getUnreadCount() = rows.map { items -> items.count { !it.isRead } }
        override suspend fun markRead(id: Int) { rows.value = rows.value.map { if (it.id == id) it.copy(isRead = true) else it } }
        override suspend fun setRead(id: Int, read: Boolean) {
            if (fail) error("Storage unavailable")
            rows.value=rows.value.map { if(it.id==id) it.copy(isRead=read) else it }
        }
        override suspend fun markAllRead() { rows.value = rows.value.map { it.copy(isRead = true) } }
        override suspend fun deleteRead(): Int {
            deletes++
            if (fail) error("Storage unavailable")
            val count = rows.value.count { it.isRead }
            rows.value = rows.value.filterNot { it.isRead }
            return count
        }
    }

    private fun show(repo: Repository) {
        val vm = NotificationsViewModel(repo).also { models.put("notifications", it) }
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(palette = ThemePalette.OCEAN)) {
                NotificationsOverlay({}, {}, vm)
            }
        }
        compose.waitUntil(5_000) { vm.notifications.value.size == repo.rows.value.size }
    }
    private fun search() = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("notification-search")))
    private fun readTab() = compose.onNodeWithText(text(R.string.notif_tab_read), substring = true).performClick()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5100-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
    }

    @Test fun titleSearchCombinesWithTabsAndClearsEmptyResults() {
        val repo = Repository(); show(repo)
        search().performTextInput("alpha")
        compose.onNodeWithText("Alpha Adventure").assertIsDisplayed()
        compose.onNodeWithText("Beta Journey").assertDoesNotExist()
        readTab()
        compose.onNodeWithText("Alpha Adventure").assertIsDisplayed()
        compose.onNodeWithText("Gamma Sunset").assertDoesNotExist()
        search().performTextReplacement("missing")
        compose.onNodeWithText(text(R.string.notif_search_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.search_clear_query)).performClick()
        compose.onNodeWithText("Gamma Sunset").assertIsDisplayed()
        screenshot("notifications-read")
    }

    @Test fun clearingRequiresConfirmationAndPreservesUnreadEvenDuringSearch() {
        val repo = Repository(); show(repo); readTab()
        search().performTextInput("alpha")
        compose.onNodeWithTag("notif-clear-read").performClick()
        compose.onNodeWithTag("notif-clear-cancel").performClick()
        assertEquals(0, repo.deletes); assertEquals(4, repo.rows.value.size)
        compose.onNodeWithTag("notif-clear-read").performClick()
        screenshot("notifications-clear-confirmation")
        compose.onNodeWithTag("notif-clear-confirm").performClick()
        compose.waitUntil(5_000) { repo.rows.value.size == 2 }
        assertTrue(repo.rows.value.none { it.isRead })
        compose.onNodeWithText(text(R.string.notif_tab_unread), substring = true).performClick()
        search().performTextClearance()
        compose.onNodeWithText("Alpha Adventure").assertIsDisplayed()
        compose.onNodeWithText("Beta Journey").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test fun failedClearKeepsRecordsAndAllowsRetry() {
        val repo = Repository().apply { fail = true }; show(repo); readTab()
        compose.onNodeWithTag("notif-clear-read").performClick()
        compose.onNodeWithTag("notif-clear-confirm").performClick()
        compose.onNodeWithText(text(R.string.notif_clear_failed)).assertIsDisplayed()
        assertEquals(4, repo.rows.value.size)
        repo.fail = false
        compose.onNodeWithTag("notif-clear-read").performClick()
        compose.onNodeWithTag("notif-clear-confirm").performClick()
        compose.waitUntil(5_000) { repo.rows.value.size == 2 }
        compose.onNodeWithText(text(R.string.notif_clear_failed)).assertDoesNotExist()
    }

    @Test fun databaseDeleteReadPreservesUnreadAndReportsDeletedCount() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, AnimeScheduleDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            dao.upsert(NotificationEntity(1,101,"Unread",5,null,0,false,1))
            dao.upsert(NotificationEntity(2,102,"Read",5,null,0,true,1))
            assertEquals(1, dao.deleteRead())
            assertEquals(listOf(1), dao.getAll().first().map { it.id })
            assertEquals(1, dao.getUnreadCount().first())
            assertEquals(0, dao.deleteRead())
        } finally { db.close() }
    }
    @Test fun filteredMarkLeavesOtherUnreadRecordsUntouched() {
        val repo = Repository(); show(repo)
        search().performTextInput("alpha"); search().performImeAction()
        compose.onNodeWithTag("notif-tools").performClick()
        compose.onNodeWithTag("notif-mark-visible").performClick()
        compose.waitUntil(5000) { repo.rows.value.first { it.id == 1 }.isRead }
        assertFalse(repo.rows.value.first { it.id == 2 }.isRead)
        compose.onNodeWithTag("notif-mark-visible").assertIsNotEnabled()
        androidx.test.espresso.Espresso.pressBack(); compose.waitForIdle()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithText(text(R.string.notif_search_empty)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.search_clear_query)).performClick()
        compose.onNodeWithText("Beta Journey").assertIsDisplayed()
    }
    @Test fun sortChangesChronologicalHistoryAndToolsBackRetainsFilters() {
        val repo = Repository()
        repo.rows.value = repo.rows.value.map { if (it.id == 1) it.copy(createdAtEpochSeconds = 1_700_000_000) else it }
        show(repo)
        compose.onNodeWithTag("notif-tools").performClick()
        compose.onNodeWithTag("notif-sort-OLDEST").performClick().assertIsSelected()
        screenshot("notification-tools")
        androidx.test.espresso.Espresso.pressBack(); compose.waitForIdle()
        val alpha = compose.onNodeWithText("Alpha Adventure").fetchSemanticsNode().boundsInRoot.top
        val beta = compose.onNodeWithText("Beta Journey").fetchSemanticsNode().boundsInRoot.top
        assertTrue(alpha < beta)
        compose.onNodeWithTag("notif-tools").performClick()
        compose.onNodeWithTag("notif-sort-NEWEST").performClick()
        androidx.test.espresso.Espresso.pressBack(); compose.waitForIdle()
        assertTrue(compose.onNodeWithText("Beta Journey").fetchSemanticsNode().boundsInRoot.top < compose.onNodeWithText("Alpha Adventure").fetchSemanticsNode().boundsInRoot.top)
    }
    @Test fun databaseBatchMarksMoreThanSqliteParameterLimitAndPreservesOthers() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, AnimeScheduleDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            for (id in 1..1205) dao.upsert(NotificationEntity(id,id,"Anime $id",1,null,0,false,1))
            dao.markReadBatch((1..1200).toList() + listOf(1,2))
            assertEquals(5,dao.getUnreadCount().first())
            assertEquals(1200,dao.getAll().first().count { it.isRead })
            dao.markReadBatch(emptyList()); assertEquals(5,dao.getUnreadCount().first())
        } finally { db.close() }
    }

    @Test fun toolsUseTheirOwnScrollPositionAfterLongHistory() {
        val repo = Repository()
        repo.rows.value = (1..40).map { AppNotification(it,it,"Anime $it",1,null,0,false,1_791_360_000 - it * 86400L) }
        show(repo)
        compose.onNodeWithTag("notif-history").performScrollToNode(hasText("Anime 40"))
        compose.onNodeWithTag("notif-tools").performClick()
        compose.onNodeWithTag("notif-sort-NEWEST").assertIsDisplayed()
        compose.onNodeWithTag("notif-mark-visible").assertIsDisplayed()
    }

    @Test fun singleReadToggleMovesBetweenTabsWithoutOpeningAnime() {
        val repo=Repository();var opened=0
        val vm=NotificationsViewModel(repo).also { models.put("notifications",it) }
        compose.setContent { AnimeScheduleTheme(themeMode=ThemeMode.DARK,options=ThemeOptions(palette=ThemePalette.OCEAN)) { NotificationsOverlay({opened=it},{},vm) } }
        compose.waitUntil(5000){vm.notifications.value.size==4}
        compose.onNodeWithTag("notif-toggle-1").performClick()
        compose.waitUntil(5000){repo.rows.value.first { it.id==1 }.isRead}
        assertEquals(0,opened);assertFalse(repo.rows.value.first { it.id==2 }.isRead)
        readTab()
        compose.onNodeWithTag("notif-history").performScrollToNode(hasTestTag("notif-toggle-1"))
        compose.onNodeWithTag("notif-toggle-1").performClick()
        compose.waitUntil(5000){!repo.rows.value.first { it.id==1 }.isRead}
        assertEquals(0,opened)
        screenshot("single-notification-status")
    }
    @Test fun singleReadFailureRetainsNotificationAndRetryWorks() {
        val repo=Repository().apply { fail=true };show(repo)
        compose.onNodeWithTag("notif-toggle-1").performClick()
        compose.onNodeWithText(text(R.string.notif_mark_failed)).assertIsDisplayed()
        assertFalse(repo.rows.value.first { it.id==1 }.isRead)
        repo.fail=false
        compose.onNodeWithTag("notif-history").performScrollToNode(hasTestTag("notif-toggle-1"))
        compose.onNodeWithTag("notif-toggle-1").performClick()
        compose.waitUntil(5000){repo.rows.value.first { it.id==1 }.isRead}
        compose.onNodeWithText(text(R.string.notif_mark_failed)).assertDoesNotExist()
    }
    @Test fun databaseReadTogglePreservesOtherRecordsAndCreationTimes() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(instrumentation.targetContext,AnimeScheduleDatabase::class.java).build()
        try {
            val dao=db.notificationDao()
            dao.upsert(NotificationEntity(1,101,"Alpha",1,null,100,true,200))
            dao.upsert(NotificationEntity(2,102,"Beta",1,null,101,false,201))
            dao.setRead(1,false)
            assertEquals(2,dao.getUnreadCount().first())
            val alpha=dao.getAll().first().first { it.id==1 }
            assertEquals(200L,alpha.createdAtEpochSeconds);assertEquals(100L,alpha.airingAtEpochSeconds)
            dao.setRead(1,true);assertEquals(1,dao.getUnreadCount().first())
            assertFalse(dao.getAll().first().first { it.id==2 }.isRead)
        } finally { db.close() }
    }

}
