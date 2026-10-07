package com.owlcoder.animeschedule.domain.model

import com.owlcoder.animeschedule.presentation.screens.discovery.*
import java.time.*
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class Workspace5100Test {
    private val date = LocalDate.of(2026, 10, 7)
    private val zone = ZoneId.of("Europe/Belgrade")
    private fun search(id: Int) = AnimeSearchResult(id,id,"Anime $id",null,null,"TV","2026",80.0,12,null)
    @Test fun `search score respects the catalog scale including low and invalid ratings`() {
        val rows = listOf(search(1),search(2).copy(meanScore = 8.0),search(3).copy(meanScore = 79.0),search(4).copy(meanScore = null),search(5).copy(meanScore = 0.0),
            search(6).copy(meanScore = 10.0),search(7).copy(meanScore = 101.0),search(8).copy(meanScore = Double.NaN))
        assertEquals(listOf(1),rows.discover(SearchFilter(minimumScore = 8)).map { it.anilistId })
        assertEquals(0.8, rows[1].communityScore()!!, 0.0001)
        assertEquals(1.0, rows[5].communityScore()!!, 0.0001)
        assertNull(rows[6].communityScore());assertNull(rows[7].communityScore())
        assertEquals(8,rows.discover(SearchFilter()).size)
    }
    @Test fun `search length ranges include boundaries and exclude invalid counts`() {
        val rows = listOf(1,13,14,26,27,0).mapIndexed { i,n -> search(i+1).copy(totalEpisodes=n) } + search(7).copy(totalEpisodes=null)
        assertEquals(listOf(1,2),rows.discover(SearchFilter(length=EpisodeLength.SHORT)).map { it.anilistId })
        assertEquals(listOf(3,4),rows.discover(SearchFilter(length=EpisodeLength.STANDARD)).map { it.anilistId })
        assertEquals(listOf(5),rows.discover(SearchFilter(length=EpisodeLength.LONG)).map { it.anilistId })
        assertEquals(7,rows.discover(SearchFilter()).size)
    }
    @Test fun `search year is exact and trimmed while absent and malformed years are excluded`() {
        val rows = listOf(search(1),search(2).copy(year=" 2026 "),search(3).copy(year="2025"),search(4).copy(year=null),search(5).copy(year="2026?"))
        assertEquals(listOf(1,2),rows.discover(SearchFilter(year=2026)).map { it.anilistId })
    }
    @Test fun `new search constraints combine with tracking format sort and reset`() {
        val tracked = MalListEntry(1,status=WatchStatus.WATCHING,episodesWatched=0,score=0,totalEpisodes=12)
        val rows = listOf(search(1).copy(userListEntry=tracked),search(2),search(3).copy(meanScore=70.0))
        val f=SearchFilter(tracking=TrackingFilter.TRACKED,formats=setOf("TV"),sort=SearchSort.SCORE,minimumScore=8,length=EpisodeLength.SHORT,year=2026)
        assertTrue(f.isActive);assertEquals(listOf(1),rows.discover(f).map{it.anilistId})
        assertFalse(SearchFilter().isActive);assertEquals(3,rows.discover(SearchFilter()).size)
    }
    private fun episode(id:Int,malId:Int?=id) = AiringEpisode(id,id,malId,1,date.atTime(10,0).atZone(zone).toEpochSecond()+id*60,"Anime $id",null,null,null,emptyList(),null,null,null,null,null)
    @Test fun `agenda estimate counts distinct airings and uses clamped personal durations`() {
        val days = listOf(ScheduleDay(date,listOf(episode(1),episode(1),episode(2))),ScheduleDay(date.plusDays(1),listOf(episode(3),episode(1))))
        val tools=WatchTools(episodeMinutes=24,durationOverrides=mapOf(1 to 48,2 to 999))
        assertEquals(228L,days[0].estimatedWatchMinutes(tools));assertEquals(252L,days.estimatedWatchMinutes(tools))
        assertEquals(0L,emptyList<ScheduleDay>().estimatedWatchMinutes(tools))
    }
    @Test fun `agenda estimate falls back to list MAL id and matches ICS durations`() {
        val e=episode(1,null).copy(malListEntry=MalListEntry(101,status=WatchStatus.WATCHING,episodesWatched=0,score=0,totalEpisodes=null))
        val days=listOf(ScheduleDay(date,listOf(e)));val tools=WatchTools(durationOverrides=mapOf(101 to 48))
        assertEquals(48L,days.estimatedWatchMinutes(tools))
        val ics=days.toCalendarIcs(tools,Instant.EPOCH){"Ep $it"}
        assertTrue(ics.contains("DTEND:20261007T084900Z"))
    }
    @Test fun `next day skips empty past and selected dates regardless of input order`() {
        val days=listOf(ScheduleDay(date.plusDays(3),listOf(episode(3))),ScheduleDay(date, listOf(episode(1))),ScheduleDay(date.plusDays(1),emptyList()),ScheduleDay(date.plusDays(2),listOf(episode(2))))
        assertEquals(date.plusDays(2),days.nextBroadcastDay(date));assertEquals(date.plusDays(3),days.nextBroadcastDay(date.plusDays(2)))
        assertNull(days.nextBroadcastDay(date.plusDays(3)))
    }
    @Test fun `weekly share excludes empty dates orders days and deduplicates broadcasts`() {
        val text=listOf(ScheduleDay(date.plusDays(1),listOf(episode(2))),ScheduleDay(date,listOf(episode(1),episode(1))),ScheduleDay(date.plusDays(2),emptyList())).toWeekAgendaText(zone,Locale.ENGLISH){"Ep $it"}
        assertTrue(text.indexOf("Wednesday")<text.indexOf("Thursday"));assertFalse(text.contains("Friday"))
        assertEquals(1,Regex("Anime 1").findAll(text).count());assertTrue(text.contains("Europe/Belgrade"))
        assertEquals("",emptyList<ScheduleDay>().toWeekAgendaText(zone,Locale.ENGLISH){"Ep $it"})
    }
    private fun detail(id:Int,mal:Int?)=AnimeDetail(animeId=id,malId=mal,titleRomaji="Title",titleEnglish=null,titleNative=null,
        coverImageUrl=null,coverColor=null,bannerImageUrl=null,description=null,genres=emptyList(),averageScore=null,meanScore=null,
        episodes=null,duration=null,status=null,format=null,season=null,seasonYear=null,nextAiringEpisode=null,nextAiringAt=null,
        studios=emptyList(),characters=emptyList(),relations=emptyList(),trailerSite=null,trailerId=null,siteUrl=null,malListEntry=null)

    @Test fun `copied catalog links follow AniList and MAL id spaces without using provider URLs`() {
        assertEquals("https://anilist.co/anime/1",detail(1,101).catalogLink())
        assertEquals("https://myanimelist.net/anime/101",detail(-101,101).catalogLink())
        assertNull(detail(0,null).catalogLink())
        assertEquals("https://myanimelist.net/anime/101",detail(-101,null).copy(malListEntry=MalListEntry(101,status=WatchStatus.WATCHING,episodesWatched=0,score=0,totalEpisodes=null)).catalogLink())
    }
    @Test fun `character sharing contains names and a canonical public link`() {
        assertEquals("Alpha\nアルファ\nhttps://anilist.co/character/1",CharacterDetail(1,"Alpha","アルファ",null,"Private ignored").shareText())
        assertEquals("Alpha",CharacterDetail(0,"Alpha","Alpha",null,null).shareText())
    }
}
