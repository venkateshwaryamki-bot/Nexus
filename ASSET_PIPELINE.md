# Asset Pipeline

## Current assets (M1): what's final and what's placeholder

| Asset | Source | Status | Replacement requirement |
|---|---|---|---|
| Nexus Prime floating island, 5 stages (home) | Scripted Blender scene, Cycles pre-render (`tools/art/nexus_island.py`) → `res/drawable-nodpi/island_stage0-4.webp` (829×954, 312 KB total) | Production-candidate (original, stylised low-poly) | An artist pass can replace the WebPs 1:1 (same size and framing) |
| Nexus Prime live 3D (optional setting) | Procedural (`world/scene/PlanetScene.kt`) | Placeholder | Kept as an option per TD-010 |
| Shaders | `world/gles/src/main/assets/shaders/*.vert|frag` | Production-candidate | Extend for normal maps and texture atlases once authored assets arrive |
| Character portraits (AIRA, VYON, ORION, KAEL, NEXA) | Vector, drawn in code | **Placeholder** | Rigged 3D models + 2D key art matching the identity rules |
| Explorer insignias (6) | Vector, drawn in code | Final-candidate | Optional premium variants |
| Planet orbs (galaxy previews) | Vector, drawn in code | **Placeholder** | Rendered planet thumbnails per world |
| Focus Chamber environments (4) | Scripted Blender scenes (`tools/art/focus_chamber.py`): portal with procedural nebula, pillars, lanterns, pedestal, themed props → `chamber_*.webp` (1080×1920, ~100 KB each) + `chamber_thumb_*.webp` | Production-candidate | Layout contract: portal centre ≈ 36% of height, pedestal ≈ 70% |
| Energy core | Blender crystal with orbit rings (`focus_chamber.py --core`) → `energy_core.webp` | Production-candidate | |
| Ambient focus soundscape | Synthesised, seamless 24 s loop (`generate_sfx.py`) → `ambient_focus.ogg` (134 KB) | Placeholder | Composed ambient bed, −20 LUFS, seamless loop |
| UI glyphs | Vector, drawn in code | Final-candidate | |
| Launcher icon | Vector drawable (adaptive + monochrome) | Final-candidate | Designer pass |
| Fonts | Space Grotesk, Inter (OFL 1.1, Inter subset to Latin) | Final | |
| SFX (intro, completion, energy) | Synthesised (`tools/audio/generate_sfx.py`) | **Placeholder** | Composed sound design. Intro ≤ 7 s, mono 44.1 kHz OGG, −14 LUFS |

Placeholder geometry isn't final production art. This table is the authoritative list.

## Planned 3D pipeline (M2+)

```
Blender (source .blend in /art, LFS) ──► glTF 2.0 (.glb, Draco optional) ──► app/src/main/assets/worlds/<planet>/
                                           │
                                           └► validation script: triangle budget, texture size, naming
```

Per-character requirements:
- Model, textures and materials.
- Animations: **idle, movement, interaction, celebration**.
- One skeleton per character, ≤ 40 bones, ≤ 12k triangles.

Per-planet requirements:
- Environment model and terrain, lighting setup, atmosphere parameters.
- Buildings, with every upgrade level as its own mesh or variant.
- Decorative elements and particle definitions.

### Budgets (mid-range target)

| Item | LOW | MEDIUM | HIGH |
|---|---|---|---|
| Scene triangles | 25k | 50k | 90k |
| Texture max size | 512² | 1024² | 2048² |
| Texture format | ETC2 / ASTC 6×6 | ASTC 6×6 | ASTC 4×4 |
| Particles | 160 | 420 | 900 |
| Render scale | 0.6 | 0.8 | 1.0 |
| FPS cap | 30 | 60 | 60 |

### Licensing rules

- Only original assets or assets under CC0, CC-BY, OFL or MIT/Apache, recorded in `licenses/` with source URLs.
- No marketplace assets that clash with the NEXUS visual language.
- No assets resembling existing game IP.

## Regenerating generated assets

```bash
python3 tools/audio/generate_sfx.py
```

```bash
./gradlew :world:scene:run --args="$PWD/tools/render-harness/scenes HIGH"
```

```bash
node tools/render-harness/render.mjs
```

## 2.5D pre-rendered art (UI v2)

The illustrations are rendered offline from Blender scripts, so the art is reproducible, reviewable in code, and costs nothing at runtime beyond decoding a WebP.

```bash
pip install bpy numpy pillow
```

Use Python 3.13, ideally in a virtual environment.

```bash
BPY_PYTHON=/path/to/venv/bin/python tools/art/build_assets.sh
```

- Each stage renders at 1080² with Cycles (64 samples + OpenImageDenoise), using a transparent film and the AgX view transform.
- Bloom comes from a second, emission-only render (all non-emissive surfaces turned matte black), blurred at two radii and added back. Only real light sources glow.
- All stages share one crop box, so the island never shifts when the stage changes.
- Placement is deterministic and fixed across stages: the city grows rather than reshuffles.
