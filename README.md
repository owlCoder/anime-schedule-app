# Anime Schedule

Native Android app for tracking anime airing schedules and managing your MyAnimeList list — ad-free.

## Features

- **Today / Tomorrow / next 7 days** airing schedule, with title search, upcoming-only and own-list filters, shown in your device time zone or a time zone you pick
- **Anime details** — cover, banner, synopsis, studios, characters, relations, countdown to the next episode, and links to the watch sources you configure
- **Seasonal browser** — every anime of a season with title search, hide-tracked toggle, genre/format filters and sorting
- **List insights** — smart filters and saved views with rename/reordering; sort by newest/oldest edits, title, highest/lowest score, progress, remaining episodes or watch time
- **Episode editor** — grouped status, large progress controls, direct entry, ±10 steps, ratings, notes and safe removal; consecutive edits reuse the schedule overlay
- **Personal tracking tools** — account-scoped local favorites and notes, All-status and Unrated filters, random picks from current results, and UTF-8 CSV export through the Android document picker
- **Activity and goals** — the latest 300 local progress changes (including offline edits), 7/30/90-day comparisons, daily trends, top titles and a Watching-backlog forecast, with a configurable Monday–Sunday episode goal using your schedule timezone
- **Watch planner** — plan Watching, paused and planned titles within a time budget, include breaks between episodes, see episode ranges and share the plan
- **Themes** — 15 predefined light/dark palettes shared with onboarding; a separate Display menu for scheduled light/dark hours, compact layouts, AMOLED canvas, higher contrast and reduced animations
- **Share anime** — send the title and AniList link with the native Android share sheet
- **MyAnimeList integration** — OAuth 2.0 login, list read/update, a "+1 episode" quick action, and offline edits that are queued and delivered when you are back online
- **Search** — find anime by title (paged results, recent searches, list status editing)
- **Notifications** — optional local notifications when an anime on your *Watching* list airs, plus an in-app history with an unread badge
- **Watch sources** — your own search-URL templates (Crunchyroll and Netflix are preset), opened in the app's built-in browser with an ad/tracker filter or in another app
- **Settings** — searchable options, compact Appearance menu for theme mode and palettes, a separate Display menu, time zone override, notification timing, language, cache retention, episode duration and personal backups
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
./gradlew assembleRelease      # minified (R8) + signed release APK
./gradlew bundleRelease        # signed Play Store AAB
```

Device tests capture PNGs by default. After installing the app and test APKs, a functional rerun can
skip duplicate image capture while keeping the same assertions:

```bash
adb shell am instrument -w -r -e qaScreenshots false \
  com.owlcoder.animeschedule.test/androidx.test.runner.AndroidJUnitRunner
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

Current release: **5.12.9** (version code **46**, October 9, 2026).

The app version is defined by `versionName` / `versionCode` in [app/build.gradle.kts](app/build.gradle.kts); the
in-app changelog (Settings → Changelog) lists what changed in each release.

### Polished in 5.12.9

- Home can browse the previous seven days, return to Today and open the selected week's overview;
  historical data loads on demand and remains available from the local cache.
- Shared timing for navigation, panels, content resizing, expanding sections and control feedback.
- Reduced motion covers all panels and content sizes; system changes apply live.
- Animated picker/chip selection, clearer search focus, consistent menu feedback and 48 dp switches.
- Onboarding previews and page navigation follow the same policy as the main UI.

See the [release validation](docs/release-5.12.9.md).

### Polished in 5.12.8

- Search uses the shared theme-aware input with consistent focus/IME handling, 48 dp clear
  actions and distinct recent-search cards. The filter toolbar is shorter and shows active filtering.
- Tool menus honor compact layout; discovery chips and inset list actions keep 48 dp touch targets.
- Detail actions share their height, the episode increment has a localized accessible label,
  and Back remains available while detail data is loading.

See the [release validation](docs/release-5.12.8.md).

### Polished in 5.12.7

- Appearance and onboarding share 15 fixed palettes and compact System/Light/Dark controls.
- Settings icons follow the current palette with contrasting glyphs. Display options live in
  their own overlay; separate accents, wallpaper colors and saved appearances are removed.
- Existing accents migrate to matching palettes. Personal backups store the current appearance
  with schema 2 and can import schema 1 data without bringing back saved appearances.

