# Architecture

## Module map

```
nexus/
├── app/                 Android application: Compose UI, navigation, Android system glue
│   ├── focus/           FocusSystem (alarm + notification sync), receivers (alarm/boot/actions)
│   ├── platform/        AndroidClock, AudioController, PlatformStatus (permissions, foreground)
│   └── ui/              theme (design tokens), components (panels, icons, characters), screens
├── core/domain/         Pure Kotlin (JVM). Focus timer engine, rewards, progression, streaks,
│                        civilization stages, repository interfaces. No Android imports.
├── core/data/           Room database, DAOs, repository implementations, read models
├── world/scene/         Pure Kotlin (JVM). WorldHost contract, procedural scene generation,
│                        camera, math, render constants, scene exporter (dev tool)
├── world/gles/          Android OpenGL ES 3.0 WorldHost implementation + GLSL shaders (assets)
└── tools/
    ├── render-harness/  Headless WebGL2 verification of shaders + geometry (Playwright)
    └── audio/           Procedural SFX generator
```

Dependency direction (arrows point at what a module depends on):

```
app ──► core:data ──► core:domain
 │
 └────► world:gles ──► world:scene
```

`core:domain` and `world:scene` are plain JVM modules. They hold the most important logic (the timer, rewards, scene generation) and unit-test in seconds with no emulator.

## Separation of concerns (as required)

| Concern | Where |
|---|---|
| Presentation | `app/ui/**` (Compose), `NexusViewModel` |
| Game engine integration | `world/scene/WorldContract.kt` (`WorldHost`, `WorldState`, `GraphicsQuality`); implementation in `world/gles` |
| Focus timer | `core/domain/focus/FocusTimerEngine.kt` (pure state machine), `FocusCoordinator.kt` (use cases), `app/focus/FocusSystem.kt` (Android side effects) |
| Game progression | `core/domain/reward`, `progression`, `civilization` |
| Player profile | `core/data` `PlayerRepository` → `PlayerSnapshot`, `ProfileStats` |
| Local persistence | `core/data` (Room) |
| Audio | `app/platform/AudioController.kt` |
| Notifications | `app/focus/FocusNotifier.kt` |
| Analytics (personal productivity statistics) | `daily_stat` table + `ProfileStats`. No third-party analytics. |
| Settings | `setting` table, `SettingsRepository`, `AppSettings` |

## The focus engine: data flow

```
             ┌───────────── UI (250 ms recompute, display only)
             │
 user / OS ──┼── NotificationAction ─┐
 events      │   Alarm (due/pause) ──┼──► FocusSystem ──► FocusCoordinator ──► FocusTimerEngine (pure)
             │   Boot / app update ──┘        │  (mutex)          │
             │   App start / resume           │                   ├─► SessionRepository (Room)
             │                                │                   └─► ProgressionRepository.settle()  ◄─ one transaction
             │                                ▼                                                         (state guard +
             └──────────────────────── sync alarm + ongoing notification                                 ledger PK)
```

Key properties:
- **No component owns timer state in memory.** Every entry point reloads the open session from the database and evaluates it against the `Clock`, so process death loses nothing.
- **`reconcile()` is idempotent**, so it's safe to call from many places at once. Tests run 10–20 concurrent reconciles and assert exactly one reward.
- **Settlement is a single Room transaction.** It covers the state transition, the ledger row, the wallet and XP, planet development and daily statistics.

### Session state machine

```
           start
             │
             ▼        pause (≤3, total ≤15 min)
         RUNNING ───────────────────────────► PAUSED
           │  ▲ ◄────────── resume ─────────── │
           │  └─────────────────────────────── │
  planned  │                                   │ pause allowance exhausted
  reached  │  end early                        │ (evaluated lazily, alarm-woken)
           ▼      │                            ▼
       COMPLETED  └──────────► ABANDONED ◄─────┘
     (full reward)          (partial reward if ≥10 min focused)
```

## Engine boundary

```kotlin
interface WorldHost {
    fun setWorldState(state: WorldState)     // development 0..1, stage 0..4
    fun setQuality(quality: GraphicsQuality) // LOW / MEDIUM / HIGH
    fun playEnergyPulse(amount: Int)         // cosmetic only
    fun onHostResume(); fun onHostPause()
}
```

The world is a pure function of `WorldState`. It never writes game state. `AppContainer.worldHostFactory` chooses the implementation: `NexusWorldView` in production and `FakeWorldView` in tests, with a Godot host possible later.

## Rendering pipeline (world/gles)

1. **Sky:** fullscreen triangle with procedural stars, a two-tone nebula and sun glare.
2. **Opaque lit:** planet (displaced icosphere, glossy oceans, city-light mask revealed by development, energy-pulse wave), structures, orbiting stations.
3. **Additive:** energy-network lines (flowing), atmosphere rim, GPU-animated particles, glow sprites (fake bloom).

Frame pacing comes from Choreographer with a `maxFps` cap. The surface renders at `renderScale` of native resolution. Geometry rebuilds on a background thread only when `WorldState` changes, and GL resources are recreated in `onSurfaceCreated` after context loss.

## Database schema (v2)

| Table | Purpose |
|---|---|
| `player` | Single row: username, avatar, energy balance, lifetime energy, total XP, credits (v2), onboarding flag, account type (`guest`) |
| `focus_session` | Every session with full timestamp anchors, pause data, interruptions, end reason and anomaly flags |
| `reward_ledger` | One row per settled session (PK = session id); the audit trail and the duplicate-reward guard |
| `daily_stat` | Per-day focus, completed and ended-early counts, energy, XP, rewarded minutes (feeds the daily limiter) |
| `planet_progress` | Per-planet unlock flag and development energy (M1: Nexus Prime) |
| `setting` | Key-value user settings |

The schema JSON is exported to `core/data/schemas/`. v1→v2 is a Room `AutoMigration` that adds `credits` (default 0) to `player` and `reward_ledger`. `MigrationTest` checks that a populated v1 database upgrades without losing progress.

## Backend readiness

All storage goes through the interfaces in `core/domain/repository` plus the read-model repositories. Future additions:
- **Auth:** `player.accountType` already exists.
- **Cloud sync:** the ledger is append-only and keyed by session UUID, so syncing it is conflict-free.
- **Leaderboards, friends, seasons:** new repositories behind interfaces. The local simulation must be labelled as simulation (see `GAME_DESIGN.md`).

No backend code exists in M1, by design.
