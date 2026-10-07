package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentSearchesDataStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun `single removal persists after reopening and does not reorder remaining history`() = runTest {
        val file = File(temporary.root, "recents.preferences_pb")
        var job = SupervisorJob()
        var backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        val store = RecentSearchesDataStore(backing)
        store.save("Alpha"); store.save("Beta"); store.save("Gamma")
        store.remove("Beta"); store.remove("Missing")
        assertEquals(listOf("Gamma", "Alpha"), store.recentSearchesFlow.first())
        job.cancelAndJoin()
        job = SupervisorJob()
        backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        try {
            val reopened = RecentSearchesDataStore(backing)
            assertEquals(listOf("Gamma", "Alpha"), reopened.recentSearchesFlow.first())
            reopened.remove("Gamma"); reopened.remove("Alpha")
            assertTrue(reopened.recentSearchesFlow.first().isEmpty())
        } finally { job.cancelAndJoin() }
    }
}
