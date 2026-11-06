# Art Direction

NEXUS must read as a **premium futuristic 3D game**, never as a dashboard, a habit tracker or a generic Pomodoro app. It should be futuristic but readable: light is used with intent and the screen isn't flooded with neon.

## Colour tokens

Defined once in `ui/theme/Theme.kt` (`NexusColors`) and `world/scene/Palette.kt`:

| Token | Hex | Use |
|---|---|---|
| Deep Space Navy | `#061321` | Primary background, window background |
| Midnight Blue | `#0B2035` | Secondary background, panels |
| Electric Cyan | `#27DFFF` | Primary accent: energy, the tower, interactive highlights |
| Neon Green | `#43F58A` | Energy gained, success, the pulse wave |
| Cosmic Gold | `#F5C85B` | Premium, rules, level-ups, warnings that aren't errors |
| Purple | `#9275FF` | NEXA, mystery, nebulae, locked sectors |
| Text | White / `#A9BCCF` / `#6B8399` | Primary / secondary / muted |

Rules:
- Cyan is the default accent, and green means *you earned something*.
- Gold is reserved for premium moments.
- Purple belongs to NEXA and the unknown.

## Typography

- **Rajdhani** (SemiBold/Bold) for game-oriented headings, labels and HUD text, always uppercase with tracking.
- **Space Grotesk** for body text, numbers and the timer.
- Minimum body size is 12.5 sp. Labels are 13 sp with wide tracking, for readability on mid-range 1080p panels.

## UI language

- **Glass HUD panels** (`GlassPanel`): translucent gradient, hairline cyan border fading downward, and **corner brackets** as the signature motif.
- The primary button is an energised cyan→green gradient with a slow moving sheen and press scale.
- Secondary buttons are outlined and tinted.
- Icons are **custom line glyphs drawn in code** (`NexusIcon`): a minimal stroke set with no stock icon pack.
- Motion is slow and purposeful. Reduce motion freezes it.

## Characters: identity rules (apply on every screen)

| Character | Silhouette | Palette | Signature |
|---|---|---|---|
| VYON | Helmeted explorer, angular shoulder armour | Black and deep navy, subtle metal trim | **Cyan chest reactor** + cyan visor |
| AIRA | Elegant holographic bust, flowing hair, halo ring | White, silver, cyan, translucent | Scanlines, core gem |
| ORION | Small floating rounded robot | White and metallic blue | **Two glowing cyan eyes**, antenna, hover glow |
| KAEL | Commander bust, crest | Dark metal, silver | Blue energy accents |
| NEXA | Abstract energy form, orbiting rings | Cyan, violet, white particles | Bright central core |

M1 portraits are **vector placeholders** (`ui/components/Characters.kt`). They lock in silhouette and colour so the 3D models can match them later.

## Planet identities

| Planet | Base / glow | Architecture language |
|---|---|---|
| Nexus Prime | `#0E3A66` / cyan | Octagonal tower with stacked energy rings, hex habitats, block labs, spires, ring reactors |
| Orion | `#1B4F8C` / ice blue | Crystal shards, floating labs |
| Aether | `#0D5C5A` / turquoise | Floating islands, bioluminescent flora |
| Titan | `#6A2A12` / orange | Megastructures, reactors, smokestacks |
| Eclipse | `#2A1450` / purple | Monoliths, accretion rings |

## 3D look (M1 native renderer)

- Stylised PBR-ish shading: wrap lighting for a soft terminator, Blinn-Phong specular (glossy oceans, satin metal), and Fresnel rim tinted toward cyan.
- Exposure tonemapping (`1 − e^(−x)`) keeps emissives from clipping into neon soup.
- City lights are point clusters revealed by development, strongest on the night side.
- The atmosphere is an additive rim shell, and its strength grows with development.
- Glow sprites stand in for bloom on the tower beacon, reactors and lab tips.
- The energy pulse is a green wave from the tower across the surface, plus a particle boost.

Reference renders from the verification harness are in `docs/media/scene_stage*.jpg`.
