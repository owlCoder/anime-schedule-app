package com.owlcoder.animeschedule

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.mal.*
import com.owlcoder.animeschedule.data.api.mal.auth.*
import com.owlcoder.animeschedule.data.api.mal.dto.*
import com.owlcoder.animeschedule.data.local.datastore.*
import com.owlcoder.animeschedule.data.local.db.*
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.data.repository.*
import com.owlcoder.animeschedule.data.work.*
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.WorkScheduler
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import retrofit2.Response

/** Isolated Room/DataStore/encrypted preferences; never uses or changes a real MAL account. */
class MalSyncAndActionsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefix = "qa-5110-${UUID.randomUUID()}"
    private val dataFile = File(context.cacheDir, "$prefix.preferences_pb")
    private val dataJob = SupervisorJob()
    private lateinit var db: AnimeScheduleDatabase
    private lateinit var prefs: UserPreferencesDataStore
    private lateinit var tools: WatchToolsStore
    private lateinit var tokens: SecureTokenStore
    private lateinit var manager: MalAuthManager
    private lateinit var repository: MalRepositoryImpl
    private lateinit var handler: NotificationActions
    private val api = FixtureApi()
    private val session = MalSession()
    private val work = object : WorkScheduler {
        override fun scheduleFlushPendingUpdates() = Unit
        override fun checkAiringNotifications() = Unit
    }
    private val ids = listOf(9_115_101, 9_115_102, 9_115_103)
    private val isolated = object : ContextWrapper(context) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int) = context.getSharedPreferences("$prefix-$name", mode)
        override fun deleteSharedPreferences(name: String) = context.deleteSharedPreferences("$prefix-$name")
    }
    @Before fun setup() = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        db = Room.databaseBuilder(context, AnimeScheduleDatabase::class.java, "$prefix.db").build()
        val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(dataJob + Dispatchers.IO)) { dataFile }
        prefs = UserPreferencesDataStore(data); tools = WatchToolsStore(data, prefs)
        tokens = SecureTokenStore(isolated)
        manager = MalAuthManager(FixtureAuth, api, tokens, prefs, db.malListEntryDao(), db.pendingListUpdateDao(), session, db.notificationActionDao(), work, database = db)
        createRepository()
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        prefs.setNotificationsEnabled(true)
        seed()
    }
    private fun createRepository() {
        repository = MalRepositoryImpl(api, db.malListEntryDao(), db.pendingListUpdateDao(), db.animeDetailDao(), manager, prefs, work, tools, session, db, db.notificationActionDao())
        handler = NotificationActions(context, repository, db.notificationActionDao(), db.notificationDao(), db.malListEntryDao(), prefs, tools, NotificationPoster(context), session)
    }
    private suspend fun seed() {
        api.status = MalListStatus("watching", 4, 8)
        db.malListEntryDao().upsert(MalListEntryEntity(101, 101, "Fixture Adventure", null, 12, "watching", 4, 8, null))
    }
    private suspend fun notification(id: Int, until: Long? = null, generation: Int = 0) {
        db.notificationDao().upsert(NotificationEntity(id, 101, "Fixture Adventure", 5, null, 100, false, 100))
        db.notificationActionDao().insert(NotificationActionEntity(id, 101, snoozeGeneration = generation, snoozedUntilEpochMs = until))
    }
    @After fun cleanup() = runBlocking {
        for (id in ids) {
            NotificationPoster(context).cancel(id)
            WorkManager.getInstance(context).cancelAllWorkByTag(NotificationActions.reminderTag(id)).result.get()
        }
        dataJob.cancelAndJoin(); db.close(); context.deleteDatabase("$prefix.db"); dataFile.delete()
        isolated.deleteSharedPreferences("secure_tokens")
        Unit
    }
    @Test fun oauthStateOfflineRecoveryUndoAndLogout() = runBlocking {
        val auth = AuthRepositoryImpl(manager, tokens, prefs)
        val uri = android.net.Uri.parse(auth.beginLogin())
        assertEquals("plain", uri.getQueryParameter("code_challenge_method"))
        assertEquals(tokens.getOAuthState(), uri.getQueryParameter("state"))
        assertTrue(auth.completeLogin("fixture-code", tokens.getOAuthState()))
        api.offline = true
        assertTrue(repository.updateListEntry(101, MalListUpdate(WatchStatus.ON_HOLD, 7, 9)) is AppResult.Success)
        assertEquals(1, repository.syncState.first().pendingCount)
        assertEquals(7, db.malListEntryDao().getByAnimeId(101)!!.numEpisodesWatched)
        assertTrue(repository.undoListChange(repository.undoChange.value!!.id) is AppResult.Success)
        assertEquals(4, db.pendingListUpdateDao().getByAnimeId(101)!!.numWatchedEpisodes)
        repository.incrementEpisode(101)
        api.offline = false
        assertTrue(repository.retrySync())
        val state = repository.syncState.first()
        assertEquals(0, state.pendingCount); assertTrue(state.lastSuccessEpochMs > 0)
        assertEquals(5, api.status.numEpisodesWatched)
        api.offline = true; repository.incrementEpisode(101)
        auth.logout()
        assertFalse(prefs.userPreferencesFlow.first().malLoggedIn)
        assertNull(tokens.getMalAccessToken()); assertTrue(db.pendingListUpdateDao().getAll().isEmpty())
        assertTrue(db.malListEntryDao().getAll().first().isEmpty())
        assertTrue(repository.incrementEpisode(101) is AppResult.Error)
    }
    @Test fun expiredSameOwnerPreservesQueueAndDifferentOwnerCannotReceiveIt() = runBlocking {
        api.offline = true; repository.incrementEpisode(101)
        prefs.expireMalSession()
        api.offline = false
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        assertEquals(1, db.pendingListUpdateDao().getAll().size)
        notification(ids[0]); api.username = "OtherFixture"
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        assertTrue(db.pendingListUpdateDao().getAll().isEmpty())
        assertTrue(db.malListEntryDao().getAll().first().isEmpty())
        assertNull(db.notificationActionDao().get(ids[0]))
        assertEquals(0, api.patches)
    }
    @Test fun onlineRemovalClearsTheEarlierOfflineEditWithoutReaddingIt() = runBlocking {
        api.offline = true; repository.incrementEpisode(101)
        assertEquals(1, db.pendingListUpdateDao().getAll().size)
        api.offline = false
        assertTrue(repository.removeListEntry(101) is AppResult.Success)
        assertNull(db.malListEntryDao().getByAnimeId(101))
        assertTrue(db.pendingListUpdateDao().getAll().isEmpty())
        assertTrue(repository.flushPendingUpdates())
        assertEquals(0, api.patches)
    }
    @Test fun backgroundWorkerRetriesOfflineThenDeliversWhenConnectionRecovers() = runBlocking {
        api.offline = true; repository.incrementEpisode(101)
        fun worker() = TestListenableWorkerBuilder<PendingUpdatesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker = PendingUpdatesWorker(appContext, workerParameters, repository)
            }).build()
        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
        assertEquals(1, db.pendingListUpdateDao().getAll().size)
        api.offline = false
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(db.pendingListUpdateDao().getAll().isEmpty())
        assertEquals(5, api.status.numEpisodesWatched)
    }
    private suspend fun snooze(id: Int, generation: Int) = handler.snooze(id, generation, db.notificationActionDao().get(id)!!.actionToken)
    private suspend fun remind(id: Int, generation: Int) = handler.remind(id, generation, db.notificationActionDao().get(id)!!.actionToken)
    private suspend fun actionWorker(id: Int, action: String, generation: Int = 0, token: String? = null): NotificationActionWorker {
        val nonce = token ?: db.notificationActionDao().get(id)!!.actionToken
        return TestListenableWorkerBuilder<NotificationActionWorker>(context)
            .setInputData(workDataOf(NotificationActionReceiver.ID to id, NotificationActionReceiver.GENERATION to generation, NotificationActionReceiver.ACTION to action, NotificationActionReceiver.TOKEN to nonce))
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker = NotificationActionWorker(appContext, workerParameters, handler)
            }).build()
    }
    @Test fun repeatedNotificationActionAfterDatabaseReopenAppliesOnlyOneIncrement() = runBlocking {
        notification(ids[0]); api.offline = true
        val results = listOf(async { actionWorker(ids[0], NotificationActionReceiver.INCREMENT).doWork() }, async { actionWorker(ids[0], NotificationActionReceiver.INCREMENT).doWork() }).awaitAll()
        assertTrue(results.all { it == ListenableWorker.Result.success() })
        assertEquals(5, db.malListEntryDao().getByAnimeId(101)!!.numEpisodesWatched)
        db.close(); db = Room.databaseBuilder(context, AnimeScheduleDatabase::class.java, "$prefix.db").build(); createRepository()
        assertEquals(ListenableWorker.Result.success(), actionWorker(ids[0], NotificationActionReceiver.INCREMENT).doWork())
        assertEquals(5, db.malListEntryDao().getByAnimeId(101)!!.numEpisodesWatched)
        assertEquals(1, db.pendingListUpdateDao().getAll().size)
        api.offline = false; assertTrue(repository.flushPendingUpdates()); assertEquals(5, api.status.numEpisodesWatched)
    }
    @Test fun staleNotificationCannotApplyToAnotherAccountEvenWhenEpisodeIdIsReused() = runBlocking {
        notification(ids[0])
        val oldToken = db.notificationActionDao().get(ids[0])!!.actionToken
        api.username = "OtherFixture"
        assertTrue(manager.handleCallback("fixture-code", "fixture-verifier"))
        seed(); notification(ids[0])
        assertNotEquals(oldToken, db.notificationActionDao().get(ids[0])!!.actionToken)
        assertEquals(ListenableWorker.Result.failure(), actionWorker(ids[0], NotificationActionReceiver.INCREMENT, token = oldToken).doWork())
        assertEquals(4, db.malListEntryDao().getByAnimeId(101)!!.numEpisodesWatched)
        assertEquals(ListenableWorker.Result.success(), actionWorker(ids[0], NotificationActionReceiver.INCREMENT).doWork())
        assertEquals(5, db.malListEntryDao().getByAnimeId(101)!!.numEpisodesWatched)
    }
    @Test fun snoozeDeduplicatesOldTapsAndReminderRespectsMuteAndQuietHours() = runBlocking {
        notification(ids[0])
        assertTrue(snooze(ids[0], 0))
        val first = db.notificationActionDao().get(ids[0])!!
        assertTrue(snooze(ids[0], 0))
        assertEquals(first, db.notificationActionDao().get(ids[0]))
        db.notificationActionDao().finishSnooze(ids[0], 1)
        assertFalse(snooze(ids[0], 0))
        notification(ids[1], System.currentTimeMillis() - 1, 1)
        tools.setNotificationMuted(101, "Fixture Adventure", true)
        remind(ids[1], 1)
        val native = context.getSystemService(NotificationManager::class.java)
        assertFalse(native.activeNotifications.any { it.id == ids[1] })
        assertNull(db.notificationActionDao().get(ids[1])!!.snoozedUntilEpochMs)
        tools.setNotificationMuted(101, "Fixture Adventure", false)
        val hour = java.time.LocalTime.now().hour
        prefs.setQuietHours(QuietHours(true, hour, (hour + 1) % 24))
        notification(ids[2], System.currentTimeMillis() - 1, 1); remind(ids[2], 1)
        assertFalse(native.activeNotifications.any { it.id == ids[2] })
    }
    @Test fun nativeNotificationHasTwoDistinctImmutableActionsAndDueReminderPosts() = runBlocking {
        notification(ids[0], System.currentTimeMillis() - 1, 1)
        remind(ids[0], 1)
        val native = context.getSystemService(NotificationManager::class.java).activeNotifications.single { it.id == ids[0] }.notification
        assertEquals(2, native.actions.size)
        assertEquals(context.getString(R.string.notification_increment), native.actions[0].title.toString())
        assertEquals(context.getString(R.string.notification_snooze), native.actions[1].title.toString())
        assertTrue(native.actions.all { it.actionIntent.isImmutable }); assertNotEquals(native.actions[0].actionIntent, native.actions[1].actionIntent)
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar expand-notifications").use { fd -> java.io.FileInputStream(fd.fileDescriptor).readBytes() }
        // Wait for the system shade transition, which need not emit accessibility events.
        Thread.sleep(1200)
        instrumentation.uiAutomation.waitForIdle(500, 5000)
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "qa-5110-system-actions.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse").close()
    }
    private object FixtureAuth : MalAuthService {
        override suspend fun exchangeToken(clientId: String, code: String, codeVerifier: String, grantType: String, redirectUri: String) = MalTokenResponse("fixture-access", "fixture-refresh", 3600)
        override suspend fun refreshToken(clientId: String, refreshToken: String, grantType: String) = MalTokenResponse("fixture-access", "fixture-refresh", 3600)
    }
    private class FixtureApi : MalApiService {
        var offline = false; var username = "Fixture"; var patches = 0
        var status = MalListStatus("watching", 4, 8)
        private fun network() { if (offline) throw IOException("Fixture offline") }
        override suspend fun getMe(fields: String): MalUserResponse { network(); return MalUserResponse(1, username) }
        override suspend fun getUserAnimeList(fields: String, limit: Int, offset: Int, nsfw: Boolean): MalAnimeListResponse {
            network(); return MalAnimeListResponse(listOf(MalAnimeListItem(MalAnimeNode(101, "Fixture Adventure", numEpisodes = 12, myListStatus = status))))
        }
        override suspend fun updateListStatus(animeId: Int, status: String?, numWatchedEpisodes: Int?, score: Int?): MalListStatus {
            network(); patches++; this.status = this.status.copy(status = status ?: this.status.status, numEpisodesWatched = numWatchedEpisodes ?: this.status.numEpisodesWatched, score = score ?: this.status.score); return this.status
        }
        override suspend fun deleteListStatus(animeId: Int) = Response.success<Unit>(Unit)
        override suspend fun getAnimeDetail(malId: Int, fields: String) = MalAnimeNode(101, "Fixture Adventure", numEpisodes = 12, myListStatus = status)
    }
}
