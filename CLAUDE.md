# CLAUDE.md

## Context Budget

Use minimal context by default.

* Start with targeted search: `rg`, filenames, imports, Gradle files, routes/navigation, schemas, DTOs, repositories, use cases, ViewModels, tests.
* Read only files directly needed for the task.
* Do not scan whole folders, generated files, build output, `.gradle`, `.idea`, APK/AAB files, or large files unless required.
* Expand scope only for imports, contracts, shared models, API clients, database schemas, auth flow, or failing checks.
* Prefer small diffs over broad rewrites.

## Project

Native Android anime schedule app.

Goal: show today/tomorrow/week anime airing schedule in the user's timezone and allow MyAnimeList login/list updates from one ad-free app.

## Recommended Stack

* Platform: Android native
* Language: Kotlin
* UI: Jetpack Compose + Material 3/Material You latest UI
* Architecture: MVVM + Clean Architecture
* Async/state: Coroutines, Flow, StateFlow
* Networking: Retrofit + OkHttp
* GraphQL: Apollo Kotlin for AniList
* Local cache: Room (version 2, fallbackToDestructiveMigration)
* Settings: DataStore Preferences
* DI: Hilt
* Images: Coil 3
* Background work: WorkManager
* Auth: OAuth 2.0 Authorization Code + PKCE via Chrome Custom Tabs
* Secure storage: AndroidX Security Crypto / EncryptedSharedPreferences
* Serialization: Kotlinx Serialization
* Build: Gradle Kotlin DSL + Version Catalog
* Min SDK: 26+

## Core Rules

* Keep changes small and behavior-preserving unless explicitly asked otherwise.
* Reuse existing screens, navigation, repositories, use cases, DTOs, mappers, database entities, components, utilities, and version catalog entries before adding new ones.
* Use canonical imports; do not add compatibility wrappers for old paths.
* Do not rollback, delete, or overwrite unrelated user changes.
* Avoid `Any`; use named DTOs/models or typed sealed states.
* Validate external/unsafe input at API boundaries.
* Treat files over ~300 lines or methods over ~70 lines as refactor candidates.
* Do not refactor multiple domains in one task unless required.
* Keep user-facing text in Serbian Latin where app copy is requested; keep code and technical identifiers in English.

## Product Rules

* Do not build a WebView wrapper around MyAnimeList.
* Do not scrape private MAL HTML pages.
* Do not store MyAnimeList passwords.
* Do not log access tokens, refresh tokens, auth codes, or user private list data.
* Do not hardcode API tokens/secrets in Git.
* Use official MyAnimeList API v2 + OAuth for login and list updates.
* Use AniList GraphQL as the primary airing schedule source.
* Use Jikan only as a public read-only fallback, never for login or list mutation.

## Multi-user MAL Accounts

The app must support any user logging in with their own MyAnimeList account through OAuth 2.0 Authorization Code + PKCE.

* Do not ask for or store MAL usernames/passwords.
* Do not use a shared MAL account.
* Do not hardcode access tokens.
* Each device/user stores only their own access token and refresh token locally in EncryptedSharedPreferences.
* Store tokens only in encrypted Android storage (AndroidX Security Crypto / EncryptedSharedPreferences).
* Logout must clear all tokens and private cached MAL data (Room mal_list_entries table + DataStore login state).
* All MAL write operations must use the currently authenticated user's token via AuthInterceptor.
* The app uses one MAL API client registration, but every user authorizes separately via OAuth PKCE flow.

## Rate Limits and Caching

Assume every external API is rate-limited.

* Treat AniList as about 90 requests/minute.
* Treat Jikan as 3 requests/second and 60 requests/minute.
* Use cache-first / stale-while-revalidate for schedule and MAL list data.
* Add throttling, debounce, retry with exponential backoff, and graceful error states.
* Respect `Retry-After` on HTTP 429.
* Never poll schedule endpoints continuously.
* Refresh schedule on app open, manual refresh, and limited WorkManager sync.
* Send MAL update requests only after real user actions.

## Timezone Rules

* Store remote airing times as UTC epoch seconds.
* Convert to local display time only in domain/UI using `ZoneId`.
* Default timezone is `ZoneId.systemDefault()`.
* Allow manual timezone override in Settings.
* Do not hardcode Serbia/Belgrade as the only timezone.

