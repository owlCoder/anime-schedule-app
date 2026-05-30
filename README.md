# Anime Schedule

Native Android app for tracking anime airing schedules and managing your MyAnimeList list — ad-free.

## Features

- **Today / Tomorrow / This Week** airing schedule (AniList GraphQL)
- **Anime details** — cover, banner, synopsis, studios, countdown to next episode
- **MyAnimeList integration** — OAuth 2.0 PKCE login, list read/update, +1 episode quick action
- **Search** — MAL anime database search with list status editing
- **In-app notifications** — notification history with unread badge
- **Settings** — theme (Light / Dark / System), accent color, timezone override, notification timing, language
- **Language support** — English and Serbian (Latin), switchable without app restart
- **Onboarding** — first-run flow with language, theme, notifications and MAL login setup
- **Changelog** — in-app version history

## Screenshots

> Coming soon

## Tech stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Async | Coroutines + Flow + StateFlow |
| Networking | Retrofit + OkHttp |
| GraphQL | Apollo Kotlin (AniList) |
| Local cache | Room |
| Preferences | DataStore |
| DI | Hilt |
| Images | Coil 3 |
| Background sync | WorkManager |
| Auth | OAuth 2.0 PKCE via Chrome Custom Tabs |
| Secure storage | EncryptedSharedPreferences |

## Data sources

- **AniList GraphQL API** — primary airing schedule source
- **MyAnimeList API v2** — user list read/write, OAuth login
- **Jikan REST API** — public fallback (read-only)

## Requirements

- Android 12+ (minSdk 31)
- MAL API client registration at [myanimelist.net/apiconfig](https://myanimelist.net/apiconfig)

## Build

### Debug

```bash
./gradlew assembleDebug
```

### Release

```bash
./gradlew assembleRelease
```

Requires `local.properties` (not committed to git):

```properties
MAL_CLIENT_ID=your_mal_client_id
MAL_REDIRECT_URI=rs.owlcoder.animeschedule://oauth

# Release signing
KEYSTORE_PATH=/path/to/keystore.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

## Version

**1.2.54** (versionCode 3)

### Changelog

See [in-app changelog](app/src/main/res/values/strings.xml) or open the app → Settings → Changelog.

## License

MIT — see [LICENSE](LICENSE)
