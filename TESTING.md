# Testing

## Automated tests (all run on the JVM, no device needed)

```bash
./gradlew :core:domain:test :world:scene:test :core:data:testDebugUnitTest :app:testDebugUnitTest
```

| Suite | Tests | Covers |
|---|---|---|
| `core:domain` `FocusTimerEngineTest` | 13 | Timestamp-derived timing, exact completion boundary, back-dated completion after process death, pause accounting, pause count limit, pause allowance termination time, **wall-clock manipulation ignored**, **reboot bridging** (running and paused), negative-time clamp, interruption tracking, closed-state transitions rejected |
| `core:domain` `RewardAndProgressionTest` | 9 | Energy = minutes for 15/25/45/60/90, partial reward threshold and rate, daily healthy-play limiter, XP level curve, ranks, civilization stages (incl. clamping), forgiving streaks with shields |
| `core:domain` `FocusCoordinatorTest` | 9 | **20 concurrent reconciles → exactly one reward**, idempotent settle, single active session, stale session settled before a new start, end-early partial, end-early after time is up completes instead, persisted pause/resume, pause-limit settlement, daily limiter across sessions |
| `world:scene` `PlanetSceneTest` | 6 | Deterministic generation, monotonic visible growth across all 5 stages, orbital unlock, quality budgets (< 100k triangles), matrix inverse, no NaN/Inf in vertex data |
| `core:data` `MigrationTest` (Robolectric + Room MigrationTestHelper) | 1 | A populated v1 database migrates v1 → v2 → v3 with progress intact |
| `core:data` `BuildingTest` | 4 | Building spends only the balance and adds XP; insufficient funds and locked stages are rejected without spending; 8 concurrent taps build once; upgrades stop at the max level |
| `core:domain` `ConstructionTest` | 5 | Unlock stages, exact missing amounts, max level, rising costs, XP per level |
| `core:data` `SettlementTest` (Robolectric + real SQLite) | 4 | 10 concurrent reconciles → one ledger row; direct duplicate settle rejected by the DB; **open session survives a new handle (process death)**; daily stats, longest session and streak aggregation |
| `app` `FirstSessionJourneyTest` (Robolectric + Compose) | 1 (long) | Full first-run journey through the real app: intro → guest registration → AIRA/VYON → home (outpost) → 60-min session → pause/resume (paused time excluded) → completion → **+60 energy / +145 XP, one ledger row** → home shows Small Settlement and the world receives stage 1 plus an energy pulse → profile. Writes 12 screenshots to `app/build/screenshots/`. |

| `app` `DesignSystemTest` (Robolectric + Compose) | 1 | Renders every token and component, checks that bottom-nav selection moves on tap, and saves `ds_reference.png`. |

**Latest result (UI v2 Phase 5): 53/53 passing.** The journey test now also covers the illustrated home, the chamber controls (pause/resume via icon buttons, energy-core charge), the completion burst, the reward screen (+60 / +145 / +12, evolution), the credits chip, and building the first Energy Reactor (balance 60 → 30, island shows the structure, +20 XP).

### Shader and render verification (headless GPU path)

```bash
./gradlew :world:scene:run --args="$PWD/tools/render-harness/scenes HIGH"
```

```bash
node tools/render-harness/render.mjs
```

This compiles and links all 6 GLSL ES 3.00 programs from `world/gles/src/main/assets/shaders` under WebGL2 (ANGLE/SwiftShader in headless Chromium). It then renders the exported geometry for all 5 stages plus an energy-pulse frame, and fails on any compile, link or GL error. Output goes to `tools/render-harness/out/`. **M1: 6/6 renders, glError = 0.**

Limit: WebGL2 is a strict subset, so it catches language and compile errors. Driver-specific behaviour on Adreno or Mali still needs real devices.

## Manual device test plan (required before calling M1 "device-verified")

> **Not yet executed.** No physical device or emulator (no KVM) was available in the M1 build environment. Every row below is still pending.

Reference devices: one Snapdragon 6/7-series (e.g. Galaxy A54/A55), one MediaTek Dimensity 7000-class, one low-end Helio G-series (LOW tier), plus a Pixel on the latest Android.

| # | Scenario | Steps | Expected |
|---|---|---|---|
| 1 | Timer accuracy | 25-min session, compare against a stopwatch | ±1 s at completion |
| 2 | Background | Start, press Home, wait for completion | Notification at completion (exact alarm granted) or within minutes (not granted); +25 energy |
| 3 | App killed | Start, swipe the app away from recents (and `adb shell am kill`), reopen mid-session | Remaining time correct; ongoing notification still counting |
| 4 | Force-stop | `adb shell am force-stop com.nexus.focus` mid-session, reopen after completion time | Session settled on open, debrief shown, reward once |
| 5 | Device restart | Start 45 min, reboot after 10 min, wait | Alarm restored by `BootReceiver`; completion credited; debrief notes reboot recovery |
| 6 | Pause/resume | Pause from the notification action, resume in-app | Paused time excluded; 3-pause limit; auto-end after 15 min total pause |
| 7 | Duplicate rewards | Complete while the app is open and the alarm fires at the same moment | One ledger row (`adb shell run-as com.nexus.focus sqlite3 databases/nexus.db "select count(*) from reward_ledger"`) |
| 8 | Clock change | Mid-session, move the system time +2 h | No speed-up; debrief notes the clock change |
| 9 | Offline | Airplane mode for the whole flow | Fully functional (the app has no INTERNET permission) |
| 10 | Low memory | `adb shell am send-trim-memory com.nexus.focus RUNNING_CRITICAL`, background and foreground | GL context restored, scene rebuilt |
| 11 | Orientation / resize | Portrait-locked; test split-screen and a foldable | Layout intact; GL viewport correct |
| 12 | Resolutions | 720p, 1080p, 1440p, 20:9 and 16:9 | HUD readable; planet framed |
| 13 | Notifications denied | Deny POST_NOTIFICATIONS | Sessions still work and settle when the app opens |
| 14 | Doze | `adb shell dumpsys deviceidle force-idle` during a session | Completion processed at the next maintenance window or on open |
| 15 | Quality tiers | LOW / MEDIUM / HIGH on each device | Meets the FPS targets |

### Metrics to record per device

| Metric | How | M1 target |
|---|---|---|
| FPS (home) | `adb shell dumpsys gfxinfo com.nexus.focus framestats`, GPU profiling bars | ≥ 30 LOW, ≥ 55 MEDIUM on reference mid-range |
| RAM | `adb shell dumpsys meminfo com.nexus.focus` (PSS) | < 120 MB on home |
| Cold start | `adb shell am start -W -n com.nexus.focus/com.nexus.app.MainActivity` | < 800 ms to first frame |
| APK size | Build output | **1.98 MB release (measured)** |
| Battery | `adb shell dumpsys batterystats`, 60-min session with screen off | Near-zero app CPU (no service, no wakelocks) |
| Crash rate | Logcat / Play Vitals | 0 in the test pass |

## Known limitations

See the "Known issues" section of `DEVELOPMENT_ROADMAP.md`.
