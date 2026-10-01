package com.owlcoder.animeschedule.domain.repository

/** Requests background work without exposing WorkManager to repositories and ViewModels. */
interface WorkScheduler {
    /** Delivers MAL list edits queued while offline as soon as a connection is available. */
    fun scheduleFlushPendingUpdates()

    /** Checks the cached schedule for episodes that just aired and posts their notifications. */
    fun checkAiringNotifications()
}
