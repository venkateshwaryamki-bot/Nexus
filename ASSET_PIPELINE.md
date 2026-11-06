# Asset Pipeline

## Current assets (M1): what's final and what's placeholder

| Asset | Source | Status | Replacement requirement |
|---|---|---|---|
| Nexus Prime island, layered (home, Build, debrief) | Scripted Blender scene (`tools/art/island_layers.py`): `base_s0-4` terrain, `tower_s0-4`, `b_<type>_<1-3>` buildings; each layer cropped, with its frame position and camera depth generated into `ui/art/IslandLayout.kt` (`export_layers.py`) | Production-candidate | Replace any layer 1:1 (same camera: ortho 9.4, from (0,-14,5.2) aimed at (0,0,-0.35)) |
| Nexus Prime live 3D (optional setting) | Procedural (`world/scene/PlanetScene.kt`) | Placeholder | Kept as an option per TD-010 |
| Shaders | `world/gles/src/main/assets/shaders/*.vert|frag` | Production-candidate | Extend for normal maps and texture atlases once authored assets arrive |
| Character portraits (AIRA, VYON, ORION, KAEL, NEXA) | Vector, drawn in code | **Placeholder** | Rigged 3D models + 2D key art matching the identity rules |
| Explorer insignias (6) | Vector, drawn in code | Final-candidate | Optional premium variants |
| Planet orbs (galaxy previews) | Vector, drawn in code | **Placeholder** | Rendered planet thumbnails per world |
| Focus Chamber environments (4) | Scripted Blender scenes (`tools/art/focus_chamber.py`): portal with procedural nebula, pillars, lanterns, pedestal, themed props → `chamber_*.webp` (1080×1920, ~100 KB each) + `chamber_thumb_*.webp` | Production-candidate | Layout contract: portal centre ≈ 36% of height, pedestal ≈ 70% |
| Grove trees (5 stages) | Scripted Blender sprites (`tools/art/trees.py`) with the island camera and a shadow-catcher disc. The anchor (projected ground point) and 10 slot projections + depths are exported to `IslandLayout.trees` / `treeSlots` | Production-candidate | Keep the ground point at the world origin when replacing a sprite |
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
- **Layered island.** Every layer renders with the same orthographic camera. Building and tower layers use the terrain as a Cycles *shadow catcher*, so they carry their own contact shadows and are hidden where terrain is in front of them. The app stacks the layers far to near using the generated depth values. `tools/art/compose_preview.py` does the same in Python for quick checks, including the grove (`--minutes N`).
- **Gotcha:** after creating or moving the camera, call `bpy.context.view_layer.update()` before projecting points with `world_to_camera_view` or using `matrix_world`. Otherwise the transform is stale and projections silently use a default camera.
- Placement is deterministic and fixed across stages: the city grows rather than reshuffles.