See the [release validation](docs/release-5.12.7.md).

### Polished in 5.12.6

- Overlay backgrounds stay sharp under the sheet’s own scrim. Header dismissal follows the
  sheet animation; top-level navigation uses a calm fade without content zoom.
- My List has a compact primary header, one filter panel, a visible result/sort summary,
  wider titles and aligned 48 dp card actions. The sign-in view scrolls with larger text.
- In-app notifications separate title, episode and time. System notifications use a dedicated
  monochrome small icon, expanded text and a bounded cover thumbnail. Launcher/themed icons
  share the new calendar/play mark.

See the [release validation](docs/release-5.12.6.md) for this emulator review.

### Polished in 5.12.5

- Shared segmented tabs fade their fixed surface tone instead of interpolating from transparent
  black. Press feedback scales only the content; the selection hairline fades with the fill.
- Light/dark UI regressions sample tab backgrounds during pressing and selection transitions.

See the [release validation](docs/release-5.12.5.md) for this emulator review.

### Polished in 5.12.4

- Selecting a recent query and opening the list editor release search focus and hide the
  keyboard. Returning from editing keeps the normal results layout and navigation visible.
- Search retains an accessible input label, and clearing an empty result is labeled clearly.
- A UI regression case covers history selection, editing, cancellation and clearing an empty
  result without deleting saved history.

See the [release validation](docs/release-5.12.4.md) for this emulator review.

### Polished in 5.12.3

- White status icons retain contrast on bright anime banners.
- Watch-source shortcuts have a 48 dp minimum height, grow with text size, and show a play
  icon when no site logo is available. Their press feedback follows the rounded shape.

See the [release validation](docs/release-5.12.3.md) for this final emulator review.

### Polished in 5.12.2

- System Back returns from the watch-source editor to its visible list before closing the modal.
- Source fields have leading icons, matching low surface colors, accessible minimum heights and
  Next/Done keyboard actions. Leaving the editor clears focus and hides its keyboard.
- Dates and strings follow the same app language; native launchers retain the Activity context.
- Appearance shares the dark/AMOLED switch style and keeps a single accessible toggle per row.
- Wrapped segmented-control labels keep equal-height, vertically centered options.

See the [release validation](docs/release-5.12.2.md) for emulator checks and the signed AAB.

### Polished in 5.12.1

- Home has inline header actions, grouped dates, a matching search surface and three compact,
  equal-width default shortcuts. Active quick filters remain visible; other choices stay in Filters.
- Shared overlays blur the Activity backdrop and immediately release that effect when the last
  modal is disposed. Changing an editor's content keeps its backdrop; nested sheets retain ownership.
- Switch thumbs use theme colors in dark and AMOLED modes instead of a fixed white fill.
- The season browser uses readable controls, larger posters and a scrolling controls/results area.
  Search retains focus when results change; a TV icon replaces Home's stars shortcut. List scores
  use the same 0–10 display scale as the rest of the catalog.

See the [release validation](docs/release-5.12.1.md) for physical-phone checks and the signed AAB.

### New in 5.12.0

Thirty additions and improvements to library filters/saved views, session planning, activity insights,
weekly schedules and search. See the [complete release checklist](docs/release-5.12.0.md).
Smart-filter and sort sheets scroll; planning supports paused/planned titles, budgeted breaks,
episode ranges and native sharing. Activity reports compare equal-length periods and show daily trends,
top titles and a backlog estimate. Search adds newest-year sorting, an upper score limit and list-status
filtering. Unexpected search/pagination/list-refresh failures recover without losing cached content.
Library statistics no longer recalculate while typing, and pinned sorting uses a single comparator.
Settings search clears its focus before opening overlays and dismisses the keyboard.

Validation: **228 unit tests** and the full **101-case Pixel 10 Pro UI/integration suite** passed.
Debug and optimized production builds passed; lint reported 0 errors and 84 warnings. The signed
version 37 AAB passed bundletool, signing and 16 KB alignment checks. See the release checklist for
device testing status, measured timing limits and account-test coverage.

### Polished in 5.11.1

- Schedule shortcuts wrap into aligned rows with leading icons and accessible full names. The
  weekly overview no longer has a duplicate button; its fallback remains when the shortcut is removed.
