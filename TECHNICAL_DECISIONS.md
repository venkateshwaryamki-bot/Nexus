# Technical Decisions

Decision records for NEXUS. Each one states the context, the options considered, the decision, and what would make us revisit it.

---

## TD-001 — 3D technology: Unity vs Godot (vs a native renderer)

### Context

NEXUS is a **productivity app first** and a game second (see the product vision). The constraints that matter most:

- The focus timer, persistence, notifications, alarms and boot recovery must be rock-solid Android code. None of it may depend on a game loop.
- The 3D content is a **living diorama**: one planet with structures that grow, energy effects, particles and an orbit camera. There's no physics, no combat and no level streaming. Characters (Milestone 2+) need skinned animation.
- Mid-range Android: 30 FPS minimum, small install, low battery use. Users keep the app open daily, and sessions last 15–90 minutes.
- Local-first, with a backend later. The app must remain usable when the 3D layer runs at low quality.
- Rich Android UI: forms, settings, charts, accessibility, text input, notifications.

### Evaluation

| Criterion | Unity 6 (URP) + Unity as a Library | Godot 4.x (Android library / `godot-lib` AAR) | Native Kotlin + OpenGL ES 3.0 renderer |
|---|---|---|---|
| Android performance | Good, mature mobile pipeline. The engine loop is heavier at idle. | Good for simple scenes. The Vulkan Mobile/Compatibility renderers are less battle-tested on low-end GPUs. | Best for this scope: only what the scene needs, rendered on demand at a capped frame rate. |
| 3D rendering quality ceiling | Highest: URP, Shader Graph, VFX, baked lighting, post-processing. | High: PBR, glow, SDFGI (desktop), decent mobile post-processing. | Limited to what we write. Stylised PBR, rim/atmosphere and fake bloom are fine, but advanced post-processing and shadows cost engineering time. |
| Memory | ~150–250 MB baseline with an empty scene embedded. | ~80–150 MB baseline. | ~30–60 MB for app plus scene (geometry ≈ 3 MB). |
| APK size | +25–45 MB for the runtime (IL2CPP, URP). | +20–30 MB for the runtime. | **≈ 2 MB total** (measured: 1.98 MB minified release, M1). |
| Development complexity | Two projects (Unity + Android), UaaL lifecycle quirks, single-instance limits, slow iteration on the Android side. | Two projects. Embedding via `GodotFragment`/v2 plugins works but is newer, and the docs are thinner. | One project, one language, one build. The renderer is ours to maintain. |
| Asset ecosystem | Largest (Asset Store), though licence vetting is required. | Smaller, growing, mostly CC0/MIT. | Blender → glTF works, but needs our own loader (Milestone 2) or Filament's gltfio. |
| UI integration | Poor: game UI and Android UI live in separate worlds, and overlaying Compose on Unity is awkward. | Better (Android views over the Godot view), but there are still two UI stacks. | Best: Compose HUD directly over the GL view, sharing state and accessibility. |
| Android plugin support | Mature, but JNI bridges on both sides for alarms, notifications and Room. | v2 plugin system, workable. | Not needed: everything is native Android. |
| Long-term maintainability / licensing | Proprietary, with pricing and licence history risk. Requires the editor licence. | MIT, no royalties, open source. | No vendor risk. Bus factor: renderer knowledge lives in this repo. |
| Usable in this development environment | **No**: editor licence and GUI required, and binaries can't be downloaded here. | **No**: GitHub release downloads are blocked by the sandbox network policy, so there's no editor and no export templates. | **Yes**: builds and tests in CI-like headless environments, and shaders verify in headless Chromium. |

### Decision

**Between the two requested engines, Godot 4 is the better fit for NEXUS.** It's MIT-licensed, has a smaller runtime, embeds as an Android library under a native shell, and needs no licence for CI. Unity's higher rendering ceiling doesn't justify its runtime cost, licensing exposure and UI-integration friction for a diorama-scale scene inside a productivity app.

**For Milestones 1–2, NEXUS ships a native OpenGL ES 3.0 renderer behind a `WorldHost` interface**, and Godot remains the planned upgrade path. The reasons, specific to this project:

1. The productivity engine must be native regardless. Once it is, the 3D layer is only a view, and a 2 MB native view beats a 25–45 MB embedded engine on every metric we're asked to optimise (install size, startup, memory, battery).
2. Neither engine's tooling can run in the current development environment. A native renderer is the only option we can *build and verify* here instead of claiming.
3. The interface boundary (`world/scene/WorldContract.kt`) makes this reversible. The app sends `WorldState`, quality and pulse events, and nothing else, so a `GodotWorldHost` can replace `NexusWorldView` without touching the app, domain or data modules.

### Prototype evidence (Milestone 1)

- The full Nexus Prime scene (20k-triangle planet, up to 6k structure triangles, ~1000 energy-line segments, 900 GPU-animated particles, 50 glow sprites) builds in ~13 ms on the JVM (warm). See `./gradlew :world:scene:run`.
- All 6 shader programs compile and link under WebGL2/ANGLE with no GL errors, and render all 5 civilization stages (`docs/media/scene_stage*.jpg`).
- Minified release APK: **1.98 MB**.

### Revisit when (decision gate at the start of Milestone 3)

