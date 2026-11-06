"""Composites rendered island layers exactly like the app does (depth-sorted, placed by crop box / tree anchor).

    python3 tools/art/compose_preview.py <layers dir> <out.png> base_s2,tower_s2,b_lab_2 [--minutes 600]
--minutes adds the grove the app would show for that many completed focus minutes.
"""
import json, sys
from PIL import Image
d, out, names = sys.argv[1], sys.argv[2], sys.argv[3].split(",")
minutes = int(sys.argv[sys.argv.index("--minutes") + 1]) if "--minutes" in sys.argv else None
m = json.load(open(f"{d}/layers.json")); res = m["res"]
items = [(m["layers"][n]["depth"], "layer", n) for n in names]
if minutes is not None and "trees" in m:
    T = m["trees"]; stages = ["seed", "sprout", "young", "mature", "ancient"]; need = [0, 25, 75, 150, 300]
    for k in range(min(len(T["slots"]), minutes // 150 + 1)):
        mm = minutes - k * 150; st = max(i for i in range(5) if mm >= need[i])
        items.append((T["slots"][k]["depth"], "tree", (stages[st], T["slots"][k]["px"])))
canvas = Image.new("RGBA", (res, res), (6, 17, 30, 255))
for depth, kind, v in sorted(items, key=lambda x: -x[0]):
    if kind == "layer":
        box = m["layers"][v]["box"]; canvas.alpha_composite(Image.open(f"{d}/{v}.png").convert("RGBA"), (box[0], box[1]))
    else:
        st, (px, py) = v; a = m["trees"]["stages"][st]["anchor"]
        canvas.alpha_composite(Image.open(f"{d}/tree_{st}.png").convert("RGBA"), (int(px - a[0]), int(py - a[1])))
canvas.convert("RGB").save(out)
