package com.owlcoder.animeschedule.presentation.screens.schedule

import com.owlcoder.animeschedule.domain.model.AiringEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DashboardScheduleSelectorTest {
    private val now = Instant.parse("2026-08-03T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `featured is the most recently aired item when it is still current`() {
        val selection = DashboardScheduleSelector.select(
            episodes = listOf(episode(11, now.minusSeconds(1_800)), episode(12, now.minusSeconds(300))),
            clock = clock,
        )

        assertEquals(12, selection.featured?.airingId)
        assertEquals(emptyList<AiringEpisode>(), selection.upcoming)
    }

    @Test
    fun `featured falls back to next future episode`() {
        val selection = DashboardScheduleSelector.select(
            episodes = listOf(episode(1, now.plusSeconds(4_000)), episode(2, now.plusSeconds(600))),
            clock = clock,
        )

        assertEquals(2, selection.featured?.airingId)
        assertEquals(listOf(1), selection.upcoming.map { it.airingId })
    }

    @Test
    fun `upcoming is capped at four and never includes episodes after ninety minutes`() {
        val selection = DashboardScheduleSelector.select(
            episodes = (1..6).map { episode(it, now.plusSeconds(it * 900L)) } + episode(99, now.plusSeconds(5_401)),
            clock = clock,
        )

        assertEquals(1, selection.featured?.airingId)
        assertEquals(listOf(2, 3, 4, 5), selection.upcoming.map { it.airingId })
    }

    @Test
    fun `duplicate airing ids are removed before selection`() {
        val selection = DashboardScheduleSelector.select(
            episodes = listOf(episode(7, now.plusSeconds(600)), episode(7, now.plusSeconds(600)), episode(8, now.plusSeconds(1_200))),
            clock = clock,
        )

        assertEquals(7, selection.featured?.airingId)
        assertEquals(listOf(8), selection.upcoming.map { it.airingId })
    }

    @Test
    fun `no featured or upcoming item is returned when all episodes are old`() {
        val selection = DashboardScheduleSelector.select(
            episodes = listOf(episode(1, now.minusSeconds(1_801))),
            clock = clock,
        )

        assertNull(selection.featured)
        assertEquals(emptyList<AiringEpisode>(), selection.upcoming)
    }

    private fun episode(id: Int, airingAt: Instant) = AiringEpisode(
        airingId = id,
        animeId = id,
        malId = null,
        episode = 1,
        airingAtEpochSeconds = airingAt.epochSecond,
        title = "Anime $id",
        titleRomaji = null,
        coverImageUrl = null,
        coverColor = null,
        genres = emptyList(),
        averageScore = null,
        totalEpisodes = null,
        status = null,
        format = null,
        malListEntry = null,
    )
}
