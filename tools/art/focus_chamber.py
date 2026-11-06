"""
NEXUS art pipeline — Focus Chamber environments (portrait backdrops) and the energy core sprite.

    python tools/art/focus_chamber.py --env command_centre --out build/art/chamber_command_centre.png
    python tools/art/focus_chamber.py --core --out build/art/energy_core.png

Backdrop layout contract (fractions of image height, used by FocusScreen):
  portal/timer centre ≈ 0.36 · pedestal top ≈ 0.70 (the core floats just above it).
"""
import argparse, math, random, sys, os
sys.path.insert(0, os.path.dirname(__file__))
import bpy
from mathutils import Vector
from artkit import *

P = argparse.ArgumentParser()
P.add_argument("--env", default="command_centre")
P.add_argument("--core", action="store_true")
P.add_argument("--out", default="chamber.png")
P.add_argument("--res", type=int, default=1080)
P.add_argument("--samples", type=int, default=64)
A = P.parse_args()
rnd = random.Random(7)

THEMES = {
    "command_centre": dict(accent="#20D6EA", second="#9275FF", floor="#0d1a28", wall="#0f2233", warm="#bfefff"),
    "observatory":    dict(accent="#9275FF", second="#20D6EA", floor="#120f26", wall="#191433", warm="#d9ccff"),
    "aether_forest":  dict(accent="#2FE3C8", second="#37E6A0", floor="#0a1f1e", wall="#0c2a27", warm="#c8fff2"),
    "titan_station":  dict(accent="#FF8A3D", second="#F5C85B", floor="#1d110c", wall="#2a1610", warm="#ffd9b0"),
}

def core_sprite():
    reset(A.res, A.res, A.samples, transparent=True, world_strength=0.4)
    glass = mat("crystal", "#1a8fae", 0.12, 0.2, "#1fbfd6", 0.35, transmission=0.25, coat=1.0)
    inner = mat("inner", "#ffffff", 0.2, 0, "#9ff3ff", 5.0)
    ring = mat("ring", "#20D6EA", 0.3, 0, "#20D6EA", 2.2)
    # Faceted bipyramid crystal (top + bottom cones), with a bright inner heart.
    prim("cone", (0, 0, 0.55), (0.42, 0.42, 0.55), material=glass, vertices=6, radius1=1, depth=2).rotation_euler[2] = 0.3
    prim("cone", (0, 0, -0.55), (0.42, 0.42, 0.55), (math.pi, 0, 0.3), material=glass, vertices=6, radius1=1, depth=2)
    prim("ico_sphere", (0, 0, 0), (0.16, 0.16, 0.28), material=inner, subdivisions=2)
    prim("torus", (0, 0, 0), (1, 1, 1), (math.radians(72), 0, 0.4), material=ring, major_radius=0.78, minor_radius=0.018, major_segments=64)
    prim("torus", (0, 0, 0), (1, 1, 1), (math.radians(-64), math.radians(20), 0), material=ring, major_radius=0.68, minor_radius=0.012, major_segments=64)
    for i in range(10):  # orbiting shards
        a = i / 10 * 2 * math.pi
        prim("ico_sphere", (math.cos(a) * 0.95, math.sin(a) * 0.3, math.sin(a * 2) * 0.25), (0.035,) * 3, material=ring, subdivisions=1)
    light("AREA", (2, -3, 3), 300, (0.6, 0.9, 1.0), rot=(math.radians(50), 0, math.radians(35)), size=3)
    light("AREA", (-2, 2, -2), 150, (0.6, 0.45, 1.0), rot=(math.radians(-130), 0, math.radians(-40)), size=3)
    camera((0, -4.2, 0.6), (0, 0, 0), ortho_scale=2.6)
    render_with_bloom(A.out, A.samples, A.res, strength=(0.4, 0.3))