## Architecture Boundaries

```text
app/
  presentation/
  domain/
  data/
  core/
```

* Presentation owns Compose screens, navigation, ViewModels, and UI state.
* Domain owns models, repository interfaces, and use cases.
* Data owns API clients, Room, DataStore, secure storage, mappers, and repository implementations.
* Core owns shared networking, time, result/error, and UI utilities.
* Keep API DTOs out of UI.
* Keep Room entities out of UI.
* Map remote/local models to domain models before presentation.

## What Is Already Built

### Navigation (5 tabs)
`Screen`: Schedule, Search, MyList, Notifications, Settings, About, Detail

### Screens
| Screen | File | Notes |
|---|---|---|
| Schedule | `ScheduleScreen.kt` | Today/Tomorrow/Week tabs, PullToRefresh |
| Search | `SearchScreen.kt` | Local query state + debounced VM query |
| MyList | `MyListScreen.kt` | Tabs by WatchStatus, auto-refresh on init |
| Notifications | `NotificationsScreen.kt` | Placeholder with EmptyState |
| Settings | `SettingsScreen.kt` | Profile card (MAL avatar), theme, timezone, notifications |
| About | `AboutScreen.kt` | App info, data sources |
| Detail | `AnimeDetailScreen.kt` | Hero banner, MAL list edit FAB |

### Components
* `AiringEpisodeCard` — accent color strip, cover, countdown, +1 button
* `MyListEntryCard` — cover image, episode count, status chip
* `SearchResultCard` — cover, meta, add/edit button
* `EmptyState` — shared icon + title + subtitle for all empty states
* `ListStatusBottomSheet` — status/episode/score editor
* `ErrorBanner`, `LoadingShimmer`, `CountdownText`

### Key Implementation Details

**MAL OAuth PKCE**
* Method: `plain` (not S256) — MAL does not support S256 correctly
* PKCE verifier stored in `EncryptedSharedPreferences` (survives Activity recreation)
* Redirect URI must NOT be double-encoded: use `@Field(encoded=true)` + `URLEncoder.encode()` before passing

**Anime Detail ID resolution**
* Schedule/search clicks pass AniList media ID → `AnimeDetailQuery($id)`
* My List clicks pass MAL ID → try `AnimeDetailQuery($id)` first, fall back to `AnimeDetailByMalQuery($idMal)`
* `AnimeDetailEntity` has both `animeId` (AniList) and `malId` (nullable MAL ID)
* Detail flow is reactive: `combine(animeDetailDao.getById, malListEntryDao.observeByAnimeId)` — updates immediately after MAL list edit

**Room DB**
* Version 2, `fallbackToDestructiveMigration` (dev mode — no migration scripts needed)
* Tables: `airing_episodes`, `anime_details` (has `malId` index), `mal_list_entries`

**GraphQL schema**
* `schema.graphqls` has `Media(id: Int, idMal: Int, type: MediaType)` — both params supported
* Two queries: `AnimeDetailQuery` (by AniList id) and `AnimeDetailByMalQuery` (by idMal)

**Edge-to-edge insets**
* Root Scaffold: `contentWindowInsets = WindowInsets(0,0,0,0)`
* Each TopAppBar: `windowInsets = WindowInsets.statusBars`
* Bottom navbar: `windowInsetsPadding(WindowInsets.navigationBars)` inside floating pill

**Theme**
* `ThemeMode`: SYSTEM / LIGHT / DARK stored in DataStore
* Telegram-inspired color palette in `Color.kt`

### Pending / Not Yet Done
* In-app notifications system (Room entity, WorkManager, badge on navbar)
* Settings: language support (Serbian/English), changelog screen
* Settings dialogs: iOS-style bottom sheet pickers (currently AlertDialog)
* MyList: title/cover comes from Room cache — already working after refresh
* About screen: bottom text clipped on small screens (needs scroll)

## Build and Checks

```powershell
.\gradlew.bat assembleDebug
```

```bash
./gradlew assembleDebug
```

Requires `local.properties` (not in git):
```
MAL_CLIENT_ID=...
MAL_REDIRECT_URI=rs.owlcoder.animeschedule://oauth
```
