# 5.12.9 — consistent motion and previous-week browsing

Version code **46**, branch **main**, October 9, 2026.

- Shared motion uses an 80 ms press, 180 ms release, 160 ms quick feedback,
  240 ms content changes and 300 ms navigation/panel entry. Layout changes and selection
  use bounded easing instead of independently settling springs. Graphics-layer press feedback
  and layout-time offsets remain in use.
- Every AnimatedContent explicitly controls its size animation; reduced motion disables
  the fade and size together. Expanding search headers, editor sections and recent activity
  use the same policy. Onboarding previews and Next/Back paging follow it too.
- AppSheet owns its dialog, sliding surface and dim scrim. Entry takes 300 ms and Back/scrim
  exit takes 240 ms, with both disabled under reduced motion. The framework window does not
  add another fade or dim. Panels keep their rounded top corners, AMOLED canvas, keyboard
  and system-bar insets, scroll constraints, maximum width and active-panel toast ownership.
  Panels stay in place while their content scrolls; explicit Back closes them.
  Scrim opacity is read during drawing, avoiding panel recomposition on each fade frame.
- Nested Back navigation is explicit for detail tools, notification tools, agenda reminders,
  day-list editors and watch-source editing. A child page returns to its parent within the same
  dialog; only the final Back runs the exit. Repeated dismissal taps are consumed once.
- System animation-disable changes are observed live with a disposed ContentObserver;
  the app's reduced-motion preference remains additive. No global animation settings are
  written by production code.
- Choice rows, discovery chips, palette outlines, settings/menu presses and segmented-control
  tint have consistent feedback. Selection retains opaque palette surfaces, preventing dark
  intermediate fills. Search gets a restrained focus outline, and switches retain their themed
  thumb while using a 48 dp touch target and bounded travel.
- Home's date card includes Previous/Next week arrows and a Today action. It supports the seven
  previous local dates as well as today's forward week. Counts, broadcasts, filters, detail links
  and the weekly overview follow the selected period. Date-rail changes use shared motion.
- Historical refreshes use an explicit start date and the existing DST-aware seven-day bounds.
  They start only on demand, share in-flight requests, reuse successful ranges during the session,
  show a loading state or Retry when necessary and preserve cached past broadcasts offline.
  Historical fetches do not trigger system-notification checks or add startup network requests.
- Schedule refresh errors have one persistent, aligned Retry banner instead of repeating the same
  message in a toast. An unavailable date is not labelled as having no broadcasts; known empty
  past/future dates use a date-neutral title. Disabled icon controls visibly fade using shared motion.

## Validation

- `testDebugUnitTest`, debug APK/test APK builds and `lintDebug` passed: **235 unit tests**,
  no failures/errors/skips; **0 lint errors**, 96 warnings.
- **122 distinct UI tests passed** on Pixel_10_Pro, Android API 36. The broad 71-test run
  passed 70 tests; its remaining test still referenced Material's old scrim description.
  It now uses the app's localized panel-dismiss label and passed in the final **50/50** run.
- The earlier core run hit an Espresso window-focus timeout while closing a shortcut panel.
  That test passed in isolation and in the final full core group without changing production
  behavior or bypassing Android Back. The final run includes all 20 core tests plus 29 settings,
  theme, personal-tool and visual-review tests and the corrected detail-tools regression.
- Dedicated motion tests cover rapid content changes, zero-duration size changes, live system
  reduced-motion updates, repeated panel reopen and complete dialog removal after its exit.
  System animation settings are restored by the test's `finally` block.
- The final **26/26** agenda/motion run also covers loading historical broadcasts, selecting a
  past date, opening that week's overview, returning to Today, reusing the loaded range and
  retrying an offline failure. It verifies one error banner and no premature empty-day message.
  Three new unit tests check lazy loading, in-flight reuse, offline retry and cached history.
- Fresh emulator screenshots were inspected for schedule layout, detail actions, weekly overview,
  redesigned library, notifications, source editing with the keyboard, rounded settings pickers,
  theme palettes, display controls, onboarding and larger Serbian text. Fresh history screenshots
  verify the date rail and overview in light mode and the single offline banner in dark mode.

- `assembleRelease`, `bundleRelease` and `lintRelease` passed: **0 lint errors / 96 warnings**.
- Installed the final signed, non-debuggable **5.12.9 (46)** release on Pixel_10_Pro, API 36.
  Native checks loaded real broadcasts for October 2–8, selected October 5, opened the matching
  overview (**108 broadcasts**) and returned to October 9 using both Today and the next arrow.
  Reopening history retained the loaded data. The new rail, historical cards and overview were
  visually reviewed in Light/Ocean; the dark offline state was reviewed in the UI fixture.
- Native review of the shared panel motion also covered seasonal content, notifications,
  watch-source editing and returning to a second source editor, search focus, library,
  settings, palettes and display controls in Light/Ocean and AMOLED dark. A dismissal recording
  confirmed that the panel and scrim exit together, without a lingering blur or dim layer.
- Default size/density remain **1280×2856 / 480**, with no overrides, and font scale is **1.0**.
  System animations retain their effective default scale of **1.0**. Serbian Latin, Light/Ocean,
  AMOLED and compact layout were preserved; contrast, reduced motion and scheduled theme stay off.
  The MAL account remains signed out and notification permission remains denied. The test companion
  was removed and the final release is running. No fatal exception was found in its current process.
- Captures, test logs and public validation reports are in the ignored `captures/ui-5.12.9/`
  directory. This pass does not establish a measured startup/FPS improvement or exercise live MAL sync.

## Release artifact

Signed AAB copied to Desktop as **AnimeSchedule-5.12.9-46-release.aab**. The existing
5.12.8 Desktop bundle was preserved. No remote push or Play Store upload was performed.

- Size: **7,830,822 bytes**.
- SHA-256: `2c0026756473c36b3b5aacc8e7d5fc33949ffe7936b37164e13facdca8ff90b2`.
- Bundletool validation, ZIP integrity and original AAB/APK signer checks passed.
  Manifest confirms package `com.owlcoder.animeschedule`, min SDK 31, target SDK 36,
  version 5.12.9/code 46 and a non-debuggable release. Language resources stay together;
  the bundle contains the R8 mapping.
- Release APK ZIP alignment and all eight native ELF load alignments meet 16 KB requirements.
  The 64-bit RELRO checks found no unrelated writable bytes on protected pages. The emulator
  uses 4 KB pages, so this does **not** claim a 16 KB runtime test.
