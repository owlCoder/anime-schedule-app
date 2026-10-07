package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.domain.model.WatchActivity

/** Exports exactly the visible records, with CSV quoting and formula neutralization. */
internal fun List<WatchActivity>.toActivityCsv(): String = buildString {
    append("\uFEFFmal_id,title,date,episode_delta,progress\r\n")
    this@toActivityCsv.forEach { activity ->
        val cells = listOf(activity.animeId.toString(), activity.title, activity.date, activity.episodeDelta.toString(), activity.progress.toString())
        append(cells.mapIndexed { index, value ->
            val safe = if (index in 1..2 && (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || value.startsWith('\t'))) "'$value" else value
            "\"${safe.replace("\"", "\"\"")}\""
        }.joinToString(","))
        append("\r\n")
    }
}
