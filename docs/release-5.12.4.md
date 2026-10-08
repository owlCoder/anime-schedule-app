# 5.12.4 — search focus polish

Version code **41**, branch **main**.

- Selecting a recent search submits its query, releases input focus and hides the keyboard.
  Previously it explicitly refocused input after selection and kept keyboard chrome over results.
- Opening a result's list-status editor also clears search focus. Closing the editor returns
  to the normal results header and navigation, without reopening the old search keyboard.
- The input exposes the same accessible search label when empty or populated.
- The no-results action says Clear search and retains recent-search history.
- A UI regression covers these transitions with fake repositories and no remote account writes.
  English/Serbian changelog text and the expected changelog title are updated.

## Validation

- Debug APK and instrumentation APK compiled; **228 unit tests passed**, with no failures,
  errors or skipped cases.
- All **11 DiscoveryWorkspaceUiTest cases passed** on Pixel_10_Pro, including the new
  history/editor/no-results regression. This is the focused discovery suite, not the full
  instrumentation suite.
- Optimized release APK/AAB and release lint built successfully. Debug and release lint
  each reported **0 errors and 89 warnings**; warnings remain in the project.
- Native review used the installed, non-debuggable **5.12.4 / 41** APK and real Frieren
  search results. Selecting the existing history entry restored the Search title and bottom
  navigation without the IME toolbar. After deliberately refocusing the input, opening the
  list editor and pressing Android Back returned to the same unfocused results. No list
  changes were saved.
- Search filters and the Serbian changelog were also opened and visually checked. Overlay
  backgrounds retained blur; closed overlays returned to a sharp results screen. The
  changelog displayed the new version and release text correctly.
- Emulator scaling remained **1280 × 2856, density 480, font scale 1.0**. Serbian/light
  appearance and denied notification permission were retained. The release app remains
  installed and the instrumentation companion was removed.
- One release cold launch completed in **3364 ms**. This is a smoke observation, not a
  comparative performance benchmark. The app crash buffer was empty; the last ANR record
  remained the earlier **October 8, 19:57:54** debug-launch incident described in
  [5.12.2 validation](release-5.12.2.md). This round does not establish its root cause.
- Local screenshots and artifact validation are saved in the ignored
  `captures/ui-5.12.4/` directory.

## Release artifact

Desktop: `AnimeSchedule-5.12.4-41-release.aab`, **7,952,442 bytes**.

SHA-256: `8215eae8b8efb6bb798a7d5f402e8de862d0ab8367bdcfabbdd5e5439134ee49`

Bundletool validation, ZIP integrity, AAB/APK signatures and the original signer certificate
passed. Package/version/SDK metadata and non-debuggable status were verified. The bundle
retains language resources together, contains the R8 mapping and requests 16 KB native
library alignment. APK alignment and all eight bundled native ELF load alignments passed;
64-bit RELRO ranges did not overlap unrelated writable bytes. These are artifact checks,
not a 16 KB-device runtime test. The Desktop copy's hash matches the validated bundle.
