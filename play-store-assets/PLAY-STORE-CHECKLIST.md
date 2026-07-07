# Play Store submission checklist — AnimeSchedule

## Build

- **AAB**: `app/build/outputs/bundle/release/app-release.aab` (run `./gradlew :app:bundleRelease`)
- **applicationId**: `com.owlcoder.animeschedule`
- **versionCode / versionName**: 5 / 2.0.1 (`app/build.gradle.kts`) — bump both before every new upload
- **Signing**: `android-release-keys/animeschedule-release.jks`, alias `animeschedule`. Credentials
  in `local.properties` (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) —
  gitignored, back these up separately (password manager). **Losing this keystore means the app
  can never be updated again on Play** — same rule as every other Tapiz Android app.

## Store listing text

- `store-listing-en.txt` / `store-listing-sr.txt` — app name, short description, full description,
  ready to paste into Play Console's listing form (EN + SR-Latn locales).

## Graphics

- `animeschedule-icon-512.png` — 512×512 app icon (Play Console → Store presence → Main store listing)
- `animeschedule-feature-1024x500.png` — 1024×500 feature graphic
- `screenshot-1-schedule.png` / `screenshot-2-settings.png` / `screenshot-3-detail.png` /
  `screenshot-4-seasonal.png` — phone screenshots (captured on a 1280×2856 emulator, well above
  Play's min 320px requirement). Covers Schedule home, Settings, Detail, and the Seasonal browser.
  Add MyList/Search if you want more coverage — same `adb exec-out screencap -p` approach.
- Regenerate icon/feature graphic anytime with: `python play-store-assets/generate_assets.py`
  (requires Pillow: `pip install pillow`).

## Privacy Policy

- `PRIVACY.md` (repo root) — required because the app requests MyAnimeList account access via
  OAuth. Play Console needs a **public URL**, not a file upload — options:
  - Enable GitHub Pages on the `owlcoder/anime-schedule-app` repo and point it at this file, or
  - Use the raw GitHub URL directly: `https://raw.githubusercontent.com/owlcoder/anime-schedule-app/main/PRIVACY.md`
    (works but isn't rendered as HTML — GitHub Pages is the cleaner option).

## Content rating / Data safety form (manual, in Play Console)

- **Data safety**: declare that the app collects/accesses MyAnimeList account data (anime list,
  read + write) via OAuth, stored locally only, not shared with third parties. No ads, no
  analytics, no other personal data collected.
- **Target audience**: general audience (no account required for core schedule features).
- **Content rating questionnaire**: no user-generated content, no violence/mature content in the
  app itself (the app only displays anime metadata/cover art from AniList/Jikan — same as any
  anime database app).

## Still needed from you (can't be automated)

1. A Google Play Console **developer account** (if not already set up) under your "tapizlabs" org
   or a separate one — this app is `com.owlcoder.animeschedule`, not `rs.tapizlabs.*`, so it can
   go under either account depending on how you want it organized.
2. Enable GitHub Pages (or otherwise host `PRIVACY.md` publicly) and get the URL.
3. Upload the AAB, paste the listing text, upload graphics + screenshots, fill the Data safety
   form, submit for review.
