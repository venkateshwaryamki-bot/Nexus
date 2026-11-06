"""
NEXUS art pipeline — layered Nexus Prime island (2.5D compositing).

Renders, with ONE fixed orthographic camera, transparent layers that the app stacks back-to-front:
  base_s{0..4}   terrain + crystals per civilization stage (grass vitality changes)
  tower_s{0..4}  the Nexus Tower per stage
  b_{type}_{1..3} each player-built structure at each level, standing in its own slot
Building/tower layers use the terrain as a Cycles *shadow catcher*, so they carry their own contact
shadows on the grass. Each layer is cropped to its visible pixels; its crop box (in full-frame pixels)
and camera depth are written to a generated Kotlin layout file used by the app.

    python tools/art/island_layers.py --out build/art/layers [--res 1080] [--samples 48] [--only base_s2,b_reactor_1]
"""
import argparse, json, math, os, random, sys
sys.path.insert(0, os.path.dirname(__file__))
import bpy, bmesh
from mathutils import Vector, noise
from artkit import hexc, mat, prim, light

P = argparse.ArgumentParser()
P.add_argument("--out", default="build/art/layers")
P.add_argument("--res", type=int, default=1080)
P.add_argument("--samples", type=int, default=48)
P.add_argument("--only", default="")
A = P.parse_args()
os.makedirs(A.out, exist_ok=True)
ONLY = set(filter(None, A.only.split(",")))

# Building slots (x, y on the island top). Front arc + one back-left; never directly behind the tower.
SLOTS = {
    "reactor":   (-2.45, -0.3),
    "colony":    (-1.45, -1.8),
    "lab":       (0.15, -2.3),
    "comms":     (1.65, -1.6),
    "spaceport": (2.45, -0.05),
    "defense":   (-1.85, 1.25),
}
TYPES = list(SLOTS)
R = 3.2
CYAN, GREEN = "#20D6EA", "#37E6A0"

def rim_radius(th):
    return R * (1 + 0.10 * math.sin(3 * th + 0.7) + 0.06 * math.sin(5 * th + 2.1) + 0.04 * math.sin(9 * th))

def ground_z(x, y):
    r = math.hypot(x, y)
    return 0.18 * noise.noise(Vector((x * 0.45, y * 0.45, 1.3))) * min(1, r / 1.2) + 0.08 * (1 - min(1, r / R))

def surface(x, y):
    return Vector((x, y, ground_z(x, y)))

# --------------------------------------------------------------------------------------------- scene
def new_scene(stage):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"; sc.cycles.device = "CPU"; sc.cycles.samples = A.samples; sc.cycles.use_denoising = True
    sc.render.film_transparent = True
    sc.render.resolution_x = sc.render.resolution_y = A.res
    sc.view_settings.view_transform = "AgX"; sc.view_settings.look = "AgX - Medium High Contrast"
    sc.render.image_settings.file_format = "PNG"; sc.render.image_settings.color_mode = "RGBA"
    w = bpy.data.worlds.new("w"); sc.world = w; w.use_nodes = True
    bg = w.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.012, 0.03, 0.07, 1); bg.inputs[1].default_value = 0.9
    sun = light("SUN", (0, 0, 10), 3.4, (1.0, 0.95, 0.88), rot=(math.radians(50), math.radians(-25), math.radians(-35))); sun.data.angle = math.radians(6)
    light("AREA", (4, 5, 3), 900, (0.25, 0.85, 1.0), rot=(math.radians(-60), math.radians(30), math.radians(140)), size=6)
    light("AREA", (0, -9, -3), 380, (0.55, 0.7, 1.0), rot=(math.radians(110), 0, 0), size=8)
    light("AREA", (-6, -4, -4), 250, (0.55, 0.45, 1.0), rot=(math.radians(120), 0, math.radians(-50)), size=6)
    d = bpy.data.cameras.new("cam"); d.type = "ORTHO"; d.ortho_scale = 9.4
    cam = bpy.data.objects.new("cam", d); sc.collection.objects.link(cam); sc.camera = cam
    cam.location = (0, -14, 5.2); cam.rotation_mode = "QUATERNION"
    cam.rotation_quaternion = (Vector((0, 0, -0.35)) - cam.location).to_track_quat("-Z", "Y")
    return sc, cam