- Shortcut selection shows its count, checked state and disabled choices. Shared buttons reserve space
  for their icons alongside long labels, including larger Serbian text; sign-out uses destructive styling.
- Success, error and Undo messages have their own space in the active sheet, keeping content and actions visible. One timeout follows the message between
  the sheet and page, avoiding duplicate announcements and extending neither Undo nor its dismissal.
- Sync indicators reflect the current session/error state. Unexpected retry and schedule-refresh failures
  release busy controls and retain cached content; a later background success clears a retry error.
- Search/detail loading skeletons share a draw-only shimmer, with a static reduced-motion fallback.
  Application recovery dependencies initialize lazily in background work; unused loading code is removed.

Validation: **214 unit tests** and the full **94-case device UI/integration suite** passed on Pixel 10 Pro
(API 36.1). Visual checks cover dark/AMOLED and light surfaces, overlay feedback, and Serbian text at
135% font scale. Debug and optimized release builds and lint passed (0 errors, 85 warnings).
The signed AAB passed bundletool and signature validation; its device splits were installed over the
existing app data with the original signing certificate. The release app was checked with live search
and anime details, Appearance, notification settings and Sync center. Native LOAD and APK ZIP alignment
were checked for 16 KB pages; the emulator itself uses 4 KB pages. Live MAL account sign-in was not
exercised; authentication and account/queue checks use isolated fixtures.

### New in 5.11.0

- **Sync center** in Settings and as a Schedule shortcut: pending anime count, connection state,
  last successful sync and manual retry. Rejected changes remain visible locally; reauthentication
  verifies the account before replaying its queue. Signing out with queued changes explains their removal.
- **Undo** progress, status and score edits for 10 seconds (longer when Android accessibility requests it),
  including inside the editor. Undo validates the account and latest values before restoring them.
- **System notification actions**: +1 watched episode and a 15-minute reminder. A Room transaction commits
  progress, the offline queue and its receipt together, preventing duplicate increments after repeated
  taps or restarts. Reminders respect current quiet hours, anime muting and notification preferences;
  WorkManager timing may be deferred by Android battery restrictions.
- **Custom shortcuts**: select and reorder up to four Schedule tools—Planner, Week overview, Favorites,
  History, Activity calendar and Sync center. Choices persist per account and are included in backups.

Room 8 → 10 migrates the edit queue, notifications, list and sources without resetting user data.
OAuth callback duplication and reactive token refresh were also corrected. Background flush requests
are chained so edits arriving at the end of a running flush still receive a delivery attempt.

Validation: **209 unit tests** and **90 distinct device UI/integration cases** passed on Pixel 10 Pro
(API 36.1), including the full 88-case regression suite and focused checks after the final changes.
Coverage includes Room migrations, offline recovery, Undo, notification replay across a database reopen
and account switch, and shortcut persistence/reordering. Screens were reviewed in AMOLED/dark and light
modes, with Serbian text at 135% font scale. The signed release was installed over existing app data;
connection-state changes and shortcut navigation were also checked in the release app.
Debug/release builds and lint passed (0 errors). OAuth forms use a local mock server, and account/list
flows use isolated fixture APIs and storage; live user-account sign-in was not exercised.

### Fixed in 5.10.1

AMOLED black now applies to every shared overlay canvas, including Appearance, settings pickers,
notifications and anime editing. It updates immediately in an open sheet, follows system/scheduled
dark mode and dynamic colors, and preserves card/control contrast. Light mode retains its light surfaces.

Validation: **194 unit tests** and **9 focused UI tests** passed on Pixel 10 Pro (API 36.1),
including pixel-color checks, live theme changes, rounded settings lists and consecutive anime edits.
Appearance and editor screenshots also cover Serbian text at 135% font scale. Debug/release builds
and lint passed (0 errors); the signed release retains the existing certificate and app data.

### New in 5.10.0

1. Filter loaded search results by minimum community score.
2. Filter search results to short, standard or long episode counts.
3. Pick a release year from loaded search results, alongside tracking, format and sorting.
4. Share the complete filtered week as text through Android’s share sheet.
5. Jump to the next schedule day containing broadcasts, skipping empty dates.
6. See estimated viewing time for each day and the week using personal episode-duration overrides.
7. Read the complete synopsis in a scrollable, selectable reader within the detail-tools sheet.
8. Copy a canonical anime catalog link, with correct AniList/MAL ID handling.
9. Share a loaded character’s names and public catalog link.
10. Mark an individual notification read or unread without opening its anime.

