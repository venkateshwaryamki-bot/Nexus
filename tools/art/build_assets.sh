#!/usr/bin/env bash
# Renders all NEXUS 2.5D art and converts it to WebP app resources.
# Usage: BPY_PYTHON=/path/to/python-with-bpy tools/art/build_assets.sh [--skip-render]
set -euo pipefail
cd "$(dirname "$0")/../.."
PY="${BPY_PYTHON:-python3}"
OUT=build/art; RES=app/src/main/res/drawable-nodpi
mkdir -p "$OUT" "$RES"
if [[ "${1:-}" != "--skip-render" ]]; then
  for st in 0 1 2 3 4; do
    "$PY" tools/art/nexus_island.py --stage "$st" --out "$OUT/island_stage$st.png" --res 1080 --samples 64 > /dev/null 2>&1
  done
fi
# One shared crop for all stages (union of visible pixels + margin), so swapping stages never shifts the island.
python3 - "$OUT" "$RES" <<'PY'
import sys
from PIL import Image
out, res = sys.argv[1], sys.argv[2]
ims = [Image.open(f"{out}/island_stage{s}.png").convert("RGBA") for s in range(5)]
boxes = [im.getchannel("A").point(lambda a: 255 if a > 10 else 0).getbbox() for im in ims]
l = min(b[0] for b in boxes); t = min(b[1] for b in boxes); r = max(b[2] for b in boxes); b = max(b[3] for b in boxes)
m = 16; box = (max(0, l - m), max(0, t - m), min(ims[0].width, r + m), min(ims[0].height, b + m))
for s, im in enumerate(ims):
    c = im.crop(box); c.save(f"{res}/island_stage{s}.webp", "WEBP", quality=86, method=6)
    print(f"island_stage{s}.webp {c.size[0]}x{c.size[1]}")
PY
du -ch "$RES"/island_stage*.webp | tail -1
