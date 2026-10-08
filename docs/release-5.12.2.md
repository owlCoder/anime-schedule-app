# 5.12.2 — emulator UI polish

Version code: **39**. Work remains on **main**.

## Changes

- Android Back from a watch-source editor now returns to its visible source list. Previously,
  Material hid the modal before the callback switched its content, leaving a hidden window
  and its backdrop owner alive. The next Back closes the list normally, and reopening works.
- Source fields have leading icons, low surface backgrounds, 48 dp minimum height and
  Next/Done keyboard actions. Save, Cancel and editor Back clear focus and hide the keyboard.
- Compose configuration and resources share the selected app locale. Home dates, weekday
  rails, agenda and other date labels follow the same language as strings. System language
  resolves from system resources. Native Activity context and launcher owners are retained.
- Appearance uses the shared AppSwitch and its dark/AMOLED thumb colors. A row owns one
  accessible switch; its decorative control does not duplicate the toggle action.
- Segmented controls have equal option heights when labels wrap at enlarged text sizes.
- Version and English/Serbian changelog text are updated. No database migration is required.

## Environment and validation

Pixel 10 Pro AVD, Android API 36.1, 1280 × 2856, density 480, system font scale 1.0.
Enlarged Serbian text is exercised within Compose fixtures, without changing emulator scaling.
Account/editing tests use isolated local data. The connected phone is not used for this round.
Native QA captures and startup/artifact reports are retained locally in `captures/ui-5.12.2/`.

## Validation

- **228 unit tests** passed with no failures, errors or skipped cases.
- **33 distinct selected UI cases** passed across the focused run and corrective reruns:
  EmulatorPolishUiTest (5), TrackingAndThemesTest (13), Workspace5120UiTest (7),
  PolishRegressionUiTest (4), SheetBackdropUiTest (2) and AmoledSheetsTest (2).
  This is an aggregate result: the initial 33-case invocation passed 28 cases.
  Corrective runs resolved the remaining cases, including
  dismissing the IME before asserting modal Back, capturing an unmerged decorative switch,
  checking the field container's actual minimum height and updating the expected changelog title.
  The backdrop capture also passed on rerun after a transient PixelCopy no-data result.
- The initial visual baseline passed **39 cases** before these changes. Captures checked
  Home, anime editing, detail, search results, season lists, planner/weekly overview, Settings,
  Appearance, notification timing and cache retention. Large Serbian text was included.
- Native optimized-release checks cover localized Home, Search, the guest list, Appearance
  in light and AMOLED modes, source editing, Android Back to the visible source list,
  opening a second source, and a
  sharp Settings screen after closing the last modal. Original light/classic appearance is
  restored; language, AMOLED preference, notification permission and system scaling are retained.
- Debug and optimized production release/APK/AAB builds passed. Debug and release lint report
  **0 errors and 87 warnings**, retained in their reports.
- Bundletool validation, ZIP integrity, original upload-certificate verification, release mapping
  presence, APK alignment and native ELF 16 KB alignment checks passed. Three signed emulator
  APK splits generated from the production AAB passed signature/alignment checks. These are
  artifact checks, not a claim of runtime testing on a 16 KB device.

## Startup check and limits

A debug cold start reported an input-dispatch ANR while the host was building the optimized
release. The main-thread trace was runnable in Compose recomposition and CPU pressure was high;
the root cause is not proven. The full 114-case functional run was interrupted for this native
startup investigation and is **not counted as a passing full suite**. Fixture UI tests do not
exercise MainActivity startup.

After the build finished, five consecutive cold starts of the optimized production APK completed
without a new ANR or an app crash in Android's crash buffer. Android `am start -W` TotalTime values
were **4.369, 4.385, 5.111, 4.998 and 3.220 seconds**. These are emulator observations, not a
performance improvement claim or a guarantee that the debug ANR cannot recur. Last recorded ANR
remained October 8 at 19:57:54 during the debug check. Live MAL login and remote account changes
were not exercised.

## Release artifact

Production package: `com.owlcoder.animeschedule`, min SDK 31, target SDK 36, non-debuggable,
signed with the existing upload certificate. The emulator is left running the optimized
**5.12.2 / 39** production APK with default scaling. The test companion is removed.

Production AAB: `AnimeSchedule-5.12.2-39-release.aab`, **7,951,550 bytes**, saved on Desktop.
Older Desktop releases are preserved.

SHA-256: `b4c61ccda3549c2d5d2c1ebe17f0f720a1f724392a6149fdd37a1bbd318c7a43`.
