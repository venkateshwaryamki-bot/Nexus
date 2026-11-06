"""
NEXUS art pipeline — grove trees (Seed → Sprout → Young → Mature → Ancient).

Each stage renders as a standalone sprite with the island's exact camera, at the world origin with a
small shadow-catcher disc (contact shadow). The sprite's anchor (where the origin projects) is stored,
and so is the projected position + depth of every grove slot, so the app can place any stage on any
slot by translation alone (orthographic camera ⇒ pure 2D offset).

    python tools/art/trees.py --out build/art/layers
Adds tree_<stage>.png and a "trees" section to layers.json (consumed by export_layers.py).
"""
import argparse, json, math, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import bpy
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
from artkit import hexc, mat, prim, light
from island_geo import CAM_LOC, CAM_TARGET, ORTHO_SCALE, surface, tree_slots

P = argparse.ArgumentParser()
P.add_argument("--out", default="build/art/layers")
P.add_argument("--res", type=int, default=1080)
P.add_argument("--samples", type=int, default=48)
P.add_argument("--slots-only", action="store_true", help="recompute slot projections without re-rendering sprites")
A = P.parse_args()
STAGES = ["seed", "sprout", "young", "mature", "ancient"]

def scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"; sc.cycles.device = "CPU"; sc.cycles.samples = A.samples; sc.cycles.use_denoising = True
    sc.render.film_transparent = True; sc.render.resolution_x = sc.render.resolution_y = A.res
    sc.view_settings.view_transform = "AgX"; sc.view_settings.look = "AgX - Medium High Contrast"
    sc.render.image_settings.file_format = "PNG"; sc.render.image_settings.color_mode = "RGBA"
    w = bpy.data.worlds.new("w"); sc.world = w; w.use_nodes = True
    bg = w.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.012, 0.03, 0.07, 1); bg.inputs[1].default_value = 0.9
    sun = light("SUN", (0, 0, 10), 3.4, (1.0, 0.95, 0.88), rot=(math.radians(50), math.radians(-25), math.radians(-35))); sun.data.angle = math.radians(6)
    light("AREA", (4, 5, 3), 900, (0.25, 0.85, 1.0), rot=(math.radians(-60), math.radians(30), math.radians(140)), size=6)
    light("AREA", (0, -9, -3), 380, (0.55, 0.7, 1.0), rot=(math.radians(110), 0, 0), size=8)
    d = bpy.data.cameras.new("cam"); d.type = "ORTHO"; d.ortho_scale = ORTHO_SCALE
    cam = bpy.data.objects.new("cam", d); sc.collection.objects.link(cam); sc.camera = cam
    cam.location = CAM_LOC; cam.rotation_mode = "QUATERNION"
    cam.rotation_quaternion = (Vector(CAM_TARGET) - Vector(CAM_LOC)).to_track_quat("-Z", "Y")
    return sc, cam

def tree(stage):
    rnd = random.Random(11 + stage)
    bark = mat("bark", "#6b4a33", 0.9); soil = mat("soil", "#5a4636", 0.95)
    leaf = [mat("l%d" % i, c, 0.8) for i, c in enumerate(["#2f9e5c", "#3cb46b", "#2a8a55", "#4cc47a"])]
    glow_seed = mat("seed", "#37E6A0", 0.3, 0, "#37E6A0", 6.0)
    ancient_leaf = [mat("al%d" % i, c, 0.55, 0.05, e, s) for i, (c, e, s) in enumerate([("#1f6fb8", "#20D6EA", 0.22), ("#2a9bd6", "#2FE3C8", 0.16), ("#2a4fb0", "#9275FF", 0.14)])]
    ancient_bark = mat("abark", "#4b3a5a", 0.7, 0.1)
    mote = mat("mote", "#bff9ff", 0.3, 0, "#9ff3ff", 5.0)
    if stage == 0:  # seed: soil mound + glowing seedling bud
        prim("uv_sphere", (0, 0, 0), (0.16, 0.16, 0.06), material=soil, segments=16, ring_count=8)
        prim("uv_sphere", (0, 0, 0.07), (0.035, 0.035, 0.05), material=glow_seed, segments=12, ring_count=8)
        prim("cone", (0.02, 0, 0.12), (0.015, 0.03, 0.05), (0.3, 0.4, 0), material=leaf[1], vertices=4)
        return
    if stage == 1:  # sprout: thin stem, two leaves
        prim("uv_sphere", (0, 0, 0), (0.13, 0.13, 0.04), material=soil, segments=16, ring_count=8)
        prim("cylinder", (0, 0, 0.14), (0.012, 0.012, 0.14), material=leaf[2], vertices=6)
        for sx in (-1, 1):
            o = prim("uv_sphere", (sx * 0.06, 0, 0.27), (0.07, 0.03, 0.012), (0, sx * 0.5, 0), material=leaf[1], segments=12, ring_count=6)
        prim("uv_sphere", (0, 0, 0.29), (0.025, 0.025, 0.025), material=glow_seed, segments=8, ring_count=6)
        return
    if stage == 2:  # young tree
        prim("cylinder", (0, 0, 0.2), (0.035, 0.035, 0.2), material=bark, vertices=6)
        for dx, dy, dz, s in [(0, 0, 0.5, 0.2), (0.08, 0.04, 0.42, 0.13), (-0.07, -0.03, 0.43, 0.12)]:
            prim("ico_sphere", (dx, dy, dz), (s,) * 3, material=leaf[0], subdivisions=1)
        return
    if stage == 3:  # mature tree: thicker trunk, layered canopy
        prim("cylinder", (0, 0, 0.32), (0.06, 0.06, 0.32), material=bark, vertices=7)
        for k, (dx, dy, dz, s) in enumerate([(0, 0, 0.85, 0.32), (0.18, 0.06, 0.7, 0.22), (-0.17, -0.05, 0.72, 0.22), (0.04, 0.16, 0.62, 0.2), (-0.02, -0.12, 1.02, 0.18)]):
            prim("ico_sphere", (dx, dy, dz), (s,) * 3, material=leaf[k % 4], subdivisions=1)
        return
    # ancient tree: twisted multi-trunk, wide luminous canopy, floating motes (blueprint stage 5)
    for k in range(3):
        a = k * 2.1
        o = prim("cylinder", (0.05 * math.cos(a), 0.05 * math.sin(a), 0.45), (0.06, 0.06, 0.45), (0.12 * math.sin(a), 0.12 * math.cos(a), 0), material=ancient_bark, vertices=7)
    for k in range(4):  # roots
        a = k * math.pi / 2 + 0.4
        prim("cone", (0.13 * math.cos(a), 0.13 * math.sin(a), 0.06), (0.05, 0.05, 0.12), (math.cos(a) * 1.1, math.sin(a) * -1.1, 0), material=ancient_bark, vertices=5)
    for k, (dx, dy, dz, s) in enumerate([(0, 0, 1.2, 0.42), (0.3, 0.08, 1.0, 0.3), (-0.3, -0.06, 1.02, 0.3), (0.08, 0.26, 0.92, 0.26), (-0.1, -0.24, 0.95, 0.26), (0.02, 0, 1.5, 0.24)]):
        prim("ico_sphere", (dx, dy, dz), (s,) * 3, material=ancient_leaf[k % 3], subdivisions=1)
    for i in range(14):
        a = rnd.uniform(0, 2 * math.pi); r = rnd.uniform(0.35, 0.6)
        prim("ico_sphere", (math.cos(a) * r, math.sin(a) * r, rnd.uniform(0.4, 1.5)), (0.018,) * 3, material=mote, subdivisions=1)

