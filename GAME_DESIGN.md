# Game Design

**YOUR FOCUS. YOUR UNIVERSE.**
Core loop: **FOCUS → EARN → BUILD → EXPLORE → COMPETE → EVOLVE**

## Story

Humanity has discovered an interconnected galaxy whose civilizations have lost their energy sources. The ancient Nexus Core has selected a new generation of Explorers. The player's real-world focus generates the energy that restores planets, raises buildings and unlocks new regions. The player isn't just using an app: they're building a universe through their own discipline.

## Characters

Visual identities are fixed. See `ART_DIRECTION.md`.

| Character | Role | Function in the game | Status |
|---|---|---|---|
| **VYON** — The Explorer | Protagonist | Introduces the universe and the five worlds; appears at milestones | M1: vector placeholder portrait |
| **AIRA** — The AI Guide | Mentor | Onboarding, explanations, session debriefs | M1: vector placeholder portrait |
| **ORION** — Operations Robot | Utility companion | Daily target, streak reminders, session status (home screen) | M1: vector portrait, live tips from real data |
| **KAEL** — The Strategist | Advanced mentor | Advanced missions, challenges, competitive progression | Portrait drawn; role starts M2/M4 |
| **NEXA** — Nexus Intelligence | Universe intelligence | Represents civilization health; voices locked sectors and completions | M1: animated vector form |

## Worlds

| # | Planet | Theme | Environment | Unlock (planned) |
|---|---|---|---|---|
| 01 | **Nexus Prime** | Headquarters | Deep navy and electric blue, Nexus Tower, energy networks, floating structures | Start (**M1, playable**) |
| 02 | **Orion** | Knowledge & innovation | Crystalline formations, floating laboratories | Level 8 + Nexus Prime at Research Colony |
| 03 | **Aether** | Clarity & balance | Floating islands, bioluminescent forests, turquoise oceans | Level 15 + a 7-day streak achieved |
| 04 | **Titan** | Discipline & ambition | Industrial megacities, reactors, orange/red light | Level 25 + 50 h total focus |
| 05 | **Eclipse** | Mastery | Purple nebulae, black-hole architecture | Level 35 + all four planets developed |

Each planet gets its own scene generator and art set. Recolouring the same scene is explicitly not allowed.

### Nexus Prime progression (M1)

Development is driven by **energy channelled** (lifetime energy earned):

| Stage | Required energy | What changes visibly |
|---|---|---|
| Abandoned Outpost | 0 | Dark ruins, a dormant tower stub, faint energy lines |
| Small Settlement | 60 | Habitats light up, the tower ignites, more buildings |
| Research Colony | 300 | Labs and reactors, orbital stations appear |
| Futuristic Metropolis | 1200 | Dense city, night-side city lights, an orbital ring |
| Planetary Civilization | 3000 | Fully restored, second ring, maximum stations |

Growth is **continuous** between stages: structure count and height scale with `development = energy / 3000`, so every session makes a visible difference. Placement is deterministic, so buildings never move.

The 3000-energy cap means ~50 hours of focus to fully restore the first world. At the default daily target (2 × 25 min) that's roughly two months. That's the intended pacing.

## Economy

### Energy (primary currency)

| Event | Energy |
|---|---|
| Completed session | **1 per planned minute** (25 min → 25, 45 → 45, 60 → 60) |
| Ended early, ≥ 10 focused minutes | ½ per focused minute (rounded down) |
| Ended early, < 10 minutes | 0 (time still recorded in statistics) |

**Healthy-play limiter:** minutes beyond **6 h/day** earn energy at 50%, and beyond **10 h/day** they earn 0 energy. XP and statistics still count. The limiter exists so that competition rewards consistency, not overwork.

All values live in `core/domain/reward/RewardPolicy.kt` and `FocusRules.kt`. The Focus Chamber shows the rules to the user verbatim.

**Anti-manipulation:**
- Rewards are granted once per session (database-guarded).
- Wall-clock changes within a boot are ignored (the monotonic clock is used).
- Paused time never counts.
- A reboot can only bridge with wall time up to the planned duration, and that session is flagged.

### Credits (secondary currency, UI v2)

- Earned **only by completing** a session: 1 credit per 5 planned minutes (25 min → 5, 60 min → 12).
- Never granted for partial sessions. They stop once the daily energy cap (10 h) is reached.
- Spent on buildings, decorations and cosmetic unlocks (Phase 5+). Energy remains the main currency that restores planets.

### XP and levels

- Completed session: 2 XP per minute + 25 completion bonus (25 min → 75 XP).
- Ended early: 1 XP per focused minute (if ≥ 10 min).
- Planned for M2: missions, building construction, planet unlocks, streak milestones.

