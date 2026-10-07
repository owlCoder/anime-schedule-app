package com.owlcoder.animeschedule.presentation.screens.mylist

import com.owlcoder.animeschedule.domain.model.MalListEntry
import org.junit.Assert.*
import org.junit.Test

class ListSharingTest {
    @Test fun `sharing bounds payload preserves result order and removes title newlines`() {
        val rows = (205 downTo 1).map { MalListEntry(it, "Title\n$it", status = com.owlcoder.animeschedule.domain.model.WatchStatus.WATCHING, episodesWatched = 0, score = 0, totalEpisodes = null) }
        val result = rows.toListShareText("205 titles", { it.title }, { "More: $it" })
        assertTrue(result.startsWith("205 titles\n\n1. Title 205\n2. Title 204"))
        assertTrue(result.endsWith("200. Title 6\nMore: 5"))
        assertFalse(result.contains("Title 5\n"))
    }
}
