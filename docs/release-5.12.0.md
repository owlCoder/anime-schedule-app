# 5.12.0 — 30 additions and improvements

This release extends existing tools. Personal data stays account-scoped; planning, filters and
statistics do not submit MAL list edits. Room remains at schema 10.

1. Smart library filter: titles with personal notes.
2. Smart library filter: titles without tags.
3. Smart library filter: unfinished series longer than 26 episodes.
4. Smart library filter: unknown episode totals.
5. Smart library filter: started but unfinished titles.
6. Smart library filter: completed titles still awaiting a score.
7. Oldest-update library sorting; missing dates stay last.
8. Lowest-score library sorting; unrated titles stay last.
9. Rename saved views, with duplicate-name validation.
10. Reorder saved views, with persistence and personal-backup support.
11. Include on-hold titles in the planner.
12. Include planned titles in the planner.
13. Budget for breaks between episodes, without a trailing break.
14. Share the watch plan through Android's share sheet.
15. Show the next episode range for each planned title.
16. Activity summaries for the last 7, 30 or 90 days.
17. Episode comparison against the preceding equal-length period.
18. Seven-day activity trend, including accessible numeric totals.
19. Five most-watched titles in the selected activity period.
20. Estimated Watching-backlog finish date at the last seven days' pace.
21. Filtered broadcast counts on dashboard date buttons.
22. Weekly broadcast distribution chart in Week overview.
23. Schedule filter that hides anime with muted system alerts.
24. Local library/schedule search with independent words and accent-insensitive matching.
25. Search sorting by newest year; unknown years stay last.
26. Upper community-score limit alongside the existing lower limit.
27. Search filter for current MAL list status.
28. Recovery from unexpected search and pagination errors, keeping loaded pages.
29. Library statistics calculated only on list changes; pin priority and the main order use one sort.
30. Library refresh recovers after unexpected failures, retains cached entries and permits retry.

Smart-filter and sorting menus scroll; saved-view editing stays inline. New UI text is available in
English and Serbian Latin. Activity reports cover only the retained local history (up to 300 changes),
include corrections and exclude future dates. Forecasts require known totals and recent progress.
Planning never updates episode progress automatically.

Settings search releases focus and dismisses the keyboard before opening a sheet, so closing the
changelog does not reopen the keyboard. Period selectors and paired metric cards remain aligned at
larger font sizes; empty trend bars do not show a misleading progress marker.

## Validation

- 228 unit tests passed, with no failures or skipped cases.
- The full 101-case UI/integration suite passed on Pixel 10 Pro (API 36.1), including overlays,
  AMOLED/light themes, notification actions, offline/account fixtures and large Serbian text.
- Debug and optimized production release builds passed; Android lint reported 0 errors and 84 warnings.
- The version 5.12.0 / code 37 production AAB passed bundletool validation, ZIP integrity, release
  certificate verification, mapping-file presence and 16 KB native/APK alignment checks.
- The emulator uses 4 KB pages; these checks do not claim runtime testing on a 16 KB device.
- Live MAL account authentication was not exercised; account and queue checks use isolated fixtures.

The Settings-search focus regression and physical-device checks were completed in the
[5.12.1 follow-up](release-5.12.1.md), including dark/light overlays and enlarged Serbian text.
At the initial phone check, installed version 5.11.1 used a different signing certificate from the
local upload key. Phone testing therefore uses an isolated `com.owlcoder.animeschedule.qa` package.
The production AAB retains `com.owlcoder.animeschedule`; no production-app uninstall was performed
by the test workflow. Android later reported that the original package was no longer installed.

Cold first-frame timings on the shared emulator host varied widely. Same-session samples were
3179/3281/3133 ms for 5.11.1 and 4318/2324/3312 ms for 5.12.0; no startup improvement percentage is
claimed. Reduced statistics recomputation and one-pass sorting are verified implementation changes.

Production AAB SHA-256:
`a6f718b742ef90f71d9eaa640d390b774ef9a4636cd63b21611bce31c7aad313`.