Level curve: reaching L+1 from L costs `100 + 50·(L−1)` XP.

| Rank | Levels | Identity |
|---|---|---|
| Recruit | 1–4 | Cyan insignia |
| Explorer | 5–9 | Cyan + green |
| Commander | 10–19 | Silver |
| Pioneer | 20–29 | Gold trim |
| Galactic Master | 30–44 | Gold + purple |
| Nexus Legend | 45+ | Animated NEXA aura |

(Rank visuals and rewards land in M2.)

### Streaks (forgiving by design)

- A day counts when at least one session is **completed**.
- "Today" never breaks a streak: you have until midnight.
- Every 7 consecutive days earns a **Streak Shield** (max 2), which automatically bridges one missed day.

## Focus session rules (player-facing)

1. Choose 15 / 25 / 45 / 60 / 90 minutes, or a custom 5–180.
2. Up to **3 pauses** and **15 minutes total pause** per session. Paused time isn't focus. Exceeding the allowance ends the session (partial reward rules apply).
3. End early after 10+ minutes and you keep half the energy.
4. The timer runs on device time. Closing the app or locking the phone doesn't stop it.
5. Leaving NEXUS for another app while running is recorded as an interruption and shown in the debrief. It isn't penalised in M1. Screen-off isn't an interruption.
6. NEXUS does not block other apps (see `TECHNICAL_DECISIONS.md` TD-006).

### Focus environments

| Environment | Unlock | M1 status |
|---|---|---|
| Nexus Command Centre | Level 1 | ✅ |
| Deep Space Observatory | Level 3 | ✅ (unlockable) |
| Aether Forest | Level 6 | ✅ (unlockable) |
| Titan Research Station | Level 10 | ✅ (unlockable) |

The chamber uses lightweight 2D ambient scenes, not the GL world, to keep 90-minute sessions battery-cheap.

## Planned systems (M2–M4)

### Civilization buildings (UI v2 Phase 5, implemented)

Buildings are bought with the energy **balance** and **credits**. Lifetime energy (planet development, which drives the stage and the Nexus Tower) is never reduced by building. Each level is a separate pre-rendered model on its own island slot, so every upgrade visibly changes the island.

| Building | Unlocks at | Lv 1 | Lv 2 | Lv 3 | Level names |
|---|---|---|---|---|---|
| Energy Reactor | Abandoned Outpost | 30 E | 80 E + 5 C | 180 E + 15 C | Core → Cooled Core → Fusion Dome |
| Residential Colony | Small Settlement | 50 E | 120 E + 8 C | 250 E + 20 C | Habitat → Twin Habitats → Colony Quarter |
| Research Laboratory | Small Settlement | 60 E | 150 E + 10 C | 300 E + 25 C | Outpost Lab → Research Lab → Tech Hub |
| Communication Centre | Research Colony | 90 E + 5 C | 200 E + 15 C | 400 E + 30 C | Relay Mast → Dish Array → Signal Spire |
| Spaceport | Research Colony | 120 E + 10 C | 260 E + 20 C | 500 E + 40 C | Landing Pad → Control Tower → Starship Dock |
| Defense Tower | Futuristic Metropolis | 150 E + 10 C | 320 E + 25 C | 600 E + 45 C | Sentry → Pylon Grid → Shield Dome |
| Nexus Tower | — | grows with the civilization stage | | | |

Each construction level gives 20 × level XP. Purchases are one database transaction: the level must still match what the UI showed (a double tap builds once), funds are checked with a guarded UPDATE, and the level rises by one.

### Missions (M2)

- **Daily:** complete 1 session (20 XP), focus 60 min (40 XP), complete 2 sessions (30 XP), no abandoned sessions today (25 XP).
- **Weekly:** 10 h focus (200 XP + 50 energy), 20 sessions (150 XP), active on 5 of 7 days (120 XP).
- **Special:** first planet exploration, first building, character challenges, seasonal events.

Each mission shows objective, progress, reward, difficulty and status. Progress is derived from `daily_stat` and `focus_session`, so sessions done in M1 count retroactively where the period is still open.

### Achievements (M2)

First Contact (first completed session), Energy Pioneer (100 energy), Seven-Day Explorer (7-day streak), Master of Focus (100 h), Planet Architect (all Nexus Prime buildings at max), Galactic Commander (rank Commander), Legendary Explorer (rank Nexus Legend).

### Competition (M4)

Friends, weekly, monthly and global boards, ranked by **consistency score** (active days × capped daily focus) rather than raw hours. **Until a backend exists, any local leaderboard must be labelled "Simulated — practice board" and never presented as real players.** Seasonal events (Galactic Exploration Season, Titan Energy Challenge, Eclipse Mastery Trials) get config-driven definitions, so a backend can deliver them later.
