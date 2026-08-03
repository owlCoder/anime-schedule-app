package com.owlcoder.animeschedule.presentation.screens.schedule

import com.owlcoder.animeschedule.domain.model.AiringEpisode
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** The small, deterministic slice of the schedule that belongs on the Today dashboard. */
data class DashboardScheduleSelection(
    val featured: AiringEpisode?,
    val upcoming: List<AiringEpisode>,
)

object DashboardScheduleSelector {
    private val defaultLookAhead: Duration = Duration.ofMinutes(90)
    private val defaultRecentlyAiredWindow: Duration = Duration.ofMinutes(30)

    fun select(
        episodes: List<AiringEpisode>,
        clock: Clock,
        lookAhead: Duration = defaultLookAhead,
        recentlyAiredWindow: Duration = defaultRecentlyAiredWindow,
        maxUpcoming: Int = 4,
    ): DashboardScheduleSelection = select(
        episodes = episodes,
        now = Instant.now(clock),
        lookAhead = lookAhead,
        recentlyAiredWindow = recentlyAiredWindow,
        maxUpcoming = maxUpcoming,
    )

    fun select(
        episodes: List<AiringEpisode>,
        now: Instant,
        lookAhead: Duration = defaultLookAhead,
        recentlyAiredWindow: Duration = defaultRecentlyAiredWindow,
        maxUpcoming: Int = 4,
    ): DashboardScheduleSelection {
        if (episodes.isEmpty() || maxUpcoming <= 0 || lookAhead.isNegative || recentlyAiredWindow.isNegative) {
            return DashboardScheduleSelection(featured = null, upcoming = emptyList())
        }

        val uniqueEpisodes = episodes
            .distinctBy { it.airingId }
            .sortedBy { it.airingAtEpochSeconds }

        val featured = uniqueEpisodes
            .filter { episode ->
                val airedAt = Instant.ofEpochSecond(episode.airingAtEpochSeconds)
                !airedAt.isAfter(now) && !airedAt.isBefore(now.minus(recentlyAiredWindow))
            }
            .maxByOrNull { it.airingAtEpochSeconds }
            ?: uniqueEpisodes.firstOrNull { Instant.ofEpochSecond(it.airingAtEpochSeconds).isAfter(now) }

        val end = now.plus(lookAhead)
        val upcoming = uniqueEpisodes
            .asSequence()
            .filter { it.airingId != featured?.airingId }
            .filter { episode ->
                val airingAt = Instant.ofEpochSecond(episode.airingAtEpochSeconds)
                airingAt.isAfter(now) && !airingAt.isAfter(end)
            }
            .take(maxUpcoming)
            .toList()

        return DashboardScheduleSelection(featured = featured, upcoming = upcoming)
    }
}