def render(sc, cam, name):
    import numpy as np
    from PIL import Image, ImageFilter
    catcher = prim("cylinder", (0, 0, -0.005), (0.8, 0.8, 0.005), vertices=32); catcher.is_shadow_catcher = True
    path = os.path.join(A.out, name + ".png"); sc.render.filepath = path; bpy.ops.render.render(write_still=True)
    # Emission pass for glow (the seed bud, ancient leaves and motes).
    black = bpy.data.materials.new("black"); black.use_nodes = True; bb = black.node_tree.nodes["Principled BSDF"]
    bb.inputs["Base Color"].default_value = (0, 0, 0, 1); bb.inputs["Specular IOR Level"].default_value = 0; bb.inputs["Roughness"].default_value = 1
    for o in sc.objects:
        if o.type == "MESH" and o is not catcher:
            for i, m in enumerate(o.data.materials):
                if m.node_tree.nodes["Principled BSDF"].inputs["Emission Strength"].default_value <= 0: o.data.materials[i] = black
        if o.type == "LIGHT": o.data.energy = 0
    sc.world.node_tree.nodes["Background"].inputs[1].default_value = 0; sc.cycles.samples = 12
    epath = path.replace(".png", "_e.png"); sc.render.filepath = epath; bpy.ops.render.render(write_still=True)
    im = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32) / 255
    e = np.asarray(Image.open(epath).convert("RGBA")).astype(np.float32) / 255; os.remove(epath)
    br = Image.fromarray((np.clip(e[..., :3] * e[..., 3:4], 0, 1) * 255).astype(np.uint8))
    g = (np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 120))).astype(np.float32) * 0.5 + np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 45))).astype(np.float32) * 0.35) / 255
    rgb, al = im[..., :3], im[..., 3:4]
    out_a = np.clip(np.maximum(al, g.max(axis=2, keepdims=True)), 0, 1); out_a[out_a < 10 / 255] = 0
    st = np.where(out_a > 1e-4, (rgb * al + g) / np.maximum(out_a, 1e-4), 0)
    img = Image.fromarray((np.concatenate([np.clip(st, 0, 1), out_a], axis=2) * 255).astype(np.uint8))
    box = img.getchannel("A").getbbox(); img.crop(box).save(path)
    return list(box)

def px(cam, sc, p):
    v = world_to_camera_view(sc, cam, Vector(p))
    return [v.x * A.res, (1 - v.y) * A.res]

mpath = os.path.join(A.out, "layers.json")
manifest = json.load(open(mpath))
trees = manifest.get("trees", {"stages": {}, "slots": []}) if A.slots_only else {"stages": {}, "slots": []}
trees["slots"] = []
# Per-stage display scale (early stages are tiny in reality but must stay readable on a phone).
SCALE = [1.9, 1.6, 1.3, 1.2, 1.15]
for s, name in enumerate([] if A.slots_only else STAGES):
    sc, cam = scene(); tree(s)
    for o in sc.objects:
        if o.type == "MESH":
            o.location = o.location * SCALE[s]; o.scale = o.scale * SCALE[s]
    box = render(sc, cam, f"tree_{name}")
    bpy.context.view_layer.update()
    ax, ay = px(cam, sc, (0, 0, 0))
    trees["stages"][name] = {"box": box, "anchor": [ax - box[0], ay - box[1]]}
sc, cam = scene()
bpy.context.view_layer.update()  # camera.matrix_world is stale until the depsgraph is evaluated
for (x, y) in tree_slots(10):
    p = surface(x, y)
    trees["slots"].append({"px": px(cam, sc, p), "depth": -(cam.matrix_world.inverted() @ p).z})
manifest["trees"] = trees
json.dump(manifest, open(mpath, "w"), indent=1)
print("trees done", len(trees["slots"]), "slots")
