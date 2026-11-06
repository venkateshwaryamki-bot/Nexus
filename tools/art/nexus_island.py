"""
NEXUS art pipeline — Nexus Prime floating island (2.5D pre-rendered sprites).

Builds the island procedurally in Blender (bpy) and renders one transparent sprite per civilization
stage (0 Abandoned Outpost … 4 Planetary Civilization). Deterministic: same seed, same art.

    python tools/art/nexus_island.py --stage 2 --out build/art/island_2.png [--res 1080] [--samples 64]

Requires the `bpy` module (pip install bpy, Python 3.13). See ASSET_PIPELINE.md.
"""
import argparse, math, random
import bpy, bmesh
from mathutils import Vector, noise

P = argparse.ArgumentParser()
P.add_argument("--stage", type=int, default=2)
P.add_argument("--out", default="island.png")
P.add_argument("--res", type=int, default=1080)
P.add_argument("--samples", type=int, default=64)
A = P.parse_args()
STAGE = max(0, min(4, A.stage))
ALIVE = STAGE > 0
rnd = random.Random(0x4E58)

# ---------------------------------------------------------------- scene ---------------------------------------------
bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = "CYCLES"; sc.cycles.device = "CPU"; sc.cycles.samples = A.samples; sc.cycles.use_denoising = True
sc.render.film_transparent = True
sc.render.resolution_x = A.res; sc.render.resolution_y = int(A.res * 1.0)
sc.view_settings.view_transform = "AgX"; sc.view_settings.look = "AgX - Medium High Contrast"
sc.render.image_settings.file_format = "PNG"; sc.render.image_settings.color_mode = "RGBA"
world = bpy.data.worlds.new("w"); sc.world = world; world.use_nodes = True
bg = world.node_tree.nodes["Background"]; bg.inputs[0].default_value = (0.012, 0.03, 0.07, 1); bg.inputs[1].default_value = 0.9

def hexc(h, a=1.0):
    h = h.lstrip("#"); r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    lin = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (lin(r), lin(g), lin(b), a)

def mat(name, color, rough=0.6, metal=0.0, emit=None, emit_strength=0.0, alpha=1.0, transmission=0.0):
    m = bpy.data.materials.new(name); m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = hexc(color) if isinstance(color, str) else color
    b.inputs["Roughness"].default_value = rough; b.inputs["Metallic"].default_value = metal
    if emit:
        b.inputs["Emission Color"].default_value = hexc(emit); b.inputs["Emission Strength"].default_value = emit_strength
    if transmission: b.inputs["Transmission Weight"].default_value = transmission
    if alpha < 1: b.inputs["Alpha"].default_value = alpha
    return m

CYAN = "#20D6EA"; GREEN = "#37E6A0"
# Grass shifts from dead grey-olive (abandoned) to lush teal-green as the planet is restored.
grass_hex = ["#5b6157", "#3f8f5a", "#3fa863", "#40b56c", "#44c27a"][STAGE]
M = {
    "grass": None,
    "grass_edge": mat("grass_edge", ["#4a4f47", "#2f7347", "#2f8a50", "#2f9658", "#31a060"][STAGE], 0.9),
    "soil": mat("soil", "#5a4636", 0.95),
    "metal": mat("metal", "#9fb1c4", 0.3, 0.85),
    "metal_dark": mat("metal_dark", "#3a4a5e", 0.4, 0.8),
    "ruin": mat("ruin", "#5e6670", 0.8, 0.2),
    "white": mat("white", "#aebdcc", 0.4, 0.25),
    "glow": mat("glow", CYAN, 0.3, 0, CYAN, 9.0 if ALIVE else 0.0),
    "glow_soft": mat("glow_soft", CYAN, 0.3, 0, CYAN, 3.0 if ALIVE else 0.0),
    "glow_green": mat("glow_green", GREEN, 0.3, 0, GREEN, 6.0),
    "window": mat("window", "#0e2a3a", 0.2, 0, "#7fe9ff", 1.6 if ALIVE else 0.0),
    "glass": mat("glass", "#5fb8d0", 0.08, 0.2, CYAN, 0.25 if ALIVE else 0.0, transmission=0.35),
    "trunk": mat("trunk", "#6b4a33", 0.9),
    "leaf": [mat("leaf%d" % i, c, 0.8) for i, c in enumerate(["#2f9e5c", "#3cb46b", "#2a8a55", "#4cc47a"])],
    "dead": mat("dead", "#6a6158", 0.95),
    "crystal": mat("crystal", "#7ff3ff", 0.1, 0, CYAN, 6.0 if ALIVE else 0.6),
    "pad": mat("pad", "#24364a", 0.5, 0.6),
}

