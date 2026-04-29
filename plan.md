# Anime Schedule App — Full Implementation Plan

## Context

The project is a blank Android Compose scaffold (stage 0). Package `rs.owlcoder.animeschedule`, minSdk 31, Kotlin 2.2.10, Compose BOM 2026.02.01, Material 3 already present. No Hilt, Room, Apollo, Retrofit, DataStore, Coil, WorkManager, or Security Crypto yet. Goal: build a complete MVP anime schedule app with modern Material You UI, AniList schedule, MAL OAuth integration, and offline caching.

---

## Phase 1 — Gradle / Dependency Setup

**Files:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build.gradle.kts` (root)

### libs.versions.toml — add versions
```toml
hilt                  = "2.56.2"
hiltNavigationCompose = "1.2.0"
navigationCompose     = "2.9.0"
room                  = "2.7.1"
datastore             = "1.1.2"
lifecycle             = "2.9.1"
activityCompose       = "1.10.1"
coreKtx               = "1.16.0"
retrofit              = "2.11.0"
okhttp                = "4.12.0"
apollo                = "4.1.1"
coil                  = "3.2.0"
workmanager           = "2.10.1"
securityCrypto        = "1.1.0-alpha06"
kotlinxSerialization  = "1.8.1"
coroutines            = "1.10.2"
browser               = "1.8.0"
splashscreen          = "1.0.1"
ksp                   = "2.2.10-2.0.2"
desugar               = "2.1.5"
```

### libs.versions.toml — add libraries
- hilt-android, hilt-android-compiler, hilt-navigation-compose
- navigation-compose
- lifecycle-viewmodel-compose, lifecycle-runtime-compose
- room-runtime, room-ktx, room-compiler
- datastore-preferences
- okhttp-bom, okhttp (no version — BOM), okhttp-logging-interceptor
- retrofit, retrofit2-kotlinx-serialization-converter (jakewharton, version "1.0.0")
- apollo-runtime, apollo-normalized-cache-sqlite
- coil-compose, coil-network-okhttp (group io.coil-kt.coil3)
- work-runtime-ktx, hilt-work, hilt-compiler (androidx.hilt)
- security-crypto
- browser
- kotlinx-serialization-json
- kotlinx-coroutines-android
- core-splashscreen
- desugar_jdk_libs

### libs.versions.toml — add plugins
```toml
kotlin-android       = { id = "org.jetbrains.kotlin.android",               version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization",   version.ref = "kotlin" }
hilt                 = { id = "com.google.dagger.hilt.android",             version.ref = "hilt" }
ksp                  = { id = "com.google.devtools.ksp",                    version.ref = "ksp" }
apollo               = { id = "com.apollographql.apollo",                   version.ref = "apollo" }
```

### app/build.gradle.kts additions
- Add plugins: kotlin-android, kotlin-serialization, hilt, ksp, apollo
- Add `isCoreLibraryDesugaringEnabled = true` to compileOptions (for java.time on API 31)
- Add `buildConfig = true` to buildFeatures
- Add `kotlinOptions { jvmTarget = "11" }`
- Add `defaultConfig` BuildConfig fields: `MAL_CLIENT_ID`, `MAL_REDIRECT_URI` (read from local.properties, never hardcoded)
- Add apollo block:
  ```kotlin
  apollo {
    service("anilist") {
      packageName.set("rs.owlcoder.animeschedule.data.api.anilist.generated")
      schemaFile.set(file("src/main/graphql/anilist/schema.graphqls"))
      srcDir("src/main/graphql/anilist")
    }
  }
  ```
- Add all dependencies (coreLibraryDesugaring, hilt, navigation, lifecycle, room+ksp, datastore, okhttp BOM, retrofit, apollo, coil, workmanager+hilt-work, security-crypto, browser, kotlinx, splashscreen)

### build.gradle.kts (root) — add ksp and apollo to top-level plugins (apply false)

---

## Phase 2 — Core Layer

**Package:** `core/`

| File | Purpose |
|---|---|
| `core/result/AppResult.kt` | `sealed class AppResult<T>` with Success/Error; `sealed class AppError` (Network, RateLimit, GraphQL, Unauthorized, NoCache, Unknown) |
| `core/result/FlowExt.kt` | Extension funs: `mapResult`, `onSuccess`, `onError`, `flatMapResult` |
| `core/network/RetryPolicy.kt` | `suspend fun withRetry(maxAttempts, initialDelayMs, factor, block)` — exponential backoff |
| `core/network/RateLimitInterceptor.kt` | OkHttp interceptor: reads `Retry-After` on 429, delays, retries once |
| `core/network/AuthInterceptor.kt` | OkHttp interceptor: injects MAL Bearer token from SecureTokenStore on `api.myanimelist.net` requests |
| `core/time/TimeUtils.kt` | `epochSecondsToLocalDateTime`, `todayRangeUtc(ZoneId)`, `weekRangeUtc(ZoneId)`, `formatAiringCountdown` (Serbian: "Za 2h 15min") |
| `core/time/AiringDayCalculator.kt` | Maps UTC epoch → user's local calendar date for Today/Tomorrow/Week bucketing |
| `core/di/CoroutinesModule.kt` | Hilt module: `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` qualifier annotations + bindings |

---

## Phase 3 — Data Layer

### 3a. Room Database — `data/local/db/`

**Entities:**
- `AiringEpisodeEntity` — `airingId` PK, `animeId`, `episode`, `airingAtEpochSeconds` (UTC), `title`, `titleRomaji`, `coverImageUrl`, `genres` (JSON string), `cachedAtEpochSeconds`, `source`
- `AnimeDetailEntity` — full detail fields + `cachedAtEpochSeconds`
- `MalListEntryEntity` — `animeId`, `malId`, `status`, `numEpisodesWatched`, `score`, `updatedAt`

**DAOs:**
- `AiringEpisodeDao`: `getAiringEpisodesInRange(from, to): Flow<List<...>>`, `upsertAll`, `deleteStale`
- `AnimeDetailDao`: `getById(id): Flow<...?>`, `upsert`, cache-aware select
- `MalListEntryDao`: `getAll(): Flow<List<...>>`, `getByAnimeId(id)`, `upsert`, `deleteAll`

**`Converters.kt`** — `List<String>` ↔ JSON via kotlinx.serialization

**`AnimeScheduleDatabase.kt`** — `@Database(entities=[...], version=1, exportSchema=true)`

### 3b. DataStore — `data/local/datastore/`

- `UserPreferences.kt` — data class: `timezoneId: String`, `malLoggedIn: Boolean`, `lastScheduleSyncEpoch: Long`
- `UserPreferencesDataStore.kt` — wraps `DataStore<Preferences>`, exposes `Flow<UserPreferences>`, suspend update functions

### 3c. Secure Storage — `data/local/secure/SecureTokenStore.kt`

- Uses `EncryptedSharedPreferences` with `MasterKey`
- `saveMalTokens(access, refresh, expiresAt)`, `getMalAccessToken()`, `getMalRefreshToken()`, `clearMalTokens()`
- Never log token values

### 3d. Apollo / AniList — `data/api/anilist/`

**GraphQL files:** `app/src/main/graphql/anilist/`
- `schema.graphqls` — downloaded via Apollo introspection task
- `AiringSchedule.graphql` — Page → airingSchedules with media fields (id, title, coverImage with color, genres, score, episodes, status)
- `AnimeDetail.graphql` — full Media detail including relations, nextAiringEpisode, studios, trailer

**`AniListApolloClient.kt`** — builds `ApolloClient` with shared OkHttpClient + SQLite normalized cache

**`AniListRemoteDataSource.kt`** — `getAiringSchedule(from, to, page)`, `getAnimeDetail(id)`, handles pagination (pageInfo.hasNextPage loop), maps Apollo exceptions to AppError

### 3e. MAL OAuth — `data/api/mal/auth/`

- `PkceGenerator.kt` — `generateCodeVerifier()`, `generateCodeChallenge(verifier)` (S256), `generateState()`
- `MalAuthManager.kt` — `buildAuthorizationUri(verifier, state)`, `handleCallback(code, verifier)`, `refreshAccessToken()`, `logout()`
- `MalAuthService.kt` (Retrofit) — `@POST` token exchange and refresh endpoints

OAuth redirect URI: `rs.owlcoder.animeschedule://oauth` — stored in `local.properties`, never committed.

### 3f. MAL API — `data/api/mal/`

- `MalApiService.kt` (Retrofit):
  - `getUserAnimeList(fields, limit, offset)` — paged list fetch
  - `updateListStatus(animeId, status, episodes, score)` via `@PATCH`
  - `searchAnime(query, limit, offset, fields)` — `GET /v2/anime?q={query}` with fields: `id, title, main_picture, alternative_titles, start_date, media_type, mean, my_list_status`
  - `getAnimeDetail(malId, fields)` — `GET /v2/anime/{id}` (reuses Detail screen)
- DTOs in `data/api/mal/dto/`: `MalTokenResponse`, `MalAnimeListResponse`, `MalAnimeNode`, `MalListStatus`, `MalSearchResponse` — all `@Serializable`

### 3f-2. Search Repository — `data/repository/SearchRepositoryImpl.kt`

- Calls `MalApiService.searchAnime(query, limit=20, offset=page*20)`
- Merges `my_list_status` from response into `AnimeSearchResult.userListEntry`
- No local cache for search results (always live); Room MAL list used only to enrich status badge
- Maps `MalAnimeNode` → `AnimeSearchResult` via `MalMapper`

### 3f-3. Recent Searches — `data/local/datastore/RecentSearchesDataStore.kt`

- Stores last 10 queries as `Set<String>` in DataStore Preferences
- `getRecentSearches(): Flow<List<String>>`, `save(query)`, `clear()`

### 3g. Jikan Fallback — `data/api/jikan/`

- `JikanApiService.kt` (Retrofit, base URL `https://api.jikan.moe/`) — `getSchedule(dayOfWeek, page)`
- DTOs: `JikanScheduleResponse`, `JikanAnime`
- Rate-limit handled by `RateLimitInterceptor`

### 3h. Mappers — `data/mapper/`

- `AniListMapper.kt` — `AiringSchedule.toEntity()`, `Media.toEntity()` (AnimeDetailEntity)
- `MalMapper.kt` — `MalAnimeNode.toEntity()`, `MalListEntryEntity.toDomain()`
- `JikanMapper.kt` — `JikanAnime.toAiringEpisodeEntity(dayOfWeek)`
- `EntityMapper.kt` — `AiringEpisodeEntity.toDomain()`, `AnimeDetailEntity.toDomain()`

### 3i. Repository Implementations — `data/repository/`

**`ScheduleRepositoryImpl`** — cache-first pattern:
1. Emit Room Flow immediately (stale data OK for display)
2. If `cachedAtEpochSeconds` > 30 min ago, fetch AniList
3. On AniList failure, try Jikan
4. Upsert to Room → Room Flow auto-emits fresh data to collectors

**`AnimeDetailRepositoryImpl`** — same cache-first pattern.

**`MalRepositoryImpl`** — on 401: call `MalAuthManager.refreshAccessToken()`, retry once. On second failure: emit `AppError.Unauthorized`, set logged-out in DataStore.

---

## Phase 4 — Domain Layer

### Models — `domain/model/`

- `AiringEpisode` — airingId, animeId, episode, `airingAtEpochSeconds` (UTC), title, titleRomaji, coverImageUrl, genres, `malListEntry: MalListEntry?`
- `AnimeDetail` — full detail fields
- `MalListEntry` — animeId, `status: WatchStatus`, episodesWatched, score
- `WatchStatus` enum — WATCHING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_WATCH, NOT_IN_LIST
- `ScheduleDay` — `date: LocalDate`, `episodes: List<AiringEpisode>`
- `MalListUpdate` — nullable status, episodesWatched, score
- `AnimeSearchResult` — malId, title, titleEnglish, coverImageUrl, type (TV/Movie/OVA…), year, meanScore, `userListEntry: MalListEntry?`

### Repository Interfaces — `domain/repository/`

- `ScheduleRepository` — `getTodaySchedule(ZoneId)`, `getTomorrowSchedule(ZoneId)`, `getWeekSchedule(ZoneId)`, `suspend refreshSchedule(ZoneId)`
- `AnimeDetailRepository` — `getAnimeDetail(animeId): Flow<AppResult<AnimeDetail>>`
- `MalRepository` — `getUserList()`, `suspend updateListEntry(animeId, MalListUpdate)`, `suspend incrementEpisode(animeId)`, `suspend refreshUserList()`
- `AuthRepository` — `isLoggedIn: Flow<Boolean>`, `buildAuthUri(): Pair<Uri, String>`, `suspend handleOAuthCallback(code, verifier)`, `fun logout()`
- `SettingsRepository` — `userPreferencesFlow: Flow<UserPreferences>`, `suspend setTimezoneId(String)`, `getEffectiveZoneId(): ZoneId`
- `SearchRepository` — `suspend searchAnime(query: String, page: Int): AppResult<List<AnimeSearchResult>>`

### Use Cases — `domain/usecase/`

One class per operation, `operator fun invoke(...)`. Key ones:
- `GetTodayScheduleUseCase`, `GetTomorrowScheduleUseCase`, `GetWeekScheduleUseCase`, `RefreshScheduleUseCase`
- `GetAnimeDetailUseCase`
- `GetMalUserListUseCase`, `UpdateMalListEntryUseCase`, `IncrementEpisodeUseCase`
- `LoginWithMalUseCase`, `HandleMalCallbackUseCase`, `LogoutFromMalUseCase`
- `GetSettingsUseCase`, `SetTimezoneUseCase`
- `SearchAnimeUseCase` — debounce guard (min 2 chars), delegates to `SearchRepository`
- `GetRecentSearchesUseCase`, `SaveRecentSearchUseCase`, `ClearRecentSearchesUseCase`

---

## Phase 5 — Dependency Injection (Hilt)

**All modules in `core/di/`:**

| Module | Provides |
|---|---|
| `DatabaseModule` | `AnimeScheduleDatabase` singleton + all DAOs |
| `NetworkModule` | `OkHttpClient` singleton (auth + rate-limit + logging interceptors), `ApolloClient`, `@Named("mal")` and `@Named("jikan")` Retrofit instances, all API services |
| `DataStoreModule` | `DataStore<Preferences>` singleton |
| `AuthModule` | `SecureTokenStore`, `MalAuthManager`, `AuthInterceptor` |
| `RepositoryModule` | `@Binds` abstract bindings for all 5 repository interfaces |
| `CoroutinesModule` | Named `CoroutineDispatcher` instances |

**`AnimeScheduleApplication.kt`** — `@HiltAndroidApp`, registers WorkManager sync on startup.

---

## Phase 6 — Presentation Layer

### Navigation — `presentation/navigation/`

- `Screen.kt` — sealed class: `Schedule`, `Detail("detail/{animeId}")`, `MyList`, `Search`, `Settings`
- `BottomNavItem.kt` — Schedule (CalendarMonth), Search (Search icon), My List (FormatListBulleted), Settings (Settings icon)
- `AnimeScheduleNavHost.kt` — `NavHost` with composable routes

### MainActivity.kt (rewrite)

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(...) {
        installSplashScreen()
        super.onCreate(...)
        enableEdgeToEdge()
        setContent {
            AnimeScheduleTheme {
                val navController = rememberNavController()
                Scaffold(bottomBar = { AnimeBottomBar(navController) }) { padding ->
                    AnimeScheduleNavHost(navController, Modifier.padding(padding))
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { /* dispatch OAuth redirect */ }
}
```

`android:launchMode="singleTop"` required in manifest for OAuth redirect.

### AndroidManifest.xml additions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<application android:name=".AnimeScheduleApplication" ...>
  <activity android:launchMode="singleTop" ...>
    <!-- existing MAIN intent-filter -->
    <!-- OAuth intent-filter: scheme="rs.owlcoder.animeschedule" host="oauth" -->
  </activity>
</application>
```

### UI State & ViewModels — `presentation/screens/`

**schedule/**
- `ScheduleUiState` — todayEpisodes, tomorrowEpisodes, weekDays, isLoading, error, isLoggedIn, selectedTab
- `ScheduleTab` enum — TODAY ("Danas"), TOMORROW ("Sutra"), WEEK ("Ova nedelja")
- `ScheduleViewModel` (@HiltViewModel) — collects all three schedule flows, handles refresh, tab selection, increment episode

**details/**
- `DetailUiState` — anime detail, isLoading, error, malEntry
- `DetailViewModel` — reads animeId from SavedStateHandle, collects GetAnimeDetailUseCase

**settings/**
- `SettingsUiState` — current timezone, isLoggedIn, username
- `SettingsViewModel` — exposes prefs flow, handles logout, triggers MAL login

**mylist/**
- `MyListUiState` — entries grouped by `WatchStatus`, isLoading, error, searchQuery, activeFilter
- `MyListViewModel` (@HiltViewModel) — collects `GetMalUserListUseCase`, applies in-memory filter by status tab + search query, exposes `updateEntry(animeId, MalListUpdate)` and `incrementEpisode(animeId)`

**search/**
- `SearchUiState` — query, results: `List<AnimeSearchResult>`, isLoading, error, noResults
- `SearchViewModel` (@HiltViewModel) — debounces query (300 ms) → calls `SearchAnimeUseCase` → MAL anime search API; on result tap opens Detail screen; shows "Dodaj u listu" quick-action if not already in list

**mal/** (auth)
- `AuthViewModel` — stores codeVerifier in SavedStateHandle (survives rotation), handles OAuth launch and callback

### Screens — `presentation/screens/`

**`ScheduleScreen.kt`**
- `PrimaryScrollableTabRow` with 3 tabs
- Each tab: `PullToRefreshBox` wrapping `LazyColumn` of `AiringEpisodeCard`
- `SnackbarHost` for errors

**`AnimeDetailScreen.kt`**
- `LargeTopAppBar` (collapsing) with banner image via Coil
- Genres as `SuggestionChip` row
- Synopsis, airing countdown, MAL list status row, related anime section

**`MyListScreen.kt`**
- `PrimaryScrollableTabRow` — tabs: Gledam / Završeno / Planiram / Pauzirao / Dropovao (maps to `WatchStatus`)
- `SearchBar` (M3) pinned at top — filters current tab in real-time by title
- `LazyColumn` of `MyListEntryCard`
- Pull-to-refresh syncs fresh MAL list
- Empty state per tab with descriptive Serbian text
- Login prompt card if not logged in

**`SearchScreen.kt`**
- `SearchBar` (M3, always expanded) at top — autofocuses keyboard on enter
- Debounced MAL anime search (300 ms after last keystroke)
- `LazyColumn` of `SearchResultCard` — cover, title, year, type, MAL score, user's current status badge if already in list
- Each result: tap → Detail screen; long-press or swipe → quick "Dodaj / Promeni status" bottom sheet
- Empty query state: show recent searches (saved to DataStore, max 10)
- No-results state with Serbian text

**`SettingsScreen.kt`**
- `LazyColumn` of setting rows
- Timezone picker (`AlertDialog` with scrollable list of `ZoneId` options)
- MAL login/logout row with account name when logged in

### UI Components — `presentation/components/`

**`MyListEntryCard.kt`** — Material 3 `ElevatedCard`:
- Cover thumbnail, title, episode progress ("Ep 12/24"), score stars if set
- Status badge chip (color-coded per WatchStatus)
- Right: `+1` `FilledTonalIconButton` — visible only in Gledam tab
- Long-press or trailing `...` → `DropdownMenu`: Promeni status / Uredi epizode / Uredi ocenu / Ukloni iz liste

**`SearchResultCard.kt`** — Material 3 `Card`:
- Cover thumbnail, title (romaji + native small), year, type, MAL score
- Status badge if anime already in user's list
- Trailing `IconButton` (Add/Edit) opens `ListStatusBottomSheet`

**`ListStatusBottomSheet.kt`** — `ModalBottomSheet`:
- Status picker: radio buttons for all `WatchStatus` values with Serbian labels
- Episodes watched: `OutlinedTextField` with numeric keyboard
- Score: horizontal 1–10 star row or `Slider`
- Confirm / Cancel buttons; calls `UpdateMalListEntryUseCase`

**`AiringEpisodeCard.kt`** — Material 3 `ElevatedCard`:
- Left color accent strip using `media.coverImage.color` from AniList (that's a hex color string)
- Cover thumbnail 60×85dp rounded with Coil `AsyncImage`
- Title (titleMedium), episode label, genre chips, countdown
- Right: `+1` `FilledTonalIconButton` — visible only when MAL logged in + anime in WATCHING list

**`LoadingShimmer.kt`** — 3 placeholder cards using `InfiniteTransition` brush gradient

**`CountdownText.kt`** — "Za 2h 15min" or "Emitovano"; recomputes every minute via `LaunchedEffect` + `delay(60_000)`

**`ErrorBanner.kt`** — M3 `Card` with warning icon, Serbian error text, retry button

**`GenreChip.kt`** — `SuggestionChip` for single genre

### Theme

- Keep dynamic color (`dynamicColor = true`) for Material You — uses wallpaper colors on API 31+
- Static fallback: current purple palette in `Color.kt` is fine
- Add edge-to-edge support with `enableEdgeToEdge()` in MainActivity
- Splash screen: solid background color, app icon

---

## Phase 7 — WorkManager Background Sync

**`data/work/ScheduleSyncWorker.kt`** — `@HiltWorker`, `CoroutineWorker`:
- Calls `RefreshScheduleUseCase`
- Returns `Result.retry()` up to 3 attempts, then `Result.failure()`
- Periodic: every 6 hours, requires CONNECTED network, exponential backoff 30s

**`data/work/WorkManagerScheduler.kt`** — `enqueueUniquePeriodicWork` with `KEEP` policy

Register in `AnimeScheduleApplication.onCreate()`.

---

## Implementation Order

1. Gradle setup (unblocks everything)
2. `AnimeScheduleApplication` + Hilt bootstrap
3. Core layer (Result, TimeUtils, dispatchers)
4. Room entities + DAOs + Database
5. DataStore + SecureTokenStore
6. Apollo schema download + AniList remote source + mappers
7. `ScheduleRepositoryImpl` (AniList only, Jikan fallback later)
8. Domain models + schedule use cases
9. Navigation skeleton + ScheduleScreen skeleton
10. `ScheduleViewModel` + `AiringEpisodeCard`
11. MAL: Retrofit services + `MalAuthManager` + PKCE + deep link handling
12. `AuthRepository` + `AuthViewModel` + Settings screen MAL login/logout
13. `MalRepositoryImpl` + `IncrementEpisodeUseCase` + `+1` button wired
14. `AnimeDetailScreen` + `DetailViewModel`
15. `MyListScreen` + `MyListViewModel` + `MyListEntryCard` + `ListStatusBottomSheet`
16. `SearchScreen` + `SearchViewModel` + `SearchResultCard` + MAL search API endpoint + `SearchRepositoryImpl` + `RecentSearchesDataStore`
17. Jikan fallback
18. `ScheduleSyncWorker` + WorkManager
19. Error states + shimmer loading
20. Polish: animations, splash screen, edge-to-edge refinements

---

## HTTP Client Choice: Retrofit + OkHttp

Apollo Kotlin (for AniList) already uses OkHttp internally. Sharing one `OkHttpClient` across Apollo and Retrofit means one place for auth interceptors, rate-limit handling, and logging. Ktor would add a second HTTP engine for no benefit in this stack.

---

## Key Design Decisions

- **UTC everywhere in storage:** `airingAtEpochSeconds` is always UTC. `TimeUtils.todayRangeUtc(ZoneId)` returns the UTC window for "today" in the user's zone — passed directly to Room DAO queries.
- **Cache-first:** Room DAO returns `Flow` — on any `@Upsert`, all collectors automatically receive fresh data. No manual re-emission needed.
- **Token refresh:** On 401 from MAL, `MalRepositoryImpl` calls `MalAuthManager.refreshAccessToken()` and retries once. On second 401, emits `AppError.Unauthorized` and clears login state.
- **PKCE:** `codeVerifier` stored in `SavedStateHandle` across the OAuth launch so it survives process death before the redirect returns.
- **No secrets in Git:** `MAL_CLIENT_ID` and `MAL_REDIRECT_URI` read from `local.properties` (gitignored) via `BuildConfig`.

---

## Verification

After each phase:
```powershell
.\gradlew.bat assembleDebug   # must succeed
.\gradlew.bat test            # unit tests must pass
```

End-to-end smoke test:
1. Launch app → Schedule screen shows loading shimmer → AniList data loads
2. Pull to refresh works
3. Settings → MAL login → Chrome Custom Tab opens MAL auth page
4. Complete login → redirect back → logged-in state shown in Settings
5. `+1` button appears on WATCHING anime cards and updates episode count
6. Tap anime card → Detail screen opens with banner image
7. Timezone change in Settings → schedule regroups correctly
8. Kill app, reopen → cached data shows immediately before network refresh
9. My List tab → lista se prikazuje grupisana po statusu; pretraga unutar taba filtrira u realnom vremenu
10. Promeni status u My List → bottom sheet → sačuvaj → kartica odmah odražava novi status
11. Search tab → ukucaj naziv anime → rezultati se pojavljuju posle ~300 ms; tap na rezultat → Detail ekran
12. Search → long-press rezultat → bottom sheet → postavi status → badge se odmah prikazuje na kartici