def chamber(env):
    T = THEMES[env]
    reset(A.res, int(A.res * 16 / 9), A.samples, transparent=False, world_color=(0.004, 0.01, 0.02), world_strength=0.4)
    accent = mat("accent", T["accent"], 0.3, 0, T["accent"], 6.0)
    accent_soft = mat("accent_soft", T["accent"], 0.3, 0, T["accent"], 0.7)
    second = mat("second", T["second"], 0.3, 0, T["second"], 4.0)
    floor = mat("floor", T["floor"], 0.32, 0.6, coat=0.3)
    wall = mat("wall", T["wall"], 0.5, 0.5)
    metal = mat("metal", "#55657a", 0.3, 0.9)
    lamp = mat("lamp", T["warm"], 0.3, 0, T["warm"], 3.5)
    star = mat("star", "#ffffff", 0.5, 0, "#dfefff", 10.0)

    # Floor with radiating light seams.
    prim("plane", (0, 4, 0), (14, 14, 1), material=floor)
    for i in range(14):
        a = math.radians(-90 + (i - 6.5) * 9)
        L = 9
        o = prim("cube", (math.cos(a) * L / 2, 1 + math.sin(a) * L / 2 + 4.5, 0.005), (L / 2, 0.006, 0.004), (0, 0, a), material=accent_soft)
    for r in (1.6, 2.6, 3.8):
        prim("torus", (0, 0, 0.006), (1, 1, 0.05), material=accent_soft, major_radius=r, minor_radius=0.008, major_segments=96)

    # Back wall with a huge circular portal looking out to space (timer ring sits over it).
    prim("cube", (0, 7, 5), (12, 0.3, 7), material=wall)
    portal_z = 5.3
    prim("cylinder", (0, 6.66, portal_z), (3.25, 3.25, 0.02), (math.radians(90), 0, 0), material=mat("void", "#010307", 1.0), vertices=96)
    prim("torus", (0, 6.4, portal_z), (1, 1, 1.4), (math.radians(90), 0, 0), material=metal, major_radius=3.35, minor_radius=0.18, major_segments=96)
    prim("torus", (0, 6.2, portal_z), (1, 1, 1), (math.radians(90), 0, 0), material=accent, major_radius=3.12, minor_radius=0.025, major_segments=96)
    for i in range(260):  # stars inside the portal
        a = rnd.uniform(0, 2 * math.pi); r = 3.1 * math.sqrt(rnd.random())
        prim("ico_sphere", (math.cos(a) * r, 6.58, portal_z + math.sin(a) * r), (rnd.uniform(0.008, 0.026),) * 3, material=star, subdivisions=1)
    # Nebula inside the portal: procedural emission (noise × radial falloff, two-tone).
    neb = bpy.data.materials.new("nebula"); neb.use_nodes = True; nt = neb.node_tree
    for n in list(nt.nodes):
        if n.type != "OUTPUT_MATERIAL": nt.nodes.remove(n)
    em = nt.nodes.new("ShaderNodeEmission"); tc = nt.nodes.new("ShaderNodeTexCoord")
    nz = nt.nodes.new("ShaderNodeTexNoise"); nz.inputs["Scale"].default_value = 1.4; nz.inputs["Detail"].default_value = 8; nz.inputs["Roughness"].default_value = 0.62
    nz2 = nt.nodes.new("ShaderNodeTexNoise"); nz2.inputs["Scale"].default_value = 0.8; nz2.inputs["Detail"].default_value = 3
    grad = nt.nodes.new("ShaderNodeTexGradient"); grad.gradient_type = "SPHERICAL"
    mapn = nt.nodes.new("ShaderNodeMapping"); mapn.inputs["Scale"].default_value = (0.31, 0.31, 0.31)
    ramp = nt.nodes.new("ShaderNodeValToRGB"); cr = ramp.color_ramp
    cr.elements[0].position = 0.3; cr.elements[0].color = hexc(T["second"])
    cr.elements[1].position = 0.7; cr.elements[1].color = hexc(T["accent"])
    pw = nt.nodes.new("ShaderNodeMath"); pw.operation = "POWER"; pw.inputs[1].default_value = 3.2
    mul = nt.nodes.new("ShaderNodeMath"); mul.operation = "MULTIPLY"
    mul2 = nt.nodes.new("ShaderNodeMath"); mul2.operation = "MULTIPLY"; mul2.inputs[1].default_value = 1.5
    L = nt.links.new
    L(tc.outputs["Object"], nz.inputs["Vector"]); L(tc.outputs["Object"], nz2.inputs["Vector"])
    L(tc.outputs["Object"], mapn.inputs["Vector"]); L(mapn.outputs["Vector"], grad.inputs["Vector"])
    L(nz.outputs["Fac"], pw.inputs[0]); L(pw.outputs[0], mul.inputs[0]); L(grad.outputs["Fac"], mul.inputs[1])
    L(mul.outputs[0], mul2.inputs[0]); L(nz2.outputs["Fac"], ramp.inputs["Fac"])
    L(ramp.outputs["Color"], em.inputs["Color"]); L(mul2.outputs[0], em.inputs["Strength"])
    L(em.outputs[0], nt.nodes["Material Output"].inputs["Surface"])
    prim("cylinder", (0, 6.63, portal_z), (3.2, 3.2, 0.004), (math.radians(90), 0, 0), material=neb, vertices=96)

    # Side pillars with lanterns (blueprint chamber).
    for sx in (-1, 1):
        for depth, h in ((2.2, 4.2), (4.6, 5.6)):
            x = sx * (3.0 + depth * 0.25)
            prim("cylinder", (x, depth, h / 2), (0.28, 0.28, h / 2), material=metal, vertices=12)
            prim("cube", (x - sx * 0.3, depth - 0.05, 1.6), (0.11, 0.11, 0.2), material=lamp)
            prim("cube", (x - sx * 0.29, depth - 0.3, h - 0.4), (0.03, 0.03, 0.6), material=accent)
        light("POINT", (sx * 3.0, 1.9, 1.6), 25, hexc(T["warm"])[:3]).data.shadow_soft_size = 0.2

    # Pedestal where the energy core floats.
    prim("cylinder", (0, 0, 0.12), (1.05, 1.05, 0.12), material=metal, vertices=48)
    prim("cylinder", (0, 0, 0.32), (0.72, 0.72, 0.08), material=mat("ped", "#1c2b3c", 0.25, 0.8), vertices=48)
    prim("torus", (0, 0, 0.41), (1, 1, 0.6), material=accent, major_radius=0.62, minor_radius=0.03, major_segments=64)
    prim("cylinder", (0, 0, 0.415), (0.5, 0.5, 0.005), material=mat("pedglow", T["accent"], 0.3, 0, T["accent"], 0.35), vertices=48)
    light("POINT", (0, 0, 1.3), 45, hexc(T["accent"])[:3]).data.shadow_soft_size = 0.6

    # Theme props.
    if env == "aether_forest":
        leaf = mat("leaf", "#1f8a66", 0.8); trunk = mat("trunk", "#3e2c22", 0.9); glowleaf = mat("gl", "#1fb59b", 0.6, 0, "#2FE3C8", 0.6)
        for sx in (-1, 1):
            for k in range(3):
                x = sx * rnd.uniform(1.6, 2.6); y = rnd.uniform(0.5, 3)
                prim("cylinder", (x, y, 0.35), (0.05, 0.05, 0.35), material=trunk, vertices=6)
                prim("ico_sphere", (x, y, 0.85), (0.4,) * 3, material=leaf if k % 2 else glowleaf, subdivisions=1)
    if env == "titan_station":
        pipe = mat("pipe", "#6b4a3a", 0.35, 0.9)
        for k in range(4):
            prim("cylinder", (0, 6.6, 1.2 + k * 0.45), (0.12, 0.12, 6.5), (0, math.radians(90), 0), material=pipe, vertices=16)
    if env == "observatory":
        prim("torus", (0, 6.3, portal_z), (1, 1, 1), (math.radians(90), 0, 0), material=second, major_radius=2.2, minor_radius=0.012, major_segments=96)
        prim("torus", (0, 6.3, portal_z), (1, 1, 1), (math.radians(90), 0, 0), material=second, major_radius=1.2, minor_radius=0.01, major_segments=96)

    light("AREA", (0, -3, 7), 500, (0.75, 0.85, 1.0), rot=(math.radians(35), 0, 0), size=8)  # soft top fill
    camera((0, -8.6, 2.6), (0, 2.5, 2.9), lens=30)
    render_with_bloom(A.out, A.samples, A.res, strength=(0.45, 0.35))

if A.core: core_sprite()
else: chamber(A.env)
print("rendered", A.out)
