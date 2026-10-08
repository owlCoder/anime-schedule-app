# 5.12.5 — tab transition fix

Version code **42**, branch **main**.

Native recording of the notification panel reproduced a dark inner frame during selection:
both the incoming and outgoing tab briefly darkened before reaching their resting tones.

The shared AppSegmentedControl now animates opacity with the surface RGB held constant,
avoiding interpolation from transparent black. The dark default ripple is replaced by a
subtle press scale on the icon/text content. The hairline uses the same selection opacity,
and the transient elevation change is removed. Tab roles, selection semantics, equal sizing
and callbacks are retained. Bottom navigation is unchanged.

## Validation

- Debug and release APK/AAB builds passed. **228 unit tests passed**, with no failures,
  errors or skipped cases. Debug/release lint each reported **0 errors and 90 warnings**.
- All **7 EmulatorPolishUiTest cases passed** on Pixel_10_Pro. The two new light/dark
  regressions sample both tab backgrounds during a held press and seven intermediate
  selection frames, and verify the final selection/callback. The existing large Serbian
  text case retained equal tab heights and working selection. This is a focused suite,
  not a full instrumentation run.
- A before/after native recording used the same held press on the unread notification
  tab. The optimized release recording shows the fill and outline fading without the
  earlier dark inner frame on either tab. A normal tap back to the read tab also completed.
  Recordings, contact sheets and test screenshots are in ignored `captures/ui-5.12.5/`.
- Installed release **5.12.5 / 42** is non-debuggable. The original Serbian/light appearance
  and denied notification permission were retained; scaling remains **1280 × 2856,
  density 480, font scale 1.0**. The notification panel was left on its original read tab.
  The instrumentation companion was removed. No account/list/notification records were edited.
- The release cold launch completed normally. Android's app crash buffer was empty and the
  last ANR timestamp remained the earlier **October 8, 19:57:54** debug incident documented
  in [5.12.2 validation](release-5.12.2.md); this round does not establish its root cause.

## Release artifact

Desktop: `AnimeSchedule-5.12.5-42-release.aab`, **7,953,035 bytes**.

SHA-256: `5ea07352745969e9a10f544b91cdfd363799954f3dff7033462980b48c41bca4`

Bundletool validation, ZIP integrity, AAB/APK signatures and the original signer certificate
passed. Package/version/SDK metadata and non-debuggable status were verified. The bundle
retains language resources together and includes the R8 mapping. APK alignment and all
eight bundled native ELF load alignments passed 16 KB artifact checks, with no unrelated
writable bytes overlapping 64-bit RELRO pages. These are artifact checks, not a 16 KB-device
runtime test. The Desktop copy's hash matches the validated bundle.
