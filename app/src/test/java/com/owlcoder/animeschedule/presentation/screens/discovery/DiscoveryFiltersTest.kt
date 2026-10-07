package com.owlcoder.animeschedule.presentation.screens.discovery

import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.screens.seasonal.*
import org.junit.Assert.*
import org.junit.Test

class DiscoveryFiltersTest {
    private val results = listOf(
        AnimeSearchResult(1, 101, "beta", null, null, "TV", "2025", 80.0, 12, MalListEntry(101, "beta", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 12)),
        AnimeSearchResult(2, 102, "Alpha", null, null, "MOVIE", null, 9.0, 1, null),
        AnimeSearchResult(3, null, "Unknown", null, null, null, null, null, null, null),
    )
    @Test fun `tracking and format combine without treating absent MAL id as tracked`() {
        assertEquals(listOf(1), results.discover(SearchFilter(TrackingFilter.TRACKED, setOf("TV"))).map { it.anilistId })
        assertEquals(listOf(2, 3), results.discover(SearchFilter(TrackingFilter.UNTRACKED)).map { it.anilistId })
    }
    @Test fun `score sorting normalizes both score scales and puts unknown last`() {
        assertEquals(listOf(2, 1, 3), results.discover(SearchFilter(sort = SearchSort.SCORE)).map { it.anilistId })
    }
    @Test fun `shortest sorting puts missing and zero episode totals last`() {
        assertEquals(listOf(2, 1, 3), results.discover(SearchFilter(sort = SearchSort.EPISODES)).map { it.anilistId })
        assertEquals(listOf(2, 1, 3), results.map { if (it.anilistId == 3) it.copy(totalEpisodes = 0) else it }.discover(SearchFilter(sort = SearchSort.EPISODES)).map { it.anilistId })
    }
    @Test fun `title sorting ignores case while relevance preserves provider order`() {
        assertEquals(listOf(2, 1, 3), results.discover(SearchFilter(sort = SearchSort.TITLE)).map { it.anilistId })
        assertEquals(results, results.discover(SearchFilter()))
    }
    private fun season(id: Int, episodes: Int?, status: String?, score: Int?) = SeasonalAnimeItem(id, null, "Title $id", null, null, emptyList(), "TV", status, episodes, null, null, score, null)
    @Test fun `seasonal limits combine and exclude unknown metadata only when constrained`() {
        val items = listOf(season(1, 12, "FINISHED", 80), season(2, 24, "RELEASING", 90), season(3, null, null, null), season(4, 13, "FINISHED", 79))
        assertEquals(items, items.applyFilter(SeasonalFilter()))
        assertEquals(listOf(1), items.applyFilter(SeasonalFilter(release = ReleaseFilter.FINISHED, length = EpisodeLength.SHORT, minimumScore = 8)).map { it.anilistId })
        assertEquals(listOf(2), items.applyFilter(SeasonalFilter(release = ReleaseFilter.AIRING)).map { it.anilistId })
    }
    @Test fun `episode length boundaries cover known positive counts without gaps`() {
        assertTrue(EpisodeLength.SHORT.matches(1)); assertTrue(EpisodeLength.SHORT.matches(13))
        assertFalse(EpisodeLength.SHORT.matches(14)); assertFalse(EpisodeLength.SHORT.matches(0))
        assertTrue(EpisodeLength.STANDARD.matches(14)); assertTrue(EpisodeLength.STANDARD.matches(26))
        assertTrue(EpisodeLength.LONG.matches(27)); assertFalse(EpisodeLength.LONG.matches(null))
        assertTrue(EpisodeLength.ANY.matches(null))
    }
    @Test fun `mean score acts as fallback for seasonal minimum`() {
        assertEquals(listOf(1), listOf(season(1, 12, null, null).copy(meanScore = 70), season(2, 12, null, 69)).applyFilter(SeasonalFilter(minimumScore = 7)).map { it.anilistId })
    }
    @Test fun `release status accepts fallback provider vocabulary and leaves unknown unmatched`() {
        assertTrue(ReleaseFilter.AIRING.matches("current")); assertFalse(ReleaseFilter.AIRING.matches("Unrecognized status"))
        assertTrue(ReleaseFilter.UPCOMING.matches("upcoming")); assertTrue(ReleaseFilter.FINISHED.matches("finished"))
        assertFalse(ReleaseFilter.AIRING.matches(null)); assertTrue(ReleaseFilter.ALL.matches(null))
    }
    @Test fun `season navigation rolls between years`() {
        assertEquals(AnimeSeason.FALL to 2024, adjacentSeason(AnimeSeason.WINTER, 2025, -1))
        assertEquals(AnimeSeason.WINTER to 2026, adjacentSeason(AnimeSeason.FALL, 2025, 1))
    }
}
