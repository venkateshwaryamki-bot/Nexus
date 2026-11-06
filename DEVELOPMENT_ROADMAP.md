# Development Roadmap & Progress Tracker

## Milestone status

| Milestone | Status |
|---|---|
| **M0** Research & technical decision | ✅ Complete |
| **M1** First playable MVP | ✅ Complete: code-verified on JVM and in the headless GPU harness. ⚠️ Real-device verification pending. |
| **M2** Game experience | ⏳ Next |
| **M3** Expanded universe | Planned |
| **M4** Competitive features | Planned |
| **M5** Production readiness | Planned |

---

## UI/UX v2 track (blueprint-driven transformation)

Source: the NEXUS "UI/UX Design Blueprint (Implementable)" image supplied by the product owner.
Direction: **optimised 2D/2.5D environments** with lightweight animation and asset swapping (TD-010),
keeping the existing timer, rewards, persistence and the WorldHost boundary intact.

| Phase | Scope | Status |
|---|---|---|
| 1 | Design system & reusable components: blueprint tokens, Inter + Space Grotesk, button/icon-button/card/chip/pill/progress/mission/quick-action/planet-row/top-bar/bottom-nav, in-app reference screen | ✅ Done |
| 2 | Home dashboard: 2.5D Nexus Prime floating island, currency chips, planet card, quick actions, new bottom nav (Universe · Missions · Focus · Arena · Profile) | ⏳ Next |
| 3 | Focus Chamber: animated ring countdown, glowing energy core, ambient/sound controls, task picker, energy charging | Planned |
| 4 | Session completion & reward animations (crystal burst, counters, progress 35%→40%), credits currency | Planned |
| 5 | Planet development visuals: Outpost → Research Lab → Tech Hub (layered 2.5D asset sets) | Planned |
| 6 | Tree growth: Seed → Sprout → Young → Mature → Ancient (5 separate assets, scale + sway animation) | Planned |
| 7 | Missions & achievements (functional, data-driven) | Planned |
| 8 | Galaxy & unlockable planets (Orion Lv 5, Aether Lv 10, Titan Lv 15, Eclipse Lv 20) | Planned |
| 9 | Character illustrations & interactions | Planned |
| 10 | Profile, statistics, final polish, device performance pass | Planned |

Each phase ships complete (code + tests + screenshots) before the next begins.

## M0 — Research & technical decision ✅

- [x] Inspect the environment. Findings: JDK 21, Gradle 8.14, no Android SDK (installed after allowing `dl.google.com`), no KVM, Maven Central rate-limited (mirror), GitHub release downloads blocked.
- [x] Evaluate Unity vs Godot. Recommendation: Godot over Unity, and a native renderer behind `WorldHost` for M1–M2 → `TECHNICAL_DECISIONS.md` TD-001.
- [x] Architecture → `ARCHITECTURE.md`
- [x] Dependencies documented → TD-008
- [x] Device performance targets → `ASSET_PIPELINE.md` budgets, `TESTING.md` metrics
- [x] Repository structure

## M1 — First playable MVP ✅

- [x] Android application shell (single activity, Compose, edge-to-edge, portrait)
- [x] NEXUS branding: tokens, fonts, adaptive icon, glass HUD, custom glyphs
- [x] Cinematic intro (~7 s, skippable, synthesised SFX)
- [x] Registration: guest mode functional, callsign and insignia; Google/email shown as unavailable
- [x] Character introduction: AIRA, VYON, ORION (7-line conversation)
- [x] Main home screen: live 3D Nexus Prime, HUD (avatar, name, level, XP, energy), stage panel, ORION tips, dock
- [x] One playable 3D planet: Nexus Prime, 5 stages with continuous growth
- [x] Functional focus timer: presets plus custom 5–180, task, 4 environments (3 level-locked), pause/resume/end, rules
- [x] Session persistence and recovery: timestamps, process death, reboot, clock changes
- [x] Background behaviour: countdown notification, completion alarm, boot restore, notification actions
- [x] Energy reward mechanism: transactional, duplicate-proof, healthy-play limiter
- [x] Basic progression: XP, levels, ranks, streaks with shields
- [x] Local database (Room v1, schema exported)
- [x] Session debrief (rewards, level-up, stage evolution, interruptions, anomalies)
- [x] Profile with core statistics (hours, sessions, longest, streaks, completion rate, 7-day chart, planets)
- [x] Settings: audio, haptics, graphics tier, focus defaults, daily target, notification and exact-alarm status, reduce motion, privacy, replay intro, reset progress
- [x] Tests: 41 automated plus a shader/render harness
- [ ] **Real-device run of the `TESTING.md` manual plan and metrics** (blocked in the build environment; first task for anyone with a device)

## M2 — Game experience ⏳

- [ ] 3D characters (AIRA, VYON, ORION): glTF loader (or Filament gltfio), skinning, idle/interaction/celebration animations
- [ ] Galaxy map (Screen 06) with all five planets, lock state and zoom transition
- [ ] Civilization development (Screen 07): 7 buildings, construction with energy balance, upgrades that change the 3D model. DB v2 migration with a `building` table.
- [ ] Missions (Screen 08): daily, weekly and special, derived from stats
- [ ] Achievements (Screen 09) with badge art
- [ ] Rank identities and unlock rewards; XP from missions and buildings
- [ ] Full statistics screen (Screen 12): monthly, best day, weekly consistency, history list
- [ ] Data export (JSON) via the Storage Access Framework
- [ ] Unity/Godot decision gate spike scheduled for the start of M3

## M3 — Expanded universe

- [ ] Orion, Aether, Titan and Eclipse scenes (unique generators or authored assets)
- [ ] Character progression, KAEL's advanced missions
- [ ] Additional unlockable focus environments

## M4 — Competitive features

- [ ] Backend: auth, sync and leaderboards behind the existing repository interfaces
- [ ] Friends, challenges, seasonal events (config-driven)
- [ ] Local "practice board" simulation, always labelled as simulated

## M5 — Production readiness

- [ ] Device matrix, performance profiling, baseline profiles
- [ ] Accessibility audit (TalkBack labels for canvas elements, font scaling up to 200%)
- [ ] Release signing, Play Store listing, data safety form
- [ ] Backup, restore and export verification
- [ ] Battery soak tests

---

## Known issues and limitations (M1)

1. **Not yet run on a physical device.** All verification is on JVM, Robolectric and headless WebGL2. FPS, RAM, startup time and battery figures aren't measured yet. The release build is minified with R8 and hasn't been launched.
2. **Exact alarms:** on Android 14+ the exact-alarm permission is denied by default. Without it, the completion *notification* can be late in Doze. Rewards stay exact. Settings links to the permission.
3. **Reboot bridging uses the wall clock**, so a user who changes the clock *after* a reboot during a session could gain time, capped at the planned duration and flagged `recoveredAcrossReboot`.
4. **The ongoing notification is user-dismissible on Android 14+** (a platform change for non-foreground-service notifications). The session keeps running regardless.
5. **Character, environment and planet art are placeholders** (see `ASSET_PIPELINE.md`).
6. **Session debrief after process death:** if the app process was dead when a session settled in the background, the user gets the result notification but not the in-app debrief screen. The reward is still applied.
7. **Accessibility:** Canvas-drawn elements (characters, charts, timer ring) lack TalkBack descriptions; planned for M5.
8. Missions, Planets and Rankings open "sector offline" panels that state when the feature arrives. They are not fake implementations.
