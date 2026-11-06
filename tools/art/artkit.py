"""Shared helpers for NEXUS Blender art scripts (materials, primitives, render + emission bloom)."""
import os
import bpy
from mathutils import Vector

def reset(res_x, res_y, samples, transparent=True, world_color=(0.012, 0.03, 0.07), world_strength=0.9):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"; sc.cycles.device = "CPU"; sc.cycles.samples = samples; sc.cycles.use_denoising = True
    sc.render.film_transparent = transparent
    sc.render.resolution_x, sc.render.resolution_y = res_x, res_y
    sc.view_settings.view_transform = "AgX"; sc.view_settings.look = "AgX - Medium High Contrast"
    sc.render.image_settings.file_format = "PNG"; sc.render.image_settings.color_mode = "RGBA"
    w = bpy.data.worlds.new("w"); sc.world = w; w.use_nodes = True
    bg = w.node_tree.nodes["Background"]; bg.inputs[0].default_value = (*world_color, 1); bg.inputs[1].default_value = world_strength
    return sc

def hexc(h, a=1.0):
    h = h.lstrip("#"); r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    lin = lambda c: c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (lin(r), lin(g), lin(b), a)

def mat(name, color, rough=0.6, metal=0.0, emit=None, strength=0.0, transmission=0.0, coat=0.0):
    m = bpy.data.materials.new(name); m.use_nodes = True
    b = m.node_tree.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = hexc(color)
    b.inputs["Roughness"].default_value = rough; b.inputs["Metallic"].default_value = metal
    if emit:
        b.inputs["Emission Color"].default_value = hexc(emit); b.inputs["Emission Strength"].default_value = strength
    if transmission: b.inputs["Transmission Weight"].default_value = transmission
    if coat: b.inputs["Coat Weight"].default_value = coat
    return m

def prim(kind, loc, scale=(1, 1, 1), rot=(0, 0, 0), material=None, **kw):
    getattr(bpy.ops.mesh, "primitive_%s_add" % kind)(location=loc, rotation=rot, **kw)
    o = bpy.context.object; o.scale = scale
    if material: o.data.materials.append(material)
    return o

def light(kind, loc, energy, color, rot=None, size=1.0):
    sc = bpy.context.scene
    d = bpy.data.lights.new(kind, kind); d.energy = energy; d.color = color
    if kind == "AREA": d.size = size
    o = bpy.data.objects.new(kind, d); sc.collection.objects.link(o); o.location = loc
    if rot: o.rotation_euler = rot
    return o

def camera(loc, target, lens=35, ortho_scale=None):
    sc = bpy.context.scene
    d = bpy.data.cameras.new("cam")
    if ortho_scale: d.type = "ORTHO"; d.ortho_scale = ortho_scale
    else: d.lens = lens
    o = bpy.data.objects.new("cam", d); sc.collection.objects.link(o); sc.camera = o
    o.location = loc; o.rotation_mode = "QUATERNION"
    o.rotation_quaternion = (Vector(target) - Vector(loc)).to_track_quat("-Z", "Y")
    return o

def render_with_bloom(out, samples, res, strength=(0.5, 0.35)):
    """Main render, then an emission-only render (non-emissive surfaces → matte black) used as the bloom source."""
    import numpy as np
    from PIL import Image, ImageFilter
    sc = bpy.context.scene
    sc.render.filepath = out; bpy.ops.render.render(write_still=True)
    emit_out = out.replace(".png", "_emit.png")
    black = bpy.data.materials.new("black"); black.use_nodes = True
    bb = black.node_tree.nodes["Principled BSDF"]; bb.inputs["Base Color"].default_value = (0, 0, 0, 1)
    bb.inputs["Roughness"].default_value = 1.0; bb.inputs["Specular IOR Level"].default_value = 0.0
    def emits(m):
        b = m.node_tree.nodes.get("Principled BSDF") if m and m.use_nodes else None
        if b is None: return any(n.type == "EMISSION" for n in m.node_tree.nodes) if m and m.use_nodes else False
        return b.inputs["Emission Strength"].default_value > 0
    for o in sc.objects:
        if o.type == "MESH":
            for i, m in enumerate(o.data.materials):
                if not emits(m): o.data.materials[i] = black
        if o.type == "LIGHT": o.data.energy = 0
    sc.world.node_tree.nodes["Background"].inputs[1].default_value = 0
    sc.cycles.samples = max(8, samples // 4); sc.render.filepath = emit_out
    bpy.ops.render.render(write_still=True)
    im = np.asarray(Image.open(out).convert("RGBA")).astype(np.float32) / 255
    rgb, al = im[..., :3], im[..., 3:4]
    e = np.asarray(Image.open(emit_out).convert("RGBA")).astype(np.float32) / 255
    br = Image.fromarray((np.clip(e[..., :3] * e[..., 3:4], 0, 1) * 255).astype(np.uint8))
    g1 = np.asarray(br.filter(ImageFilter.GaussianBlur(res / 120))).astype(np.float32) / 255
    g2 = np.asarray(br.filter(ImageFilter.GaussianBlur(res / 40))).astype(np.float32) / 255
    glow = g1 * strength[0] + g2 * strength[1]
    out_rgb = rgb * al + glow
    out_a = np.clip(np.maximum(al, glow.max(axis=2, keepdims=True)), 0, 1)
    straight = np.where(out_a > 1e-4, out_rgb / np.maximum(out_a, 1e-4), 0)
    Image.fromarray((np.concatenate([np.clip(straight, 0, 1), out_a], axis=2) * 255).astype(np.uint8)).save(out)
    os.remove(emit_out)
