package com.owlcoder.animeschedule.presentation.screens.schedule

import com.owlcoder.animeschedule.domain.model.AiringEpisode
import java.time.Clock
import java.time.Duration
import java.time.Instant

enum class DashboardScheduleMode {
    UPCOMING,
    LATER_TODAY,
    EARLIER_TODAY,
}

/** A small, deterministic slice of the full day that always keeps the dashboard useful. */
data class DashboardScheduleSelection(
    val featured: AiringEpisode?,
    val upcoming: List<AiringEpisode>,
    val mode: DashboardScheduleMode,
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
            return DashboardScheduleSelection(
                featured = null,
                upcoming = emptyList(),
                mode = DashboardScheduleMode.UPCOMING,
            )
        }

        val uniqueEpisodes = episodes
            .distinctBy { it.airingId }
            .sortedBy { it.airingAtEpochSeconds }

        val recentlyAired = uniqueEpisodes
            .asSequence()
            .filter { episode ->
                val airedAt = Instant.ofEpochSecond(episode.airingAtEpochSeconds)
                !airedAt.isAfter(now) && !airedAt.isBefore(now.minus(recentlyAiredWindow))
            }
            .maxByOrNull { it.airingAtEpochSeconds }

        val futureEpisodes = uniqueEpisodes.filter {
            Instant.ofEpochSecond(it.airingAtEpochSeconds).isAfter(now)
        }
        val lookAheadEnd = now.plus(lookAhead)
        val nearFuture = futureEpisodes.filter {
            !Instant.ofEpochSecond(it.airingAtEpochSeconds).isAfter(lookAheadEnd)
        }

        val mode = when {
            recentlyAired != null || nearFuture.isNotEmpty() -> DashboardScheduleMode.UPCOMING
            futureEpisodes.isNotEmpty() -> DashboardScheduleMode.LATER_TODAY
            else -> DashboardScheduleMode.EARLIER_TODAY
        }

        val featured = recentlyAired
            ?: futureEpisodes.firstOrNull()
            ?: uniqueEpisodes.lastOrNull()

        val supporting = when (mode) {
            DashboardScheduleMode.UPCOMING -> nearFuture
                .asSequence()
                .filter { it.airingId != featured?.airingId }
                .take(maxUpcoming)
                .toList()

            DashboardScheduleMode.LATER_TODAY -> futureEpisodes
                .asSequence()
                .filter { it.airingId != featured?.airingId }
                .take(maxUpcoming)
                .toList()

            DashboardScheduleMode.EARLIER_TODAY -> uniqueEpisodes
                .asReversed()
                .asSequence()
                .filter { it.airingId != featured?.airingId }
                .take(maxUpcoming)
                .toList()
        }

        return DashboardScheduleSelection(
            featured = featured,
            upcoming = supporting,
            mode = mode,
        )
    }
}
