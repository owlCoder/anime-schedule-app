# Anime Schedule

Native Android app for tracking anime airing schedules and managing your MyAnimeList list — ad-free.

## Features

- **Today / Tomorrow / This Week** airing schedule (AniList GraphQL)
- **Anime details** — cover, banner, synopsis, studios, relations, countdown
- **MyAnimeList integration** — OAuth 2.0 PKCE login, list read/update, +1 episode quick action
- **Search** — MAL anime database search with list status editing
- **Notifications tab** — in-app notification history (upcoming)
- **Settings** — theme (Light / Dark / System), timezone override, notification toggle

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
| Auth | OAuth 2.0 PKCE via Chrome Custom Tabs |
| Secure storage | EncryptedSharedPreferences |

## Data sources

- **AniList GraphQL API** — primary airing schedule source
- **MyAnimeList API v2** — user list read/write, OAuth login
- **Jikan REST API** — public fallback (read-only)

## Build

```bash
./gradlew assembleDebug
```

Requires `local.properties` with:

```
MAL_CLIENT_ID=your_mal_client_id
MAL_REDIRECT_URI=rs.owlcoder.animeschedule://oauth
```

## License

MIT — see [LICENSE](LICENSE)
