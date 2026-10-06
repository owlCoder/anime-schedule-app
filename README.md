# Anime Schedule

Native Android app for tracking anime airing schedules and managing your MyAnimeList list — ad-free.

## Features

- **Today / Tomorrow / next 7 days** airing schedule, with title search, upcoming-only and own-list filters, shown in your device time zone or a time zone you pick
- **Anime details** — cover, banner, synopsis, studios, characters, relations, countdown to the next episode, and links to the watch sources you configure
- **Seasonal browser** — every anime of a season with genre/format filters and sorting
- **List insights** — sort by recent edits, title, score, progress, or remaining episodes; see completed titles, watched episodes, average score, and your Watching backlog
- **Episode editor** — type progress directly, jump ±10 episodes, or mark a known series complete; consecutive edits reuse the schedule overlay
- **Share anime** — send the title and AniList link with the native Android share sheet
- **MyAnimeList integration** — OAuth 2.0 login, list read/update, a "+1 episode" quick action, and offline edits that are queued and delivered when you are back online
- **Search** — find anime by title (infinite scroll, recent searches, list status editing)
- **Notifications** — optional local notifications when an anime on your *Watching* list airs, plus an in-app history with an unread badge
- **Watch sources** — your own search-URL templates (Crunchyroll and Netflix are preset), opened in the app's built-in browser with an ad/tracker filter or in another app
- **Settings** — theme (Light / Dark / System), accent colour, time zone override, notification timing, language, cache retention
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

Current release: **5.2.0** (version code **24**, October 6, 2026).

The app version is defined by `versionName` / `versionCode` in [app/build.gradle.kts](app/build.gradle.kts); the
in-app changelog (Settings → Changelog) lists what changed in each release.

## Privacy

See [PRIVACY.md](PRIVACY.md).

## License

MIT — see [LICENSE](LICENSE)
