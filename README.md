# Anime Schedule

Native Android app for tracking anime airing schedules and managing your MyAnimeList list — ad-free.

## Features

- **Today / Tomorrow / next 7 days** airing schedule, with title search, upcoming-only and own-list filters, shown in your device time zone or a time zone you pick
- **Anime details** — cover, banner, synopsis, studios, characters, relations, countdown to the next episode, and links to the watch sources you configure
- **Seasonal browser** — every anime of a season with title search, hide-tracked toggle, genre/format filters and sorting
- **List insights** — sort by recent edits, title, score, progress, or remaining episodes; see completed titles, watched episodes, average score, and your Watching backlog
- **Episode editor** — grouped status, large progress controls, direct entry, ±10 steps, ratings, notes and safe removal; consecutive edits reuse the schedule overlay
- **Personal tracking tools** — account-scoped local favorites and notes, All-status and Unrated filters, random picks from current results, and UTF-8 CSV export through the Android document picker
- **Activity and goals** — the latest 300 local progress changes (including offline edits), with a configurable Monday–Sunday episode goal using your schedule timezone
- **Themes** — eleven predefined palettes, Classic accent colors, Android wallpaper colors, saved looks, scheduled light/dark hours, compact layouts, AMOLED canvas, higher contrast and reduced animations
- **Share anime** — send the title and AniList link with the native Android share sheet
- **MyAnimeList integration** — OAuth 2.0 login, list read/update, a "+1 episode" quick action, and offline edits that are queued and delivered when you are back online
- **Search** — find anime by title (paged results, recent searches, list status editing)
- **Notifications** — optional local notifications when an anime on your *Watching* list airs, plus an in-app history with an unread badge
- **Watch sources** — your own search-URL templates (Crunchyroll and Netflix are preset), opened in the app's built-in browser with an ad/tracker filter or in another app
- **Settings** — searchable options, one Appearance menu for themes and accent colors, time zone override, notification timing, language, cache retention, episode duration and personal backups
- **Languages** — English and Serbian (Latin), switchable without restarting the app
- **Onboarding** and an in-app **changelog**

## Tech stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3, Haze (backdrop blur) |
| Architecture | MVVM + repositories (UI → ViewModel → domain interfaces → data) |
| Async | Kotlin Coroutines + Flow / StateFlow |
| Networking | Retrofit + OkHttp (REST), Apollo Kotlin (GraphQL) |
| Local data | Room (cache, MAL list, offline edit queue), DataStore (preferences) |
| DI | Hilt |
| Images | Coil 3 |
| Background work | WorkManager |
| Auth | OAuth 2.0 + PKCE through Chrome Custom Tabs |
| Secure storage | EncryptedSharedPreferences (OAuth tokens) |

## Architecture

```
presentation/   Compose screens + ViewModels (immutable UiState exposed as StateFlow)
domain/         models and repository interfaces (no Android or network types)
data/
  api/          AniList (Apollo), MyAnimeList (Retrofit), Kitsu and AnimeSchedule.net (Retrofit)
  provider/     circuit breaker + ordered fallback between providers
  local/        Room database, DataStore, encrypted token store, offline catalog
  repository/   implementations of the domain interfaces
  work/         WorkManager jobs and scheduling
core/           result type, time utilities, DI modules, ad-block filter
```

## Data sources

Requests go to providers in a fixed order and fall through to the next one on failure, timeout, rate limiting or an
empty result. A small in-memory circuit breaker stops hammering a provider that is failing.

| Feature | Order |
|---|---|
| Airing schedule | AniList |
| Search | AniList → Kitsu → AnimeSchedule.net → previously cached titles |
| Seasonal lists | AniList → Kitsu → AnimeSchedule.net → previously cached seasons |
| Anime details | AniList → AnimeSchedule.net; a cached row is shown immediately and refreshed in the background |
| Your list | MyAnimeList API v2 (read/write, OAuth) |

Responses are cached in Room so the schedule and previously opened titles keep working offline.

## Background work

All jobs use unique work names, so scheduling is idempotent.

| Job | Schedule |
|---|---|
| Schedule + MAL list sync | every 6 h, network required |
| Airing notification check | every 15 min, local database only |
| Cache cleanup | daily |
| Flush queued MAL edits | one-off, when a connection is available |

## Requirements