def materials(stage):
    alive = stage > 0
    grass_hex = ["#5b6157", "#3f8f5a", "#3fa863", "#40b56c", "#44c27a"][stage]
    M = {
        "grass_edge": mat("grass_edge", ["#4a4f47", "#2f7347", "#2f8a50", "#2f9658", "#31a060"][stage], 0.9),
        "soil": mat("soil", "#5a4636", 0.95),
        "metal": mat("metal", "#9fb1c4", 0.3, 0.85), "metal_dark": mat("metal_dark", "#3a4a5e", 0.4, 0.8),
        "ruin": mat("ruin", "#5e6670", 0.8, 0.2), "white": mat("white", "#aebdcc", 0.4, 0.25),
        "glow": mat("glow", CYAN, 0.3, 0, CYAN, 9.0 if alive else 0.0),
        "glow_b": mat("glow_b", CYAN, 0.3, 0, CYAN, 7.0),            # buildings are always powered
        "glow_soft": mat("glow_soft", CYAN, 0.3, 0, CYAN, 2.5),
        "glow_green": mat("glow_green", GREEN, 0.3, 0, GREEN, 6.0),
        "glow_gold": mat("glow_gold", "#F5C85B", 0.3, 0, "#F5C85B", 5.0),
        "window": mat("window", "#0e2a3a", 0.2, 0, "#7fe9ff", 1.6 if alive else 0.0),
        "window_b": mat("window_b", "#0e2a3a", 0.2, 0, "#7fe9ff", 1.6),
        "glass": mat("glass", "#5fb8d0", 0.08, 0.2, CYAN, 0.25, transmission=0.35),
        "shield": mat("shield", "#7fd8ff", 0.05, 0, CYAN, 0.5, transmission=0.8),
        "crystal": mat("crystal", "#7ff3ff", 0.1, 0, CYAN, 6.0 if alive else 0.6),
        "pad": mat("pad", "#24364a", 0.5, 0.6), "hull": mat("hull", "#d7e2ec", 0.3, 0.6),
    }
    # Rock strata + grass noise (same look as the original island).
    m = bpy.data.materials.new("rock"); m.use_nodes = True; nt = m.node_tree
    b = nt.nodes["Principled BSDF"]; b.inputs["Roughness"].default_value = 0.9
    tc = nt.nodes.new("ShaderNodeTexCoord"); sep = nt.nodes.new("ShaderNodeSeparateXYZ")
    nz = nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value = 3.0
    add = nt.nodes.new("ShaderNodeMath"); add.operation = "MULTIPLY_ADD"; add.inputs[1].default_value = 0.35
    ramp = nt.nodes.new("ShaderNodeValToRGB"); cr = ramp.color_ramp
    cols = ["#33405a", "#4c5b74", "#66728a", "#45536e", "#7a8296", "#3b4a66"]
    cr.elements[0].color = hexc(cols[0]); cr.elements[1].color = hexc(cols[-1])
    for i, c in enumerate(cols[1:-1]): cr.elements.new((i + 1) / (len(cols) - 1)).color = hexc(c)
    cr.interpolation = "CONSTANT"
    mp = nt.nodes.new("ShaderNodeMapRange"); mp.inputs["From Min"].default_value = -4.0; mp.inputs["From Max"].default_value = 0.3
    L = nt.links.new
    L(tc.outputs["Object"], sep.inputs[0]); L(tc.outputs["Object"], nz.inputs["Vector"]); L(nz.outputs["Fac"], add.inputs[0])
    L(sep.outputs["Z"], add.inputs[2]); L(add.outputs[0], mp.inputs["Value"]); L(mp.outputs["Result"], ramp.inputs["Fac"]); L(ramp.outputs["Color"], b.inputs["Base Color"])
    M["rock"] = m
    g = bpy.data.materials.new("grass"); g.use_nodes = True; nt = g.node_tree
    b = nt.nodes["Principled BSDF"]; b.inputs["Roughness"].default_value = 0.85
    nz = nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value = 1.6; nz.inputs["Detail"].default_value = 4
    ramp = nt.nodes.new("ShaderNodeValToRGB"); cr = ramp.color_ramp; base = hexc(grass_hex)
    cr.elements[0].position = 0.35; cr.elements[0].color = tuple(c * 0.7 for c in base[:3]) + (1,)
    cr.elements[1].position = 0.7; cr.elements[1].color = tuple(min(1, c * 1.35) for c in base[:3]) + (1,)
    nt.links.new(nz.outputs["Fac"], ramp.inputs["Fac"]); nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    M["grass"] = g
    return M

