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

Goal: show today/tomorrow/week anime airing schedule in the user’s timezone and allow MyAnimeList login/list updates from one ad-free app.

## Recommended Stack

* Platform: Android native
* Language: Kotlin
* UI: Jetpack Compose + Material 3/Material You latest UI
* Architecture: MVVM + Clean Architecture
* Async/state: Coroutines, Flow, StateFlow
* Networking: Ktor Client or Retrofit + OkHttp
* GraphQL: Apollo Kotlin for AniList
* Local cache: Room
* Settings: DataStore Preferences
* DI: Hilt
* Images: Coil
* Background work: WorkManager
* Auth: OAuth 2.0 Authorization Code + PKCE via Chrome Custom Tabs
* Secure storage: AndroidX Security Crypto / EncryptedSharedPreferences
* Serialization: Kotlinx Serialization or Moshi
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

Use this structure unless the existing project already defines a better one.

```text
app/
  presentation/
  domain/
  data/
  core/
```

Rules:

* Presentation owns Compose screens, navigation, ViewModels, and UI state.
* Domain owns models, repository interfaces, and use cases.
* Data owns API clients, Room, DataStore, secure storage, mappers, and repository implementations.
* Core owns shared networking, time, result/error, and UI utilities.
* Keep API DTOs out of UI.
* Keep Room entities out of UI.
* Map remote/local models to domain models before presentation.

## MVP Scope

Implement first:

* Today schedule
* Tomorrow schedule
* This week schedule
* Anime details
* Settings with timezone override
* MAL OAuth login/logout
* MAL list read
* MAL status/episode/score update where API supports it
* Quick `+1 episode` action
* Offline cache and clear error states

Defer:

* Social features
* Recommendations
* Comments/reviews
* Custom accounts outside MAL OAuth
* Multi-platform/iOS unless explicitly requested

## Build and Checks

Before finishing a coding task, run the smallest relevant checks.

Prefer:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

For Linux/macOS:

```bash
./gradlew test
./gradlew assembleDebug
```

If checks cannot run, state exactly why and what remains unverified.