- Android 12+ (minSdk 31)
- JDK 17+ to run Gradle (the build provisions a JDK 21 toolchain automatically)
- A MAL API client registered at [myanimelist.net/apiconfig](https://myanimelist.net/apiconfig) for sign-in

## Build

```bash
./gradlew assembleDebug        # debug build
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
./gradlew connectedDebugAndroidTest # Compose overlay regression tests on a connected emulator
./gradlew assembleRelease      # minified (R8) + signed release build
```

`local.properties` (not committed) holds your credentials:

```properties
MAL_CLIENT_ID=your_mal_client_id
MAL_REDIRECT_URI=com.owlcoder.animeschedule://oauth

# Release signing (only needed for assembleRelease)
KEYSTORE_PATH=/path/to/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

Without signing properties you can still validate the release configuration with
`./gradlew minifyReleaseWithR8`.

## Version

Current release: **5.8.0** (version code **30**, October 7, 2026).

The app version is defined by `versionName` / `versionCode` in [app/build.gradle.kts](app/build.gradle.kts); the
in-app changelog (Settings → Changelog) lists what changed in each release.

### New in 5.8.0

1. Hide airing episodes at or below your tracked MAL progress.
2. Show only local favorites in the schedule, matched by MAL ID.
3. Inspect a seven-day overview with daily broadcast counts, unique anime and the busiest day.
4. Export the filtered week as an RFC 5545 ICS calendar with UTC times and personal estimated durations.
5. Share the selected day’s agenda with local airing times through Android’s share sheet.
6. Set notification quiet hours in your schedule time zone, including overnight or whole-day windows.
7. Mute individual anime’s system alerts from detail tools and manage them in notification settings.
8. Filter notification history to all dates, today, seven days or thirty days alongside title search.
9. Search the loaded character list by name/native name and filter by role before opening details.
10. View and copy Romaji, English and native anime titles.

Quiet hours and per-anime muting preserve in-app notification history. Suppressed system alerts
are not replayed later; delayed checks also respect the intended alert time, including the offset. Muted anime are account-scoped local metadata and are included in personal
backups; quiet hours are device preferences. Favorites use MAL IDs; muted alerts use AniList IDs.
Character search uses characters already loaded with the detail (up to twelve from AniList),
so opening it makes no additional catalog request. Calendar exports use currently cached, filtered broadcasts and estimated durations, include no
reminders, and write only to the file chosen by the user. Details tools reuse one sheet, schedule
controls wrap with large fonts, and the detail title panel maintains contrast over banner images.

### New in 5.7.0

1. Filter search results by titles on your list or titles you have not tracked.
2. Combine search format filters from loaded pages.
3. Sort search results by relevance, title, community score or fewest episodes.
4. Remove individual recent searches while preserving the rest of your history.
5. Filter the seasonal catalog by airing, finished or upcoming release status.
6. Limit seasonal results to short (1–13), standard (14–26) or long (27+) series.
7. Set a minimum seasonal community score; unknown metadata is excluded only when constrained.
8. Switch seasonal discovery between posters and a readable list with progress metadata.
9. Move to previous/next seasons with automatic year rollover (1940 through current year + 2).
10. Open a random title from current filtered seasonal results; disabled while loading or empty.

Discovery controls wrap with large fonts, option rows use separate rounded surfaces, and search
results use individually keyed lazy items. A failed next page preserves current results and offers
an explicit retry. Query changes immediately invalidate earlier responses, even during debounce.
Search filters and sorting apply to loaded pages; load more to explore additional results.

### New in 5.6.0

1. Filter your list by an inclusive score range (0 includes unrated); save it with list views.
2. Sort by remaining watch time, including per-anime episode duration; unknown totals sort last.
3. Select multiple current results and add/remove local favorites or pins in one operation.
4. Rename, merge or delete tags across local metadata and saved views, with removal confirmation.
5. View backlog totals by Watching, Plan to Watch and On Hold, with unknown totals and daily-goal estimates.
6. Choose balanced, shortest-finish-first or focused watch-planner allocation.
7. Include/exclude individual Watching titles for the current planning session.
8. Export visible watch activity as UTF-8 CSV through Android’s document picker.
9. Compare two titles’ status, rating, progress, episode duration and remaining time.
10. Filter watch history to all records, the current week, the last 30 or the last 90 days.

Tools are grouped into watch/planning, organization and history/data, with aligned icon tiles,
compact navigation rows, separate rounded surfaces and clear active-filter counts. Multi-selection
uses checkbox accessibility semantics. Local bulk actions never update MAL progress or status.

### New in 5.5.0

1. Pin anime above other results, preserving the selected sort within each group.
2. Save up to eight named list views (search, status, quick filters, tags and sorting).
3. Plan a watch session by available minutes, with pinned-title priority and fair episode allocation.
4. Override episode duration per anime in the editor (or use the global default).
5. Explore a 28-day activity calendar with current and best streaks from retained local history.
6. Set a daily episode goal (0 disables it) using the selected schedule time zone.
7. Combine smart filters for short series, near completion and unstarted titles with existing filters.
8. Search notification titles in unread or read history.
9. Clear all read notifications with an inline confirmation; unread records stay intact.
10. Share current list results as plain text through Android (up to 200 titles, without personal notes).

My List, activity history, tools and notifications use spaced rounded cards and aligned leading-icon
controls. New preferences are account-scoped and included in personal backups; older backups remain
compatible. The planner suggests episodes without updating progress automatically.

### New in 5.4.0

- Search Settings, save up to eight appearance profiles, schedule
  light/dark hours, and use a compact list layout.
- Add personal anime tags and filter by them, search watch history or restrict it to this week,
  search list titles, notes and tags, continue the latest unfinished anime, and estimate remaining watch time with an adjustable
  episode duration.
- Export and restore local personal data and appearance through Android’s document picker.
  Restoration shows a preview and requires an explicit confirmation; it leaves MAL list data
  and authentication unchanged.
- Six additional palettes: Ocean, Lavender, Ember, Ice, Coffee and Neon (11 in total).
  Appearance combines colors, display options and saved looks in one scrolling menu with predefined colors.
  Settings pickers have separate rounded rows; source editing uses one sheet and text actions use leading
  icons and centered labels. Cached schedule content appears immediately during refresh; the
  orbit loader stays within the schedule pane so navigation remains available.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## License

MIT — see [LICENSE](LICENSE)