def terrain(M, stage):
    rnd = random.Random(0x4E58)
    bm = bmesh.new(); N = 72; RINGS = 9; top = []
    centre = bm.verts.new((0, 0, ground_z(0, 0)))
    for k in range(1, RINGS + 1):
        ring = []
        for i in range(N):
            th = i / N * 2 * math.pi; r = rim_radius(th) * k / RINGS; x, y = r * math.cos(th), r * math.sin(th)
            ring.append(bm.verts.new((x, y, ground_z(x, y))))
        top.append(ring)
    for i in range(N): bm.faces.new((centre, top[0][i], top[0][(i + 1) % N]))
    for k in range(RINGS - 1):
        for i in range(N): bm.faces.new((top[k][i], top[k + 1][i], top[k + 1][(i + 1) % N], top[k][(i + 1) % N]))
    prev = top[-1]
    levels = [(-0.18, 1.0, "edge"), (-0.45, 0.97, "soil")] + [(z, s, "rock") for z, s in [(-0.9, 0.9), (-1.5, 0.78), (-2.1, 0.6), (-2.7, 0.42), (-3.25, 0.25), (-3.7, 0.11)]]
    for li, (z, s, kind) in enumerate(levels):
        ring = []
        for i in range(N):
            th = i / N * 2 * math.pi
            jit = 1 + (0.09 * noise.noise(Vector((math.cos(th) * 2, math.sin(th) * 2, z * 1.7))) if kind == "rock" else 0)
            r = rim_radius(th) * s * jit
            zz = z + (0.25 * noise.noise(Vector((math.cos(th) * 3, math.sin(th) * 3, li))) if kind == "rock" else 0)
            ring.append(bm.verts.new((r * math.cos(th), r * math.sin(th), zz)))
        for i in range(N):
            bm.faces.new((prev[i], prev[(i + 1) % N], ring[(i + 1) % N], ring[i])).material_index = {"edge": 1, "soil": 2, "rock": 3}[kind]
        prev = ring
    tip = bm.verts.new((0.15, -0.1, -4.4))
    for i in range(N): bm.faces.new((prev[i], prev[(i + 1) % N], tip)).material_index = 3
    me = bpy.data.meshes.new("island"); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new("terrain", me); bpy.context.scene.collection.objects.link(o)
    for m in (M["grass"], M["grass_edge"], M["soil"], M["rock"]): o.data.materials.append(m)
    parts = [o]
    for i in range(14 + stage * 3):
        th = rnd.uniform(0, 2 * math.pi); z = rnd.uniform(-3.0, -0.9)
        s = 1 - (z + 0.9) / -3.5 * 0.75; r = rim_radius(th) * (0.9 + (z / 4.4) * 0.8) * 0.98
        c = prim("cone", (r * math.cos(th), r * math.sin(th), z), (0.15, 0.15, rnd.uniform(0.5, 1.0) * s + 0.25), material=M["crystal"], vertices=5, radius1=1, depth=1)
        c.rotation_mode = "QUATERNION"; c.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(Vector((math.cos(th), math.sin(th), -0.6)).normalized())
        parts.append(c)
    for i in range(4):
        th = rnd.uniform(0, 2 * math.pi)
        parts.append(prim("ico_sphere", (math.cos(th) * rnd.uniform(2.8, 3.6), math.sin(th) * rnd.uniform(1.5, 2.5) - 0.6, rnd.uniform(-3.6, -2.2)),
                          (rnd.uniform(0.2, 0.38),) * 3, material=M["rock"], subdivisions=1))
    # Faint power conduits radiating from the tower plaza once the planet is alive.
    if stage > 0:
        for i in range(6):
            a = i / 6 * 2 * math.pi + 0.3; L = 1.0 + 0.25 * stage
            mid = Vector((math.cos(a), math.sin(a), 0)) * (0.75 + L / 2)
            c = prim("cylinder", (mid.x, mid.y, ground_z(mid.x, mid.y) + 0.02), (0.018, 0.018, L / 2), material=M["glow_soft"], vertices=6)
            c.rotation_mode = "QUATERNION"; c.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(Vector((math.cos(a), math.sin(a), 0)))
            parts.append(c)
    return o, parts