# Rock with strata bands (z-driven colour ramp + noise), the island's signature underside.
def rock_material():
    m = bpy.data.materials.new("rock"); m.use_nodes = True; nt = m.node_tree
    b = nt.nodes["Principled BSDF"]; b.inputs["Roughness"].default_value = 0.9
    tc = nt.nodes.new("ShaderNodeTexCoord"); sep = nt.nodes.new("ShaderNodeSeparateXYZ")
    nz = nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value = 3.0
    add = nt.nodes.new("ShaderNodeMath"); add.operation = "MULTIPLY_ADD"; add.inputs[1].default_value = 0.35
    ramp = nt.nodes.new("ShaderNodeValToRGB"); cr = ramp.color_ramp
    cols = ["#33405a", "#4c5b74", "#66728a", "#45536e", "#7a8296", "#3b4a66"]
    cr.elements[0].position = 0.0; cr.elements[0].color = hexc(cols[0])
    cr.elements[1].position = 1.0; cr.elements[1].color = hexc(cols[-1])
    for i, c in enumerate(cols[1:-1]):
        e = cr.elements.new((i + 1) / (len(cols) - 1)); e.color = hexc(c)
    cr.interpolation = "CONSTANT"
    mp = nt.nodes.new("ShaderNodeMapRange"); mp.inputs["From Min"].default_value = -4.0; mp.inputs["From Max"].default_value = 0.3
    nt.links.new(tc.outputs["Object"], sep.inputs[0]); nt.links.new(tc.outputs["Object"], nz.inputs["Vector"])
    nt.links.new(nz.outputs["Fac"], add.inputs[0]); nt.links.new(sep.outputs["Z"], add.inputs[2])
    nt.links.new(add.outputs[0], mp.inputs["Value"]); nt.links.new(mp.outputs["Result"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    return m
M["rock"] = rock_material()

def grass_material():
    m = bpy.data.materials.new("grass"); m.use_nodes = True; nt = m.node_tree
    b = nt.nodes["Principled BSDF"]; b.inputs["Roughness"].default_value = 0.85
    nz = nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value = 1.6; nz.inputs["Detail"].default_value = 4
    ramp = nt.nodes.new("ShaderNodeValToRGB"); cr = ramp.color_ramp
    base = hexc(grass_hex)
    cr.elements[0].position = 0.35; cr.elements[0].color = tuple(c * 0.7 for c in base[:3]) + (1,)
    cr.elements[1].position = 0.7; cr.elements[1].color = tuple(min(1, c * 1.35) for c in base[:3]) + (1,)
    nt.links.new(nz.outputs["Fac"], ramp.inputs["Fac"]); nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    return m
M["grass"] = grass_material()

def obj(name, mesh, mats, smooth=False):
    o = bpy.data.objects.new(name, mesh); sc.collection.objects.link(o)
    for m in mats: o.data.materials.append(m)
    if smooth:
        for p in o.data.polygons: p.use_smooth = True
    return o

def prim(kind, loc, scale=(1, 1, 1), rot=(0, 0, 0), material=None, **kw):
    getattr(bpy.ops.mesh, "primitive_%s_add" % kind)(location=loc, rotation=rot, **kw)
    o = bpy.context.object; o.scale = scale
    if material: o.data.materials.append(material)
    return o

# ---------------------------------------------------------------- island --------------------------------------------
R = 3.2
def rim_radius(theta):
    return R * (1 + 0.10 * math.sin(3 * theta + 0.7) + 0.06 * math.sin(5 * theta + 2.1) + 0.04 * math.sin(9 * theta))

def ground_z(x, y):
    """Gentle hills on the top surface (0 at the centre plaza)."""
    r = math.hypot(x, y)
    return 0.18 * noise.noise(Vector((x * 0.45, y * 0.45, 1.3))) * min(1, r / 1.2) + 0.08 * (1 - min(1, r / R))

def build_island():
    bm = bmesh.new(); N = 72; RINGS = 9
    top = []
    centre = bm.verts.new((0, 0, ground_z(0, 0)))
    for k in range(1, RINGS + 1):
        ring = []
        for i in range(N):
            th = i / N * 2 * math.pi; r = rim_radius(th) * k / RINGS
            x, y = r * math.cos(th), r * math.sin(th)
            ring.append(bm.verts.new((x, y, ground_z(x, y))))
        top.append(ring)
    for i in range(N):
        bm.faces.new((centre, top[0][i], top[0][(i + 1) % N]))
    for k in range(RINGS - 1):
        for i in range(N):
            a, b = top[k][i], top[k][(i + 1) % N]; c, d = top[k + 1][(i + 1) % N], top[k + 1][i]
            bm.faces.new((a, d, c, b))
    # Grass lip, then rock tapering to a jagged point.
    prev = top[-1]; levels = [(-0.18, 1.0, "edge"), (-0.45, 0.97, "soil")]
    depth = [(-0.9, 0.9), (-1.5, 0.78), (-2.1, 0.6), (-2.7, 0.42), (-3.25, 0.25), (-3.7, 0.11)]
    levels += [(z, s, "rock") for z, s in depth]
    for li, (z, s, kind) in enumerate(levels):
        ring = []
        for i in range(N):
            th = i / N * 2 * math.pi
            jitter = 1 + (0.09 * noise.noise(Vector((math.cos(th) * 2, math.sin(th) * 2, z * 1.7))) if kind == "rock" else 0)
            r = rim_radius(th) * s * jitter
            zz = z + (0.25 * noise.noise(Vector((math.cos(th) * 3, math.sin(th) * 3, li))) if kind == "rock" else 0)
            ring.append(bm.verts.new((r * math.cos(th), r * math.sin(th), zz)))
        for i in range(N):
            f = bm.faces.new((prev[i], prev[(i + 1) % N], ring[(i + 1) % N], ring[i]))
            f.material_index = {"edge": 1, "soil": 2, "rock": 3}[kind]
        prev = ring
    tip = bm.verts.new((0.15, -0.1, -4.4))
    for i in range(N):
        bm.faces.new((prev[i], prev[(i + 1) % N], tip)).material_index = 3
    me = bpy.data.meshes.new("island"); bm.to_mesh(me); bm.free()
    return obj("island", me, [M["grass"], M["grass_edge"], M["soil"], M["rock"]])

island = build_island()

def surface(x, y):
    return Vector((x, y, ground_z(x, y)))

# Crystals growing out of the rock underside; brighter as the planet powers up.
for i in range(14 + STAGE * 3):
    th = rnd.uniform(0, 2 * math.pi); z = rnd.uniform(-3.0, -0.9)
    s = {-3.0: 0.25}.get(z, 1) * (1 - (z + 0.9) / -3.5 * 0.75)
    r = rim_radius(th) * (0.9 + (z / 4.4) * 0.8) * 0.98
    loc = Vector((r * math.cos(th), r * math.sin(th), z))
    out = Vector((math.cos(th), math.sin(th), -0.6)).normalized()
    o = prim("cone", loc, (0.15, 0.15, rnd.uniform(0.5, 1.0) * s + 0.25), material=M["crystal"], vertices=5, radius1=1, depth=1)
    o.rotation_mode = "QUATERNION"; o.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(out)

# Small floating rocks below the island.
for i in range(4):
    th = rnd.uniform(0, 2 * math.pi)
    o = prim("ico_sphere", (math.cos(th) * rnd.uniform(2.8, 3.6), math.sin(th) * rnd.uniform(1.5, 2.5) - 0.6, rnd.uniform(-3.6, -2.2)),
             (rnd.uniform(0.2, 0.38),) * 3, material=M["rock"], subdivisions=1)

# ---------------------------------------------------------------- Nexus Tower -----------------------------------------
def tower():
    h = [0.9, 1.6, 2.1, 2.5, 2.9][STAGE]
    prim("cylinder", surface(0, 0) + Vector((0, 0, 0.08)), (0.85, 0.85, 0.08), material=M["metal_dark"], vertices=8)
    prim("cylinder", surface(0, 0) + Vector((0, 0, 0.22)), (0.6, 0.6, 0.08), material=M["metal"], vertices=8)
    if STAGE == 0:  # broken stub, no power
        prim("cylinder", surface(0, 0) + Vector((0, 0, 0.75)), (0.26, 0.26, 0.5), material=M["ruin"], vertices=8)
        prim("cube", surface(0.35, 0.3) + Vector((0, 0, 0.12)), (0.18, 0.12, 0.1), (0.3, 0.5, 0.4), material=M["ruin"])
        return surface(0, 0) + Vector((0, 0, 1.3))
    base = surface(0, 0).z + 0.3
    prim("cylinder", (0, 0, base + h * 0.35), (0.3, 0.3, h * 0.35), material=M["metal"], vertices=8)
    for k in range(8):
        a = k / 8 * 2 * math.pi + math.pi / 8
        o = prim("cube", (0.285 * math.cos(a), 0.285 * math.sin(a), base + h * 0.36), (0.03, 0.012, h * 0.3), (0, 0, a + math.pi / 2), material=M["window"])
    prim("cone", (0, 0, base + h * 0.7 + h * 0.22), (0.24, 0.24, h * 0.22), material=M["white"], vertices=8)
    for k in range(STAGE + 1):  # energy rings multiply with each stage
        z = base + 0.2 + k * (h * 0.62 / (STAGE + 1))
        prim("torus", (0, 0, z), (1, 1, 0.6), material=M["glow"], major_radius=0.48 - k * 0.03, minor_radius=0.035, major_segments=40, minor_segments=8)
    top = Vector((0, 0, base + h * 0.7 + h * 0.46))
    prim("uv_sphere", top, (0.09, 0.09, 0.09), material=M["glow"], segments=16, ring_count=8)
    return top

tower_top = tower()

# ---------------------------------------------------------------- buildings -------------------------------------------
# Site rings around the tower; stage n fills the first slots. Positions are fixed across stages so
# the city *grows* rather than reshuffles.
sites = []
for ring, (rad, count) in enumerate([(1.55, 6), (2.25, 8), (2.75, 10)]):
    for i in range(count):
        th = i / count * 2 * math.pi + ring * 0.37
        rr = rad * rim_radius(th) / R
        sites.append((rr * math.cos(th), rr * math.sin(th), ring))
city_count = [3, 5, 9, 15, 22][STAGE]

def dome(p):
    prim("cylinder", p + Vector((0, 0, 0.05)), (0.32, 0.32, 0.05), material=M["metal_dark"], vertices=16)
    prim("uv_sphere", p + Vector((0, 0, 0.1)), (0.28, 0.28, 0.24), material=M["glass"], segments=24, ring_count=12)
    prim("cylinder", p + Vector((0, 0, 0.11)), (0.3, 0.3, 0.012), material=M["glow_soft"], vertices=24)

def lab(p, tall):
    h = 0.35 + 0.35 * tall
    prim("cube", p + Vector((0, 0, h / 2)), (0.26, 0.2, h / 2), material=M["white"])
    for k in range(int(1 + tall * 2)):
        prim("cube", p + Vector((0, -0.201, 0.12 + k * 0.18)), (0.22, 0.005, 0.035), material=M["window"])
    prim("cylinder", p + Vector((0.12, 0.05, h + 0.18)), (0.012, 0.012, 0.18), material=M["metal"], vertices=6)
    prim("uv_sphere", p + Vector((0.12, 0.05, h + 0.37)), (0.03,) * 3, material=M["glow_green"], segments=8, ring_count=6)

def dish(p):
    prim("cylinder", p + Vector((0, 0, 0.18)), (0.05, 0.05, 0.18), material=M["metal_dark"], vertices=8)
    d = prim("uv_sphere", p + Vector((0, 0, 0.42)), (0.3, 0.3, 0.09), (0.9, 0, 0.6), material=M["white"], segments=24, ring_count=8)
    prim("cylinder", p + Vector((0.12, -0.18, 0.5)), (0.012, 0.012, 0.13), (0.9, 0, 0.6), material=M["glow"], vertices=6)

def spire(p, h):
    prim("cube", p + Vector((0, 0, h / 2)), (0.16, 0.16, h / 2), material=M["metal"])
    for k in range(int(h / 0.22)):
        prim("cube", p + Vector((0, 0, 0.15 + k * 0.22)), (0.165, 0.165, 0.025), material=M["window"])
    prim("cone", p + Vector((0, 0, h + 0.15)), (0.13, 0.13, 0.15), material=M["white"], vertices=4)

def pad(p):
    prim("cylinder", p + Vector((0, 0, 0.03)), (0.34, 0.34, 0.03), material=M["pad"], vertices=24)
    prim("torus", p + Vector((0, 0, 0.065)), (1, 1, 0.5), material=M["glow"], major_radius=0.29, minor_radius=0.02, major_segments=32, minor_segments=6)

def ruin(p):
    for k in range(3):
        prim("cube", p + Vector((rnd.uniform(-0.15, 0.15), rnd.uniform(-0.15, 0.15), 0.08 + k * 0.05)),
             (rnd.uniform(0.06, 0.16), rnd.uniform(0.06, 0.14), rnd.uniform(0.05, 0.12)),
             (rnd.uniform(-0.4, 0.4), rnd.uniform(-0.4, 0.4), rnd.uniform(0, 3)), material=M["ruin"])

kinds_by_stage = {0: ["ruin"], 1: ["dome", "lab", "dome"], 2: ["dome", "lab", "dish", "pad", "lab"],
                  3: ["dome", "lab", "spire", "dish", "lab", "pad", "spire"], 4: ["spire", "dome", "lab", "spire", "dish", "pad", "spire", "lab"]}
placed = []
BUILD_SCALE = 1.55
def scaled(build, p):
    before = set(sc.objects)
    build()
    for o in set(sc.objects) - before:
        o.location = p + (o.location - p) * BUILD_SCALE
        o.scale = o.scale * BUILD_SCALE
for idx in range(city_count):
    x, y, ring = sites[idx]
    p = surface(x, y)
    kinds = kinds_by_stage[STAGE]
    k = kinds[idx % len(kinds)] if STAGE > 0 else "ruin"
    builder = {"dome": lambda: dome(p), "lab": lambda: lab(p, (2 - ring) / 2 + 0.3 * (STAGE >= 3)), "dish": lambda: dish(p),
     "spire": lambda: spire(p, 0.55 + 0.35 * (2 - ring) + 0.15 * (STAGE == 4)), "pad": lambda: pad(p), "ruin": lambda: ruin(p)}[k]
    scaled(builder, p)
    placed.append(p)

# Energy conduits from each building to the tower (glowing ground lines).
if ALIVE:
    for p in placed:
        v = Vector((-p.x, -p.y, 0)); L = v.length - 0.75
        if L <= 0: continue
        mid = p + v.normalized() * (L / 2 + 0.05)
        o = prim("cylinder", Vector((mid.x, mid.y, ground_z(mid.x, mid.y) + 0.02)), (0.02, 0.02, L / 2), material=M["glow_soft"], vertices=6)
        o.rotation_mode = "QUATERNION"; o.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(v.normalized())

# ---------------------------------------------------------------- trees -----------------------------------------------
def tree(p, size, dead=False, variant=0):
    if dead: size *= 1.6
    trunk_h = 0.25 * size
    prim("cylinder", p + Vector((0, 0, trunk_h / 2)), (0.035 * size, 0.035 * size, trunk_h / 2), material=M["trunk"] if not dead else M["dead"], vertices=6)
    if dead:
        for k in range(2):
            o = prim("cylinder", p + Vector((0.04 * (k * 2 - 1), 0, trunk_h * 0.85)), (0.015, 0.015, 0.1 * size), (0, 0.6 * (k * 2 - 1), 0), material=M["dead"], vertices=5)
        return
    leaf = M["leaf"][variant % 4]
    for (dx, dy, dz, s) in [(0, 0, 0.42, 0.24), (0.09, 0.04, 0.32, 0.17), (-0.08, -0.05, 0.34, 0.16)]:
        prim("ico_sphere", p + Vector((dx * size, dy * size, dz * size)), (s * size,) * 3, material=leaf, subdivisions=1)

tree_slots = []
for i in range(40):
    th = rnd.uniform(0, 2 * math.pi); rr = rnd.uniform(1.0, 0.93) * rim_radius(th) * rnd.uniform(0.55, 0.95)
    x, y = rr * math.cos(th), rr * math.sin(th)
    if all((Vector((x, y)) - Vector((s.x, s.y))).length > 0.6 for s in placed) and math.hypot(x, y) > 1.2:
        tree_slots.append((x, y))
tree_count = [5, 9, 14, 18, 24][STAGE]
for i, (x, y) in enumerate(tree_slots[:tree_count]):
    size = [1.0, 1.15, 1.35, 1.5, 1.6][STAGE] * rnd.uniform(0.75, 1.2)
    tree(surface(x, y), size, dead=(STAGE == 0), variant=i)


# ---------------------------------------------------------------- lights & camera -------------------------------------
def light(kind, loc, energy, color, rot=None, size=1.0):
    d = bpy.data.lights.new(kind, kind); d.energy = energy; d.color = color
    if kind == "AREA": d.size = size
    o = bpy.data.objects.new(kind, d); sc.collection.objects.link(o); o.location = loc
    if rot: o.rotation_euler = rot
    return o

sun = light("SUN", (0, 0, 10), 3.4, (1.0, 0.95, 0.88), rot=(math.radians(50), math.radians(-25), math.radians(-35)))
sun.data.angle = math.radians(6)
light("AREA", (4, 5, 3), 900, (0.25, 0.85, 1.0), rot=(math.radians(-60), math.radians(30), math.radians(140)), size=6)  # cyan rim from behind
light("AREA", (0, -9, -3), 380, (0.55, 0.7, 1.0), rot=(math.radians(110), 0, 0), size=8)                           # soft front fill on the rock
light("AREA", (-6, -4, -4), 250, (0.55, 0.45, 1.0), rot=(math.radians(120), 0, math.radians(-50)), size=6)           # violet underglow
if ALIVE:
    l = light("POINT", tower_top, 60 * STAGE, (0.2, 0.9, 1.0)); l.data.shadow_soft_size = 0.3

cam_d = bpy.data.cameras.new("cam"); cam_d.type = "ORTHO"; cam_d.ortho_scale = 9.4
cam = bpy.data.objects.new("cam", cam_d); sc.collection.objects.link(cam); sc.camera = cam
cam.location = (0, -14, 5.2)
direction = Vector((0, 0, -0.35)) - cam.location
cam.rotation_mode = "QUATERNION"; cam.rotation_quaternion = direction.to_track_quat("-Z", "Y")

sc.render.filepath = A.out
bpy.ops.render.render(write_still=True)

# Emission-only render: every non-emissive surface becomes matte black (still occludes), so bloom
# comes only from real light sources rather than sunlit white walls.
main_out = A.out
emit_out = A.out.replace(".png", "_emit.png")
black = bpy.data.materials.new("black"); black.use_nodes = True
bb = black.node_tree.nodes["Principled BSDF"]; bb.inputs["Base Color"].default_value = (0, 0, 0, 1)
bb.inputs["Roughness"].default_value = 1.0; bb.inputs["Specular IOR Level"].default_value = 0.0
def emits(m):
    b = m.node_tree.nodes.get("Principled BSDF") if m and m.use_nodes else None
    return b is not None and b.inputs["Emission Strength"].default_value > 0
for o in sc.objects:
    if o.type == "MESH":
        for i, m in enumerate(o.data.materials):
            if not emits(m): o.data.materials[i] = black
for o in sc.objects:
    if o.type == "LIGHT": o.data.energy = 0
world.node_tree.nodes["Background"].inputs[1].default_value = 0
sc.cycles.samples = max(8, A.samples // 4)
sc.render.filepath = emit_out
bpy.ops.render.render(write_still=True)

# Bloom: emission pass blurred at two radii and added back, with the glow also extending alpha (halo outside the silhouette).
try:
    import numpy as np
    from PIL import Image, ImageFilter
    import os
    im = Image.open(main_out).convert("RGBA"); a = np.asarray(im).astype(np.float32) / 255
    rgb, al = a[..., :3], a[..., 3:4]
    e = np.asarray(Image.open(emit_out).convert("RGBA")).astype(np.float32) / 255
    bright = e[..., :3] * e[..., 3:4]
    br = Image.fromarray((np.clip(bright, 0, 1) * 255).astype(np.uint8))
    g1 = np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 120))).astype(np.float32) / 255
    g2 = np.asarray(br.filter(ImageFilter.GaussianBlur(A.res / 40))).astype(np.float32) / 255
    glow = g1 * 0.5 + g2 * 0.35
    out_rgb = rgb * al + glow
    out_a = np.clip(np.maximum(al, glow.max(axis=2, keepdims=True)), 0, 1)
    straight = np.where(out_a > 1e-4, out_rgb / np.maximum(out_a, 1e-4), 0)
    res = np.concatenate([np.clip(straight, 0, 1), out_a], axis=2)
    Image.fromarray((res * 255).astype(np.uint8)).save(main_out)
    os.remove(emit_out)
except ImportError:
    print("bloom skipped (numpy/Pillow missing in this Python)")
print("rendered stage", STAGE, "->", A.out)
