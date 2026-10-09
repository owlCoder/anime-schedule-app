# 5.12.8 — search and control polish

Version code **45**, branch **main**, October 9, 2026.

- SearchScreen uses AppSearchField instead of a second input implementation. Search fields
  share a stable 52 dp minimum, palette surfaces, wrapping-safe height, 48 dp clear actions,
  request-focus support, focus callbacks and force-clearing focus on IME Search/Done.
  A disabled input also disables its clear action.
- Search filters use a shorter contextual label, right-aligned result counts and a selected
  fill when filters are active. Narrow/enlarged layouts keep the stacked toolbar.
- Recent-search cards use a distinct grouped surface and themed history icons. Selection
  has its own minimum 56 dp area; individual removal remains a separate action.
- Shared tool action rows use smaller tiles and vertical spacing in compact layout while
  labels can still grow. Inset list actions and discovery filter chips have 48 dp minimums.
- Detail status/increment buttons share a row height; the increment announces its localized
  purpose. The loading view exposes Back before the detail request completes.

## Validation

- `testDebugUnitTest`: **232 tests**, no failures, errors or skipped tests.
- Final emulator regression: **29 UI tests passed**, covering shared search focus/submit/clear,
  disabled controls, recent-search selection/removal, discovery and library controls, list
  editing, detail tools/loading, schedule filters and enlarged-text Settings navigation.
  The initial run exposed two merged-text failures: placeholder text was being included in
  the input semantics. After removing duplicate placeholder semantics while preserving the
  accessible field label, the full 29-test run passed.
- Debug and release builds succeeded. Both lint reports have **0 errors / 95 warnings**;
  the existing warnings are still present.
- Installed the signed, non-debuggable **5.12.8 (45)** release on **Pixel_10_Pro**, API 36.
  Visually checked native recent searches, search results, active filters/reset, real detail
  content and the compact tool menu in Light/Ocean and AMOLED dark. Checked the native
  search layout at actual system font scale **1.35**, then restored **1.0**.
- Default screen size/density remain **1280×2856 / 480**, with no overrides. Serbian Latin,
  Light/Ocean, AMOLED and compact layout were restored/preserved; contrast, reduced motion
  and scheduled theme remain off. Notification permission remains denied. No live MAL
  account edits were made. The test companion was removed; the release remains running.
  No fatal exception was found for the current release process.
- Local screenshots and result logs are in the ignored `captures/ui-5.12.8/` directory.
  This pass does not establish a measured startup speedup or exercise live MAL sync.

## Release artifact

Signed AAB copied to Desktop as **AnimeSchedule-5.12.8-45-release.aab**. The existing
5.12.7 Desktop bundle was preserved. No remote push or Play Store upload was performed.

- Size: **7,902,577 bytes**.
- SHA-256: `e7323b4def41ae3a2d298e7b6b1bc4468570170f338012aaab9f1bd56a9163e4`.
- Bundletool validation, ZIP integrity and original AAB/APK signer checks passed.
  Manifest confirms package `com.owlcoder.animeschedule`, min SDK 31, target SDK 36,
  version 5.12.8/code 45 and a non-debuggable release. Language resources stay together;
  the bundle contains the R8 mapping.
- Release APK ZIP alignment and all eight native ELF load alignments meet 16 KB requirements.
  The 64-bit RELRO checks found no unrelated writable bytes on protected pages. The emulator
  uses 4 KB pages, so this does **not** claim a 16 KB runtime test.