def collect(fn):
    before = set(bpy.context.scene.objects); fn(); return [o for o in bpy.context.scene.objects if o not in before]

def tower(M, stage):
    def build():
        s0 = surface(0, 0)
        prim("cylinder", s0 + Vector((0, 0, 0.08)), (0.85, 0.85, 0.08), material=M["metal_dark"], vertices=8)
        prim("cylinder", s0 + Vector((0, 0, 0.22)), (0.6, 0.6, 0.08), material=M["metal"], vertices=8)
        if stage == 0:
            prim("cylinder", s0 + Vector((0, 0, 0.75)), (0.26, 0.26, 0.5), material=M["ruin"], vertices=8)
            prim("cube", surface(0.35, 0.3) + Vector((0, 0, 0.12)), (0.18, 0.12, 0.1), (0.3, 0.5, 0.4), material=M["ruin"])
            return
        h = [0.9, 1.6, 2.1, 2.5, 2.9][stage]; base = s0.z + 0.3
        prim("cylinder", (0, 0, base + h * 0.35), (0.3, 0.3, h * 0.35), material=M["metal"], vertices=8)
        for k in range(8):
            a = k / 8 * 2 * math.pi + math.pi / 8
            prim("cube", (0.285 * math.cos(a), 0.285 * math.sin(a), base + h * 0.36), (0.03, 0.012, h * 0.3), (0, 0, a + math.pi / 2), material=M["window"])
        prim("cone", (0, 0, base + h * 0.92), (0.24, 0.24, h * 0.22), material=M["white"], vertices=8)
        for k in range(stage + 1):
            prim("torus", (0, 0, base + 0.2 + k * (h * 0.62 / (stage + 1))), (1, 1, 0.6), material=M["glow"], major_radius=0.48 - k * 0.03, minor_radius=0.035, major_segments=40, minor_segments=8)
        top = Vector((0, 0, base + h * 1.16))
        prim("uv_sphere", top, (0.09,) * 3, material=M["glow"], segments=16, ring_count=8)
        l = light("POINT", top, 60 * stage, (0.2, 0.9, 1.0)); l.data.shadow_soft_size = 0.3
    return collect(build)