Detail and agenda actions share aligned icon/text rows, and search controls adapt to enlarged text.
The filter sheet keeps Apply visible while its contents scroll. Active year/format choices remain
editable when a new query has different metadata. Numeric filters exclude unknown metadata only
when constrained; they operate on loaded pages and trigger no extra catalog requests.
Search score display, sorting and filtering share the catalog's 0–100 scale, so low scores
are no longer mistaken for high ratings on the 0–10 display scale.

Estimated viewing time counts one episode per distinct broadcast and follows your duration settings,
including MAL-specific overrides. Weekly sharing includes nonempty days in chronological order.
Descriptions decode HTML entities and paragraph breaks once per value across the detail preview,
reader, clipboard and character sheets. Copy/share tools use canonical public links.

Notification status changes use a guarded database update. Failed changes preserve history and
show an error with the action available for retry; marking unread never resends a system alert.

Validation on Pixel 10 Pro (Android API 36.1): **194 unit tests** and **72 distinct instrumentation
cases** passed across the full regression run and 18 final targeted reruns. Two native IME/local-store
timing checks were stabilized before the final rerun. **13 of these UI cases also passed at 130% system
font scale**, covering the new tools, long menus and notification history. Screenshots were reviewed
in light and dark themes. Debug/release builds and Android lint passed (0 lint errors); the release
APK uses the existing signing certificate and was installed over the previous version without
clearing app data. Native share/picker checks use local fixtures and do not send content or edit a
live MAL account.

### New in 5.9.0

1. Show only premieres (first episodes) in the schedule.
2. Filter scheduled anime by minimum community score.
3. Filter the schedule by airing, finished or upcoming release status.
4. Select morning, afternoon, evening or night broadcasts in your schedule timezone.
5. Include optional reminders in exported ICS events: at air time or 5, 15, 30 or 60 minutes before.
6. Search loaded related anime and filter by relation type before opening a title.
7. Copy an anime synopsis as readable plain text, with HTML removed.
8. Share your list status, episode progress and personal score through Android’s share sheet.
9. Mark only unread notifications matching the current title and date filters as read.
10. Sort notification history from newest or oldest, including its date groups.

Notification controls and results share one scrollable surface. Detail tools and reminder choices
reuse the same sheet for predictable Back navigation; action icons remain aligned with text.
Score filters exclude titles without a known score when a minimum is active. Time filters follow
the schedule timezone and daylight saving transitions. Related search uses loaded anime relations,
with duplicate edges and unsupported media excluded, without another catalog request.

Calendar reminders default to none and are handled by the importing calendar app; no calendar
permissions are requested. Shared progress excludes private notes. Marking filtered notifications
captures the matching unread IDs and updates them in one database transaction, using batches for
large histories. Clearing read history remains a separately confirmed action over all read entries.

Validated on Pixel 10 Pro (Android API 36.1): 183 unit tests, 63 distinct instrumentation cases,
and 21 additional checks at 130% font scale. Debug lint and the signed, optimized release build
pass; native document export, clipboard and share-sheet flows are exercised with local QA data.


### Refined in 5.8.1

- Refresh the dashboard selection on minute boundaries while visible, so featured and upcoming broadcasts follow time without reopening the screen.
- Filter each schedule day once and reuse it for day/week views; unrelated badges and edit state do not rerun the filters.
- Preserve search results when only surrounding whitespace changes, avoiding a duplicate request.
- Give search fields a full focus surface and a persistent accessible label, and dismiss the keyboard after submission.
- Stack title/actions on narrow screens or with larger text; give compact icon controls 48 dp touch targets.
- Cache up to twenty character details in each detail screen session, with retry after failure and guards against late responses after dismissal or selection changes.
- Use the shared character loading/error states, dismiss the keyboard on tool-page changes, and explicitly show an empty muted-anime list.

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
so opening it makes no additional catalog request. Calendar exports use currently cached, filtered broadcasts and estimated durations, default to no
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
