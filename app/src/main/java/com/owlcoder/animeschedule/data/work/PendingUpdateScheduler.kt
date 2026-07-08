package com.owlcoder.animeschedule.data.work

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over WorkManager so [com.owlcoder.animeschedule.data.repository.MalRepositoryImpl]
 * can request a queue flush without holding a Context (and unit tests can no-op it).
 */
interface PendingUpdateScheduler {
    fun scheduleFlush()
}

@Singleton
class WorkManagerPendingUpdateScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : PendingUpdateScheduler {
    override fun scheduleFlush() = WorkManagerScheduler.scheduleFlushPendingUpdates(context)
}
