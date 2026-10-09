# 5.12.6 — overlays, library and notifications

Version code **43**, branch **main**, October 9, 2026.

- Removed Activity backdrop blur and its ownership/animation state. Overlay windows retain
  their own dim scrim. Header dismissal waits for sheet hiding, while rejected Hidden
  transitions can still navigate nested content back to its parent.
- Root navigation/auth transitions no longer scale screen content.
- My List’s primary header contains search/statuses, with secondary filters in one panel.
  Statistics/tools remain accessible, sort/result information stays above the cards, and
  empty status views can reset to the populated library. Cards use aligned 48 dp actions,
  a wider title column, consistent margins and smaller metadata groups. Guest sign-in scrolls.
- In-app notifications separate title, episode and localized time. System alerts use a
  dedicated 24 dp monochrome glyph, expanded text, episode context and a 256 px cover request.
  Alert timestamps use the current delivery time, so old history rows can be reminded again.
  Action intent identity, immutability and replay protection are retained.
- Adaptive launcher, monochrome launcher and in-app branding share a new calendar/play mark.

## Validation

- Debug APK, instrumentation APK, optimized release APK and signed AAB builds passed.
  **228 unit tests passed**, with no failures, errors or skipped cases. Debug/release lint
  each reported **0 errors and 93 warnings**.
- All **30 focused Pixel_10_Pro instrumentation cases passed**: 2 sharp-backdrop cases,
  3 My List layout cases, 11 notification workspace cases, 5 overlay editing cases,
  5 library filter/tools cases and 4 notification action cases. This is a focused suite.
- The new library regressions exercise Serbian text at font scale 1.35, AMOLED cards,
  48 dp action targets, disabled pending increments, filter selection, an empty-status reset
  and guest sign-in/benefit scrolling. Existing cases cover tags, ratings, sorting, saved
  views and consecutive anime edits with nested Back navigation.
- A native reminder initially failed because Android rejected the fixture's historical
  alert timestamp. Using the current delivery time fixed it. The final test verifies posting,
  the dedicated small icon, episode context, expanded text and distinct immutable actions;
  other cases retain replay, snooze, mute and quiet-hour checks. These use isolated test
  databases/preferences and fake APIs, without editing a real MAL account.
- Visual review includes light and AMOLED libraries, larger text, filters, populated in-app
  notifications and the system notification shade. Production native review includes guest
  My List, Appearance, the populated day overlay, Android Back dismissal and the new adaptive
  launcher icon in Pixel All apps. The dismissal frame sequence retains a sharp Activity
  while the sheet and its own scrim exit. Captures are in ignored `captures/ui-5.12.6/`.
- Installed release **5.12.6 / 43** is non-debuggable. Original Serbian/light appearance and
  denied notification permission were retained; scaling is **1280 × 2856, density 480,
  font scale 1.0**. The test companion was removed. Release launch and launch through All apps
  completed, with no fatal exception in the current release process. This round does not
  establish the cause of the earlier debug ANR documented in [5.12.2](release-5.12.2.md).
- No controlled startup or frame-performance benchmark was run. The changes remove the
  full-screen overlay blur work and bound notification cover decoding; they do not establish
  a measured percentage speedup. A live MAL account and a 16 KB-page device were not tested.

## Release artifact

Desktop: `AnimeSchedule-5.12.6-43-release.aab`, **7,946,498 bytes**.

SHA-256: `f96761fed17fbe1d1cb240a20e14e4d07333ebb1f574e0169b61440b2345d8e6`

Bundletool validation, ZIP integrity, AAB/APK signatures and the original signer certificate
passed. Package/version/SDK metadata and non-debuggable status were verified. The bundle
retains language resources together and includes the R8 mapping. APK alignment and all
eight bundled native ELF load alignments passed 16 KB artifact checks, with no unrelated
writable bytes overlapping 64-bit RELRO pages. The emulator uses 4 KB pages; the alignment
checks do not substitute for a 16 KB-device runtime test. The Desktop copy's hash matches
the validated bundle.
