# 5.12.10 — page and control motion polish

Version code **47**, branch **main**, October 9, 2026.

- Segmented controls draw one opaque selection indicator that slides between equal-width tabs.
  The shape is cached by layout, its position is read while drawing, and RTL is respected.
  Labels keep a stable weight and all segments retain at least 48 dp touch targets.
  Narrow/enlarged layouts stack options in equal-height rows instead of splitting words into tiny columns.
- Shared content changes fade out before the incoming text fades in, within the existing
  bounded 240 ms duration. Reduced motion removes the fades, delays and size animation together.
  Top-level and detail navigation use the same fade-through approach and shorter translation.
- Detail tools, notification tools, calendar reminders and watch-source editors change pages inside one dialog with
  restrained directional movement. Saved page state retains input and scroll position on return.
  Those panels let the page transition own resizing, avoiding two animations chasing the same height.
  Panel headings also animate when changing pages. Source forms initialize for each selected source
  and keep the outgoing fields until the exit finishes; reopening after Cancel discards unsaved edits.
- Primary-button fill and shared label/icon states animate consistently. Plain buttons use the
  shared press feedback without a second ripple. Search reserves its clear action's width and
  fades that action, avoiding a cursor/text jump on the first or last character.
- Panel toasts stay composed through their exit instead of being immediately removed. Replacing
  a message animates the content and height; outgoing actions/dismissals are guarded against
  affecting the current message. Timeout and Undo ownership remain in the existing controller.
- Keyed library, search and seasonal list/grid cards animate their placement and appearance
  during filtering and sorting using the same motion policy. Reduced motion settles immediately.
- The floating bottom bar uses a shorter translation on appearance/disappearance.

## Validation

- Debug APK/test APK builds, `testDebugUnitTest` and `lintDebug` passed: **235 unit tests**,
  no failures/errors/skips; **0 lint errors / 96 warnings**.
- Dedicated checks cover panel-toast exit, replacing an Undo message without executing its
  outgoing action, saved input/scroll when reopening a nested page and immediate reduced-motion
  page changes. Existing checks also cover repeated panel reopen, live system-motion changes,
  source-editor Back/save behavior and light/dark tab frames without a dark flash.
- Visual inspection exposed excessive word wrapping in narrow, enlarged segmented controls.
  The responsive row layout fixes it and its regression now checks equal-height readable rows.
  The nested-page test was corrected to target the editable field and scroll through the lazy
  collection's semantics, rather than selecting its outer box or an uncomposed list item.

- **84 distinct emulator UI tests passed** across the motion, broad regression and isolated rerun.
  This covers nested-page state and reduced motion, toast exit/replacement, segmented controls,
  notification/agenda flows, appearance, tracking, library, discovery and source editors.
  One broad-run shortcut/Back test hit Espresso's `RootViewWithoutFocusException`; it passed
  unchanged in isolation. The earlier test selector corrections are described above.
- Signed `assembleRelease`, `bundleRelease` and `lintRelease` passed with
  **0 release lint errors / 96 warnings**. The installed APK is release/non-debuggable;
  the test companion was removed before native review.
- Native release review on **Pixel_10_Pro / API 36** covered Home, notification history/tools,
  light and AMOLED-dark appearance, dark display controls, and opening Crunchyroll then Netflix
  editors with Back between them and on panel close. Both source forms showed their own data.
  Sampled screen recordings were inspected for page movement and resizing, in addition to stills.
- The emulator retains default **1280 × 2856 / 480 dpi / font scale 1.0**, with no size/density
  overrides and effective system animation scales 1.0. The app was left on current Home with
  Serbian Latin, Light/OCEAN, AMOLED and compact layout enabled; higher contrast, reduced motion
  and scheduled theme disabled; guest MAL and notification permission denied, as before review.
  No fatal exception was present in the current release process log.
- This review does not report measured startup/frame-rate gains or live authenticated MAL sync.
  Reduced-motion behavior is covered by emulator regression tests.

## Signed Play Store artifact

Desktop file: `/Users/danijel/Desktop/AnimeSchedule-5.12.10-47-release.aab`

- Size: **7,863,784 bytes**.
- SHA-256: `bc09e62229aa8f1d17f0cb8088d4e7c91f18b2eed8dbd04e1e5a449daf8055d6`.
- Bundletool validation, ZIP integrity, manifest version **5.12.10 / 47**,
  non-debuggable manifest and the existing signing certificate passed verification.
- Minimum SDK 31, target SDK 36; language splitting disabled and R8 mapping included.
- APK ZIP alignment and all eight packaged native ELF libraries satisfy 16 KB alignment;
  no invalid writable/RELRO overlap was found. Runtime review used the emulator's 4 KB pages,
  so it is not a 16 KB runtime test.

Ignored local evidence is stored under `captures/ui-5.12.10/`, including test summaries,
artifact/runtime validation, native screenshots and screen recordings.
