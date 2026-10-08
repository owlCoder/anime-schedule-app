# 5.12.1 — home, overlays and theme polish

Version code: **38**. Work remains on **main**.

## Changes

- Home keeps its title and three actions in one header row, groups dates, places search ahead of
  compact shortcuts and leaves more room for anime. Inactive quick filters live in Filters;
  selected quick filters remain directly accessible.
- Three default shortcuts use equal-width columns at normal text size. Enlarged text uses two
  columns, with an equal-width third shortcut. Icons remain to the left of centered labels.
- A TV icon opens the season browser. Season controls adapt to width and font size, posters use
  a larger minimum width, and controls scroll together with results instead of trapping them
  below a fixed toolbar. Loading, errors, empty results, filtering and list layout remain available.
- Search keeps focus across loading/empty/result changes. Opening filters or changing layout
  dismisses its keyboard. Search backgrounds use the surrounding low surface tone throughout
  the app.
- Seasonal list scores retain the catalog's 0–100 value until the shared card converts it to
  the 0–10 display scale. For example, 80 now appears as **8.0**, rather than **0.8**.
- Every shared modal owns one backdrop registration. The Activity content blurs while any
  modal is open; content changes reuse that registration. Removing the last modal removes
  blur without an additional exit tween. Closing one nested modal keeps the remaining owner's blur.
- Custom switches use a dark canvas-colored active thumb and a softer inactive thumb in dark
  and AMOLED themes. Light theme behavior and toggle accessibility remain intact.
- Version and English/Serbian changelog text are updated. No database migration is required.

## Phone environment

Physical phone: **2510DPC44G**, Android API **36**, 1156 × 2510, density **480**, font scale **1.0**.
The emulator is stopped. Enlarged Serbian text is tested through Compose fixture density, without
changing device-wide scaling. Current QA theme/language settings are retained.

Tests use `com.owlcoder.animeschedule.qa`, with isolated repositories/stores for account, queue
and editing checks. The production bundle remains `com.owlcoder.animeschedule` with the original
local upload certificate. Live MAL sign-in was not exercised. The phone uses **4 KB pages**;
bundle/APK/ELF 16 KB validation is an artifact check, not a 16 KB device runtime claim.

## Validation

- **228 unit tests** passed with no failures, errors or skipped cases.
- **109 distinct UI/integration cases** passed on the physical phone across the full run and
  corrective reruns. The final full run passed 108/109 cases; its notification-search fixture
  needed to dismiss the keyboard and scroll before clicking an off-screen empty-state action.
  After that test-only adjustment, all **11 NotificationWorkspaceTest cases** passed. No
  application code changed after the full run. This is an aggregate result, not a claim that
  the last full invocation passed every case without a rerun.
- The focused home/blur/switch/discovery/editing/AMOLED suite and visual reruns checked all
  **43 selected cases**. Captures cover light Home, large Serbian shortcuts, season controls,
  list/empty results, Settings search/changelog and editing. Native Watch sources was also
  inspected in the current AMOLED theme on the phone.
- Backdrop tests verify actual rendered contrast: blur while open, retained blur when content
  changes or another modal remains, a sharp screen after dismissal, and repeated reopenings.
  Both toolbar Close and Android Back are exercised.
- Debug and optimized production/QA release builds passed. Debug and release lint reported
  **0 errors and 86 warnings**; warnings are retained in the reports.
- Production bundletool validation, ZIP integrity, original upload-certificate verification,
  release mapping-file presence, APK alignment and native ELF 16 KB alignment checks passed.
  The three signed phone APK splits generated from the production AAB passed signature/alignment
  checks. The phone is left with the optimized **QA** APK; the AAB contains the production ID.
- Optimized QA phone smoke checks cover the real Home, loaded season grid, Watch sources,
  backdrop blur and a clear Home after Android Back. No QA-process AndroidRuntime errors were
  present in that check. The test companion is removed; temporary keep-awake and the notification
  permission granted by native notification fixtures are restored to their original disabled state.

Production AAB: `AnimeSchedule-5.12.1-38-release.aab`, **7,956,822 bytes**, saved on Desktop.

SHA-256: `4287060253c496abaa7b44e95d550d46ebb0f66c5dbb3a055c1e7c8be9459e80`.
