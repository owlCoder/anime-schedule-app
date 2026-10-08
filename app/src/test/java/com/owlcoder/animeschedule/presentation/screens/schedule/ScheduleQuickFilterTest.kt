package com.owlcoder.animeschedule.presentation.screens.schedule

import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleQuickFilterTest {
    private fun episode(id: Int, title: String, airing: Long, romaji: String? = null, onList: Boolean = false) =
        AiringEpisode(id, id, id, 1, airing, title, romaji, null, null, listOf("Action"), null, 12, null, "TV",
            if (onList) MalListEntry(id, status = WatchStatus.WATCHING, episodesWatched = 0, score = 0, totalEpisodes = 12) else null)

    @Test fun `title filter trims input and checks translated and romaji titles without case sensitivity`() {
        val episodes = listOf(episode(1, "Attack on Titan", 100, "Shingeki no Kyojin"), episode(2, "Naruto", 200))
        assertEquals(listOf(1), episodes.applyFilter(ScheduleFilter(query = "  SHINGEKI  "), 0).map { it.animeId })
        assertEquals(listOf(1), episodes.applyFilter(ScheduleFilter(query = "titan"), 0).map { it.animeId })
    }

    @Test fun `upcoming updates at the airing boundary and combines with list and genre filters`() {
        val episodes = listOf(episode(1, "A", 99, onList = true), episode(2, "B", 100, onList = true),
            episode(3, "C", 101), episode(4, "D", 102, onList = true))
        val filter = ScheduleFilter(upcomingOnly = true, onlyMyList = true, genres = setOf("Action"))
        assertEquals(listOf(4), episodes.applyFilter(filter, 100).map { it.animeId })
        assertEquals(emptyList<Int>(), episodes.applyFilter(filter, 102).map { it.animeId })
        assertEquals(4, episodes.applyFilter(ScheduleFilter(), 102).size)
    }

    @Test fun `muted filter uses AniList ids and word query can span accent insensitive titles`() {
        val episodes = listOf(episode(1, "Čuvaj Dragon", 100, "Weekend"), episode(2, "Dragon", 100, "Weekend").copy(malId = 1))
        val query = ScheduleFilter(query = "  cuvaj weekend ")
        assertEquals(listOf(1), episodes.applyFilter(query, 0).map { it.animeId })
        assertEquals(listOf(2), episodes.applyFilter(ScheduleFilter(hideMuted = true), 0, mutedIds = setOf(1)).map { it.animeId })
        assertEquals(2, episodes.applyFilter(ScheduleFilter(), 0, mutedIds = setOf(1)).size)
    }
}