# ---------------------------------------------------------------------------------------- buildings
S = 1.35  # building scale (readable at phone size)
def B(kind, level, M):
    x, y = SLOTS[kind]; p = surface(x, y)
    def at(dx, dy, dz): return p + Vector((dx * S, dy * S, dz * S))
    def sz(*v): return tuple(c * S for c in v)
    def base_plate(r):
        prim("cylinder", at(0, 0, 0.012), sz(r * 0.8, r * 0.8, 0.012), material=M["metal_dark"], vertices=24)
    def build():
        if kind == "reactor":
            base_plate(0.36 + 0.06 * level)
            prim("cylinder", at(0, 0, 0.12), sz(0.2, 0.2, 0.09), material=M["metal"], vertices=16)
            prim("cylinder", at(0, 0, 0.24), sz(0.12, 0.12, 0.05), material=M["glow_b"], vertices=16)
            prim("torus", at(0, 0, 0.17), (1, 1, 0.5), material=M["glow_b"], major_radius=0.24 * S, minor_radius=0.015 * S, major_segments=32)
            if level >= 2:
                for sx in (-1, 1):
                    prim("cone", at(sx * 0.3, 0.12, 0.15), sz(0.1, 0.1, 0.15), material=M["white"], vertices=12, radius1=1, radius2=0.7, depth=2)
                    prim("cylinder", at(sx * 0.3, 0.12, 0.31), sz(0.07, 0.07, 0.004), material=M["glow_soft"], vertices=12)
            if level >= 3:
                prim("uv_sphere", at(0, 0, 0.3), sz(0.2, 0.2, 0.16), material=M["glass"], segments=24, ring_count=12)
                prim("cylinder", at(0, 0, 0.75), sz(0.02, 0.02, 0.4), material=M["glow_b"], vertices=8)
                prim("torus", at(0, 0, 0.48), (1, 1, 1), (0.4, 0, 0), material=M["glow_b"], major_radius=0.3 * S, minor_radius=0.01 * S, major_segments=40)
        elif kind == "colony":
            spots = [(0, 0, 0.26)] + ([(-0.33, 0.12, 0.2)] if level >= 2 else []) + ([(0.32, 0.16, 0.2)] if level >= 3 else [])
            base_plate(0.3 + 0.17 * (level - 1))
            for dx, dy, r in spots:
                prim("cylinder", at(dx, dy, 0.07), sz(r + 0.03, r + 0.03, 0.03), material=M["metal_dark"], vertices=20)
                prim("uv_sphere", at(dx, dy, 0.1), sz(r, r, r * 0.85), material=M["glass"], segments=24, ring_count=12)
                prim("cylinder", at(dx, dy, 0.11), sz(r + 0.01, r + 0.01, 0.01), material=M["glow_soft"], vertices=24)
            if level >= 3:
                prim("cube", at(0.05, 0.35, 0.35), sz(0.11, 0.11, 0.35), material=M["white"])
                for k in range(3): prim("cube", at(0.05, 0.239, 0.2 + k * 0.17), sz(0.09, 0.004, 0.025), material=M["window_b"])
        elif kind == "lab":
            h = [0.32, 0.45, 0.85][level - 1]
            base_plate(0.38)
            prim("cube", at(0, 0, h / 2 + 0.06), sz(0.24, 0.2, h / 2), material=M["white"])
            for k in range(1 + level):
                prim("cube", at(0, -0.201, 0.16 + k * min(0.17, h / (1.5 + level))), sz(0.21, 0.004, 0.03), material=M["window_b"])
            prim("cylinder", at(0.14, 0.06, h + 0.22), sz(0.01, 0.01, 0.16), material=M["metal"], vertices=6)
            prim("uv_sphere", at(0.14, 0.06, h + 0.39), sz(0.03, 0.03, 0.03), material=M["glow_green"], segments=8, ring_count=6)
            if level >= 2:
                prim("cylinder", at(-0.32, 0.12, 0.16), sz(0.04, 0.04, 0.16), material=M["metal_dark"], vertices=8)
                prim("uv_sphere", at(-0.32, 0.12, 0.36), sz(0.22, 0.22, 0.07), (0.9, 0, 0.7), material=M["white"], segments=24, ring_count=8)
            if level >= 3:
                prim("cone", at(0, 0, h + 0.17), sz(0.17, 0.15, 0.12), material=M["white"], vertices=4)
                prim("torus", at(0, 0, h * 0.75), (1, 1, 0.4), material=M["glow_b"], major_radius=0.32 * S, minor_radius=0.01 * S, major_segments=40)
        elif kind == "comms":
            h = [0.6, 0.75, 1.05][level - 1]
            base_plate(0.28)
            prim("cylinder", at(0, 0, h / 2), sz(0.04, 0.04, h / 2), material=M["metal"], vertices=8)
            for k in range(2 + level):
                prim("cylinder", at(0, 0, 0.15 + k * h / (2.5 + level)), sz(0.11 - k * 0.012, 0.11 - k * 0.012, 0.006), material=M["metal_dark"], vertices=12)
            prim("uv_sphere", at(0, 0, h + 0.03), sz(0.035, 0.035, 0.035), material=M["glow_gold"], segments=10, ring_count=6)
            if level >= 2:
                for sx in (-1, 1):
                    prim("uv_sphere", at(sx * 0.22, -0.05, 0.22), sz(0.12, 0.12, 0.04), (1.1, 0, sx * 0.5), material=M["white"], segments=20, ring_count=6)
            if level >= 3:
                for k in range(2):
                    prim("torus", at(0, 0, h * (0.55 + 0.2 * k)), (1, 1, 1), (0.3 + k * 0.5, 0.2, 0), material=M["glow_b"], major_radius=(0.2 - 0.04 * k) * S, minor_radius=0.008 * S, major_segments=36)
        elif kind == "spaceport":
            base_plate(0.42)
            prim("cylinder", at(0, 0, 0.07), sz(0.36, 0.36, 0.02), material=M["pad"], vertices=32)
            prim("torus", at(0, 0, 0.095), (1, 1, 0.5), material=M["glow_b"], major_radius=0.3 * S, minor_radius=0.012 * S, major_segments=40)
            for k in range(4):
                a = k * math.pi / 2 + math.pi / 4
                prim("cylinder", at(0.3 * math.cos(a), 0.3 * math.sin(a), 0.1), sz(0.018, 0.018, 0.012), material=M["glow_gold"], vertices=8)
            if level >= 2:
                prim("cylinder", at(0.38, 0.28, 0.25), sz(0.06, 0.06, 0.25), material=M["metal"], vertices=10)
                prim("cylinder", at(0.38, 0.28, 0.53), sz(0.1, 0.1, 0.05), material=M["window_b"], vertices=10)
            if level >= 3:
                ship = prim("cone", at(0, 0, 0.2), sz(0.1, 0.1, 0.24), (math.radians(90), 0, math.radians(30)), material=M["hull"], vertices=8)
                for sx in (-1, 1):
                    prim("cube", at(sx * 0.07, 0.04, 0.16), sz(0.09, 0.03, 0.008), (0, 0, math.radians(30)), material=M["hull"])
                prim("uv_sphere", at(-0.11, -0.19, 0.2), sz(0.035,) * 3 if False else sz(0.035, 0.035, 0.035), material=M["glow_b"], segments=8, ring_count=6)
        elif kind == "defense":
            h = [0.35, 0.55, 0.75][level - 1]
            base_plate(0.3)
            prim("cylinder", at(0, 0, h / 2), sz(0.09, 0.12, h / 2), material=M["metal_dark"], vertices=8)
            prim("cylinder", at(0, 0, h + 0.04), sz(0.13, 0.13, 0.04), material=M["metal"], vertices=8)
            prim("cube", at(0.1, -0.08, h + 0.08), sz(0.14, 0.022, 0.022), (0, 0, -0.6), material=M["white"])
            prim("uv_sphere", at(0, 0, h + 0.12), sz(0.045, 0.045, 0.045), material=M["glow_b"], segments=10, ring_count=6)
            if level >= 2:
                for k in range(3):
                    a = k * 2 * math.pi / 3
                    prim("cylinder", at(0.26 * math.cos(a), 0.26 * math.sin(a), 0.12), sz(0.025, 0.025, 0.12), material=M["metal"], vertices=6)
                    prim("uv_sphere", at(0.26 * math.cos(a), 0.26 * math.sin(a), 0.26), sz(0.025, 0.025, 0.025), material=M["glow_b"], segments=8, ring_count=4)
            if level >= 3:
                prim("uv_sphere", at(0, 0, 0.0), sz(0.48, 0.48, 0.48), material=M["shield"], segments=32, ring_count=16)
    return collect(build), at(0, 0, 0)

