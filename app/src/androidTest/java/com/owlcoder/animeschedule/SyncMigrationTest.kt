package com.owlcoder.animeschedule

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.di.DatabaseModule
import com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import org.junit.*
import org.junit.Assert.*

class SyncMigrationTest {
    private val name = "qa-5110-migration.db"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AnimeScheduleDatabase::class.java)
    @After fun cleanup() { context.deleteDatabase(name) }
    @Test fun upgradePreservesOfflineEditsListNotificationsAndSources() {
        helper.createDatabase(name, 8).apply {
            execSQL("INSERT INTO pending_list_updates VALUES (101, 'watching', 5, 8, 0, 123)")
            execSQL("INSERT INTO mal_list_entries VALUES (101, 101, 'Fixture', NULL, 12, 'watching', 5, 8, NULL)")
            execSQL("INSERT INTO notifications VALUES (501, 101, 'Fixture', 6, NULL, 100, 0, 100)")
            execSQL("INSERT INTO watch_sources VALUES (1, 'Fixture source', 'https://example.com/{query}', NULL, 0, 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 10, true, DatabaseModule.MIGRATION_8_9, DatabaseModule.MIGRATION_9_10).apply {
            query("SELECT numWatchedEpisodes, rejected FROM pending_list_updates WHERE animeId=101").use {
                assertTrue(it.moveToFirst()); assertEquals(5, it.getInt(0)); assertEquals(0, it.getInt(1))
            }
            for (table in listOf("mal_list_entries", "notifications", "watch_sources")) query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
            }
            execSQL("INSERT INTO notification_actions VALUES (501, 101, 1, 0, NULL, 'fixture-token')")
            query("SELECT progressHandled FROM notification_actions WHERE notificationId=501").use {
                assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0))
            }
            close()
        }
    }
    @Test fun receiptUpgradePreservesDeduplicationAndAddsANewActionIdentity() {
        helper.createDatabase(name, 9).apply {
            execSQL("INSERT INTO notification_actions VALUES (501, 101, 1, 0, NULL)")
            close()
        }
        helper.runMigrationsAndValidate(name, 10, true, DatabaseModule.MIGRATION_9_10).apply {
            query("SELECT progressHandled, actionToken FROM notification_actions WHERE notificationId=501").use {
                assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)); assertTrue(it.getString(1).isNotBlank())
            }
            close()
        }
    }

}
