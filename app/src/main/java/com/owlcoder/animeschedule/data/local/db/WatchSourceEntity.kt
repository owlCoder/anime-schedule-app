package com.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_sources")
data class WatchSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** Must contain a {query} placeholder that gets replaced with the URL-encoded anime title. */
    val urlTemplate: String,
    val faviconUrl: String?,
    val sortOrder: Int,
    /** When true, links open via a system ACTION_VIEW intent so the OS can hand off to an
     *  installed app (e.g. Netflix) instead of always opening the in-app WebView. */
    val openExternally: Boolean = false
)
