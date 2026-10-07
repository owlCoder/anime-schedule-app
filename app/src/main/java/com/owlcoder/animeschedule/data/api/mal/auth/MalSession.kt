package com.owlcoder.animeschedule.data.api.mal.auth

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/** List writes, queue delivery and account changes share one order within the app process. */
@Singleton
class MalSession @Inject constructor() {
    val mutex = Mutex()
    var epoch: Long = 0
        private set
    fun changedAccount() { epoch++ }
}
