# 5.12.3 — final detail polish

Version code **40**, branch **main**. This is a small visual follow-up to 5.12.2.

- The detail hero starts with a stronger, smoothly fading dark scrim so white status icons
  remain readable on bright banners. The title panel and lower image treatment are retained.
- Watch-source shortcuts have a 48 dp minimum height and vertical padding that grows with
  text. A missing site logo uses a play icon. Click semantics expose a button and press
  feedback is clipped to its rounded outline.
- English/Serbian release notes and the expected changelog title are updated.

## Validation

- Optimized production APK/AAB builds and **228 unit tests** passed, with no failed,
  erroneous or skipped tests. Release lint has **0 errors and 88 warnings**.
- Native review on the Pixel 10 Pro AVD compares the same bright anime banner before/after,
  checks seasonal navigation, source shortcuts and the detail-tools overlay, and confirms a
  sharp detail screen after Android Back. Both source buttons expose a **144 px / 48 dp**
  clickable target in Android accessibility bounds. No new fixture UI suite was run for this
  small visual follow-up.
- The installed optimized APK is **5.12.3 / 40**. Its cold start completed in 3.187 seconds
  (`am start -W` TotalTime), an emulator observation rather than a benchmark.
  No app crash appeared in Android's crash buffer or new ANR in this check. The earlier debug
  ANR investigation and its limits remain documented in [5.12.2](release-5.12.2.md).
- Bundletool validation, ZIP integrity, original upload-certificate verification, release
  mapping presence, APK signature/alignment and all eight native-library 16 KB alignment
  checks passed. The production package, min SDK 31 and target SDK 36 are retained.
- Device scaling stays at font 1.0 / density 480 / 1280 × 2856. Appearance, language,
  account data and notification permission are retained. Native captures and validation
  metadata are saved locally in `captures/ui-5.12.3/`.

Production AAB: `AnimeSchedule-5.12.3-40-release.aab`, **7,952,085 bytes**, saved on Desktop.
Older releases are preserved.

SHA-256: `f01396ae16912f3d20ee98fcd0c02565dfa828fcc396b5c80b7b2229739bd2f7`.
