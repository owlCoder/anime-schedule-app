# 5.12.7 — unified palettes and display settings

Version code **44**, branch **main**, October 9, 2026.

- Settings icon tiles use the selected primary color and its contrasting foreground.
- Appearance contains a compact mode picker and 15 fixed palettes. Shared palette previews
  are cached by mode/palette and adapt to two columns with larger text or narrow width.
- Onboarding uses the same palette/mode controls, scrolls its personalization page and saves
  the setup in one preference transaction. The old seven-accent picker is removed.
- Display is a separate Settings row/overlay for AMOLED, contrast, motion, compact layout and
  day/night scheduling. Appearance reset preserves display options; Display reset preserves
  the chosen palette. Manual mode selection disables scheduled override atomically.
- Saved appearance UI, state, mutation APIs and legacy color APIs are removed. Preference
  migration replaces Classic accents with matching palettes, discards old saved looks and
  preserves unrelated account/notification preferences. Existing explicit palettes win.
- Personal backups export schema 2 with the current appearance and accept schema 1 through
  a migration. Old saved looks are ignored; Unicode tracking data and account isolation remain.

## Validation

- Debug APK, instrumentation APK, optimized release APK and signed AAB builds passed.
  **232 unit tests passed**, with no failures, errors or skipped cases. Debug/release lint
  each reported **0 errors and 94 warnings**.
- All **33 focused Pixel_10_Pro UI cases passed**: 4 new appearance/onboarding cases,
  2 AMOLED cases, 7 controls/navigation cases, 2 theme/display cases, 1 localized appearance
  case, 6 modern settings cases, 3 library cases, 5 editing cases, 2 sharp-backdrop cases
  and 1 settings-search/changelog case. After the final compact-spacing refinement,
  the 4 appearance cases and 7 controls/navigation cases were repeated successfully.
- Two relevant cases were also repeated with the emulator's actual system font scale
  set to **1.35**. Appearance changed to two palette columns and remained scrollable;
  the mode controls and separate Display options remained accessible. Font scale was
  restored to **1.0** after those checks. The onboarding personalization page also has
  large-text coverage and keeps its finish action outside the scrolling palette area.
- Unit coverage includes reopening a legacy DataStore through the real migration,
  preserving unrelated account/display preferences, ignoring invalid legacy accents,
  atomic onboarding saves, appearance-reset isolation and schema-1 backup import.
  Palette contrast checks cover all 15 choices in light and dark modes. UI assertions
  verify 48 dp touch targets and Settings icon colors across six palette/mode combinations.
- Native production review includes the Settings icon tiles, compact Appearance in light
  and AMOLED dark, live palette/mode changes, the separate Display overlay and Android
  Back dismissal. Captures and validation reports are in ignored `captures/ui-5.12.7/`.
  The previous Classic/Cyan choice migrated to Ocean; Serbian/light mode and all Display
  switches were retained, including enabled AMOLED and compact layout.
- Installed release **5.12.7 / 44** is non-debuggable. The notification permission remains
  denied, and the test companion was removed. Default scaling is **1280 × 2856, density 480,
  font scale 1.0**. Release launch completed with no fatal exception in its current process.
- No controlled startup/frame benchmark, live MAL-account flow or 16 KB-page device runtime
  test was run in this round. Theme construction and palette previews are remembered;
  these changes do not establish a measured percentage performance improvement.

## Release artifact

Desktop: `AnimeSchedule-5.12.7-44-release.aab`, **7,903,953 bytes**.

SHA-256: `779ef57359dbec9b20bdfdeae961a4d8faa7f49f75a2c9c1069342bf88f8ca5d`

Bundletool validation, ZIP integrity, AAB/APK signatures and the original signer certificate
passed. Package/version/SDK metadata and non-debuggable status were verified. The bundle
retains language resources together and includes the R8 mapping. APK alignment and all
eight bundled native ELF load alignments passed 16 KB artifact checks, with no unrelated
writable bytes overlapping 64-bit RELRO pages. The emulator uses 4 KB pages; these artifact
checks do not substitute for a 16 KB-device runtime test. The Desktop copy's hash matches
the validated bundle, and the previous 5.12.6 Desktop bundle was preserved unchanged.