# ------------------------------------------------------------------------------------------- render
def render_layer(name, show, catcher, cam):
    """Renders only [show] (+ terrain as shadow catcher), adds emission bloom, crops; returns crop box + depth."""
    import numpy as np
    from PIL import Image, ImageFilter
    sc = bpy.context.scene
    meshes = [o for o in sc.objects if o.type == "MESH"]
    for o in meshes: o.hide_render = not (o in show or o is catcher)
    if catcher is not None: catcher.is_shadow_catcher = True
    path = os.path.join(A.out, name + ".png"); sc.render.filepath = path
    bpy.ops.render.render(write_still=True)
    # Emission-only pass (non-emissive → matte black, lights off), then restore.
    saved = {}
    black = bpy.data.materials.get("black") or bpy.data.materials.new("black")
    black.use_nodes = True; bb = black.node_tree.nodes["Principled BSDF"]
    bb.inputs["Base Color"].default_value = (0, 0, 0, 1); bb.inputs["Roughness"].default_value = 1; bb.inputs["Specular IOR Level"].default_value = 0
    for o in show:
        if o.type != "MESH": continue
        for i, m in enumerate(o.data.materials):
            pb = m.node_tree.nodes.get("Principled BSDF") if m else None
            if pb is None or pb.inputs["Emission Strength"].default_value <= 0:
                saved[(o.name, i)] = m; o.data.materials[i] = black
    lights = [(o, o.data.energy) for o in sc.objects if o.type == "LIGHT"]
    for o, _ in lights: o.data.energy = 0
    ws = sc.world.node_tree.nodes["Background"].inputs[1].default_value; sc.world.node_tree.nodes["Background"].inputs[1].default_value = 0
    samples = sc.cycles.samples; sc.cycles.samples = max(8, samples // 4)
    epath = path.replace(".png", "_e.png"); sc.render.filepath = epath; bpy.ops.render.render(write_still=True)
    for (on, i), m in saved.items(): sc.objects[on].data.materials[i] = m
    for o, e in lights: o.data.energy = e
    sc.world.node_tree.nodes["Background"].inputs[1].default_value = ws; sc.cycles.samples = samples
    if catcher is not None: catcher.is_shadow_catcher = False
    im = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32) / 255
    e = np.asarray(Image.open(epath).convert("RGBA")).astype(np.float32) / 255; os.remove(epath)
    rgb, al = im[..., :3], im[..., 3:4]
    br = Image.fromarray((np.clip(e[..., :3] * e[..., 3:4], 0, 1) * 255).astype(np.uint8))
    g = (np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 120))).astype(np.float32) * 0.5 + np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 40))).astype(np.float32) * 0.35) / 255
    out_rgb = rgb * al + g; out_a = np.clip(np.maximum(al, g.max(axis=2, keepdims=True)), 0, 1)
    out_a[out_a < 10 / 255] = 0  # drop denoiser speckle (shadow-catcher layers otherwise span the frame)
    st = np.where(out_a > 1e-4, out_rgb / np.maximum(out_a, 1e-4), 0)
    img = Image.fromarray((np.concatenate([np.clip(st, 0, 1), out_a], axis=2) * 255).astype(np.uint8))
    box = img.getchannel("A").point(lambda a: 255 if a > 0 else 0).getbbox()
    img.crop(box).save(path)
    return list(box)

