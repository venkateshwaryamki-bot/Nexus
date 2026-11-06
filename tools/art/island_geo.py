"""Shared Nexus Prime island geometry + camera, used by island_layers.py and trees.py (must stay identical)."""
import math
from mathutils import Vector, noise

R = 3.2
CAM_LOC = (0, -14, 5.2)
CAM_TARGET = (0, 0, -0.35)
ORTHO_SCALE = 9.4

# Building slots (x, y on the island top). Front arc + one back-left; never directly behind the tower.
SLOTS = {
    "reactor":   (-2.45, -0.3),
    "colony":    (-1.45, -1.8),
    "lab":       (0.15, -2.3),
    "comms":     (1.65, -1.6),
    "spaceport": (2.45, -0.05),
    "defense":   (-1.85, 1.25),
}

def rim_radius(th):
    return R * (1 + 0.10 * math.sin(3 * th + 0.7) + 0.06 * math.sin(5 * th + 2.1) + 0.04 * math.sin(9 * th))

def ground_z(x, y):
    r = math.hypot(x, y)
    return 0.18 * noise.noise(Vector((x * 0.45, y * 0.45, 1.3))) * min(1, r / 1.2) + 0.08 * (1 - min(1, r / R))

def surface(x, y):
    return Vector((x, y, ground_z(x, y)))

def tree_slots(count=12):
    """Deterministic grove positions: away from the tower plaza and every building slot, inside the rim.
    Ordered so the first trees are planted where they're most visible (front-left/right), then around."""
    cands = []
    for i in range(160):
        th = i * 2.39996 % (2 * math.pi)
        frac = 0.58 + 0.32 * ((i * 0.618) % 1)
        r = rim_radius(th) * frac
        x, y = r * math.cos(th), r * math.sin(th)
        if math.hypot(x, y) < 1.25: continue
        if any(math.hypot(x - sx, y - sy) < 0.85 for sx, sy in SLOTS.values()): continue
        cands.append((x, y))
    chosen = []
    for c in cands:
        if all(math.hypot(c[0] - o[0], c[1] - o[1]) > 0.62 for o in chosen):
            chosen.append(c)
        if len(chosen) == count: break
    # Plant order: nearest to the camera (most negative y) first.
    return sorted(chosen, key=lambda p: p[1])
