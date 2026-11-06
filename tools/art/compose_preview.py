"""Composites rendered island layers exactly like the app does (depth-sorted, placed by crop box)."""
import json, sys
from PIL import Image
d, out = sys.argv[1], sys.argv[2]
names = sys.argv[3].split(",")
m = json.load(open(f"{d}/layers.json")); res = m["res"]
canvas = Image.new("RGBA", (res, res), (6, 17, 30, 255))
for n in sorted(names, key=lambda n: -m["layers"][n]["depth"]):
    box = m["layers"][n]["box"]
    canvas.alpha_composite(Image.open(f"{d}/{n}.png").convert("RGBA"), (box[0], box[1]))
canvas.convert("RGB").save(out)