def depth_of(cam, p):
    return -(cam.matrix_world.inverted() @ Vector(p)).z

manifest = {"res": A.res, "layers": {}}
mpath = os.path.join(A.out, "layers.json")
if os.path.exists(mpath): manifest = json.load(open(mpath))
def want(n): return not ONLY or n in ONLY

for stage in range(5):
    if not (want(f"base_s{stage}") or want(f"tower_s{stage}") or (stage == 2 and any(want(f"b_{t}_{l}") for t in TYPES for l in (1, 2, 3)))):
        continue
    sc, cam = new_scene(stage); M = materials(stage)
    ter, ter_parts = terrain(M, stage)
    tw = tower(M, stage)
    if want(f"base_s{stage}"):
        manifest["layers"][f"base_s{stage}"] = {"box": render_layer(f"base_s{stage}", ter_parts, None, cam), "depth": 1e9}
    if want(f"tower_s{stage}"):
        manifest["layers"][f"tower_s{stage}"] = {"box": render_layer(f"tower_s{stage}", tw, ter, cam), "depth": depth_of(cam, (0, 0, 0))}
    if stage == 2:  # buildings render once, on the lush terrain (only their own shadows are kept)
        for o in tw: bpy.data.objects.remove(o, do_unlink=True)
        for t in TYPES:
            for lv in (1, 2, 3):
                n = f"b_{t}_{lv}"
                if not want(n): continue
                objs, anchor = B(t, lv, M)
                manifest["layers"][n] = {"box": render_layer(n, objs, ter, cam), "depth": depth_of(cam, anchor)}
                for o in objs: bpy.data.objects.remove(o, do_unlink=True)
    json.dump(manifest, open(mpath, "w"), indent=1)
print("done", len(manifest["layers"]), "layers")