Run a one-week Godot spike on a real mid-range device (for example Snapdragon 6-series or Dimensity 7000-class, 6 GB RAM) with one character model and one planet. Switch to Godot if any of these holds:

- Art direction needs skinned characters, post-processing (true bloom, volumetrics) or shadows beyond what a native renderer can deliver within ~2 engineer-weeks per feature.
- Four or more distinct planet environments become more expensive to author in code than in an editor.
- A Godot build stays within +30 MB APK, < 150 MB RSS and ≥ 30 FPS on the reference device.

---

## TD-002 — App architecture: native Kotlin + Jetpack Compose

- **Kotlin + Compose** for all UI (HUD, screens, dialogs). It gives a modern, accessible, testable UI, and the HUD composes directly over the GL view.
- **Single activity**, with lightweight enum-based routing (`MainActivity.kt`). Navigation-compose was avoided because the M1 graph is flat. Revisit at M2 if deep links or nested graphs are needed.
- **Manual DI** (`AppContainer`). Hilt or Koin would add build time, APK size and indirection for about a dozen objects.

## TD-003 — Persistence: Room (SQLite)

- Room offers compile-time-checked SQL, transactions (`withTransaction`), Flow-based observation, and migration tooling with exported schemas (`core/data/schemas`).
- **Reward integrity is guaranteed in the database, not just in code:**
  1. Settlement runs in one transaction that first checks the session is still open.
  2. The `reward_ledger` primary key is the session id, so a duplicate insert aborts and rolls back the whole transaction.
- Settings live in a key-value table rather than DataStore, so there's one storage engine and one backup and reset path.

## TD-004 — Timer reliability: timestamps, not ticking

- Session state is persisted as anchors on **both** the monotonic clock (`elapsedRealtime`, plus `BOOT_COUNT`) and the wall clock.
- Within a boot, the monotonic clock is authoritative, so wall-clock changes are detected, flagged and ignored.
- Across a reboot, the wall clock bridges the gap (clamped to ≥ 0 and ≤ the planned duration), and the session is flagged `recoveredAcrossReboot`.
- Completion timestamps are back-dated to the moment the planned duration was reached, so sessions reconciled hours later land on the correct calendar day.

## TD-005 — No foreground service

A focus timer doesn't need a running process.
- An **ongoing notification with a system countdown chronometer** shows live time, rendered by SystemUI.
- An **ELAPSED_REALTIME_WAKEUP alarm** wakes us at completion or pause-expiry to settle and notify.
- `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` re-derive alarms from the database.

This avoids Android 14+ foreground-service type restrictions (no type fits "focus timer" without `specialUse` justification), reduces battery use, and removes a whole class of crashes. The trade-off: without the user-granted exact-alarm permission, the *completion notification* may arrive a few minutes late in Doze. The **rewards and timing are unaffected** because they're derived from timestamps.

## TD-006 — Distraction "blocking": honest tracking instead

Real app blocking on Android needs an AccessibilityService, Device Admin/Owner, or UsageStats with an overlay. All of these carry heavy permissions and Play policy restrictions, and are often perceived as invasive. M1 **does not block apps and does not claim to**. It records an *interruption* when the user leaves NEXUS for another app during a running session (screen-off is excluded via `PowerManager.isInteractive`) and reports it in the debrief. Possible future work is an opt-in UsageStats-based "Focus Shield", documented as such.

## TD-007 — Fonts, audio and art: original or properly licensed

- **Fonts:** Space Grotesk and Rajdhani, both under the SIL OFL 1.1 (`licenses/`). Rajdhani is subset to Latin with fontTools (735 KB → 79 KB).
- **Audio:** all SFX are synthesised by `tools/audio/generate_sfx.py`, with no third-party samples.
- **Art:** procedural and vector placeholder art, labelled as such. See `ASSET_PIPELINE.md`.

## TD-008 — Dependency versions

| Dependency | Version | Note |
|---|---|---|
| AGP | 8.13.2 | AGP 9.x is current, but its built-in Kotlin and DSL changes aren't worth the migration in M1. |
| Kotlin / KSP | 2.3.21 / 2.3.13 | |
| Compose BOM | 2026.06.01 | Newer BOMs (2026.08+) require compileSdk 37 and AGP 9.1. They're pinned to the newest set that supports API 36 on AGP 8.13. |
| core-ktx / activity / lifecycle | 1.18.0 / 1.12.4 / 2.10.0 | Same reason. |
| Room | 2.8.5 | |
| Robolectric | 4.17 | JVM tests for Room and the full UI journey. |

`compileSdk`/`targetSdk` = 36, `minSdk` = 26 (Android 8.0, ~97% of active devices, required for adaptive icons and `BOOT_COUNT`).

**Avoided dependencies:** Hilt, Navigation, DataStore, Material Icons Extended (~ MBs; icons are drawn in code), any analytics or crash SDK (no network permission in M1), and any 3D engine runtime.

## TD-009 — Development-environment notes (not project configuration)

The cloud sandbox used for M1 rate-limits Maven Central. A **machine-local** Gradle init script (`~/.gradle/init.d/`) points to Google's official Maven Central mirror, and the `robolectricRepoUrl` Gradle property points Robolectric's runtime downloads at the same mirror. Neither is committed project configuration, and normal machines need neither.
