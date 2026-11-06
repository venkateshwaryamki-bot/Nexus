#!/usr/bin/env bash
# Renders all NEXUS 2.5D art and converts it to app resources.
# Usage: BPY_PYTHON=/path/to/python-with-bpy tools/art/build_assets.sh [--skip-render]
set -euo pipefail
cd "$(dirname "$0")/../.."
PY="${BPY_PYTHON:-python3}"
OUT=build/art; RES=app/src/main/res/drawable-nodpi
mkdir -p "$OUT" "$RES"
if [[ "${1:-}" != "--skip-render" ]]; then
  # Layered Nexus Prime island: terrain + tower per stage, every building at every level.
  "$PY" tools/art/island_layers.py --out "$OUT/layers" --res 1080 --samples 48 > /dev/null 2>&1
  # Focus Chamber environments and the energy core.
  for env in command_centre observatory aether_forest titan_station; do
    "$PY" tools/art/focus_chamber.py --env "$env" --out "$OUT/chamber_$env.png" --res 1080 --samples 64 > /dev/null 2>&1
  done
  "$PY" tools/art/focus_chamber.py --core --out "$OUT/energy_core.png" --res 512 --samples 64 > /dev/null 2>&1
fi
python3 tools/art/export_layers.py "$OUT/layers"
python3 - "$OUT" "$RES" <<'PY'
import sys
from PIL import Image
out, res = sys.argv[1], sys.argv[2]
for env in ["command_centre", "observatory", "aether_forest", "titan_station"]:
    im = Image.open(f"{out}/chamber_{env}.png").convert("RGB")
    im.save(f"{res}/chamber_{env}.webp", "WEBP", quality=82, method=6)
    im.resize((im.width // 4, im.height // 4), Image.LANCZOS).save(f"{res}/chamber_thumb_{env}.webp", "WEBP", quality=80, method=6)
Image.open(f"{out}/energy_core.png").save(f"{res}/energy_core.webp", "WEBP", quality=88, method=6)
PY
du -ch "$RES"/*.webp | tail -1
