"""Draws orthographic + isometric sketches of the station from the generated model geometry.
python tools/sketch_station.py <out_dir>   (then screenshot the html files with a browser)"""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from generate_station_assets import part_elements

def boxes(futuristic, base_col, podium_col, glass_col):
    out = []
    cols = {"base": base_col, "podium": podium_col, "glass": glass_col}
    # cell origins (x, y, z) for facing north: front at z 0..16, back at z 16..32
    cells = {"front_low": (0, 0, 0), "back_low": (0, 0, 16), "front_high": (0, 16, 0), "back_high": (0, 16, 16)}
    for part, (ox, oy, oz) in cells.items():
        for e in part_elements(part, futuristic):
            tex = e["faces"]["north"]["texture"][1:]
            f, t = e["from"], e["to"]
            out.append(((f[0] + ox, f[1] + oy, f[2] + oz), (t[0] + ox, t[1] + oy, t[2] + oz), cols[tex], tex == "glass"))
    # workstation (crafting table look) in the front low cell
    out.append(((3, 1, 3), (13, 13, 13), "#8b5a2b", False))
    out.append(((3, 13, 3), (13, 14, 13), "#c89a5b", False))
    # NPC (scaled 0.944 to fit under the roof), feet on podium at y=2, centre of back cell
    s, cx, cz, y0 = 0.944, 8, 24, 2
    def part(x1, x2, y1, y2, z1, z2, c):
        out.append(((cx + x1 * s, y0 + y1 * s, cz + z1 * s), (cx + x2 * s, y0 + y2 * s, cz + z2 * s), c, False))
    part(-4, 0, 0, 11.25, -2, 2, "#3a3a8c"); part(0, 4, 0, 11.25, -2, 2, "#3a3a8c")
    part(-4, 4, 11.25, 22.5, -2, 2, "#3aa0a0")
    part(-8, -4, 11.25, 22.5, -2, 2, "#3aa0a0"); part(4, 8, 11.25, 22.5, -2, 2, "#3aa0a0")
    part(-4, 4, 22.5, 30, -4, 4, "#e0b090")
    return out

def faces(box):
    (x1, y1, z1), (x2, y2, z2), col, glass = box
    return [((0, 0, -1), [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)]),
            ((0, 0, 1), [(x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)]),
            ((-1, 0, 0), [(x1, y1, z1), (x1, y1, z2), (x1, y2, z2), (x1, y2, z1)]),
            ((1, 0, 0), [(x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1)]),
            ((0, 1, 0), [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)]),
            ((0, -1, 0), [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)])]

def norm(v):
    l = math.sqrt(sum(a * a for a in v)); return tuple(a / l for a in v)
def cross(a, b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def dot(a, b): return sum(x * y for x, y in zip(a, b))

def render(bx, fwd, up, scale=6, w=330, h=330, title=""):
    fwd = norm(fwd); right = norm(cross(fwd, up)); upv = cross(right, fwd)
    center = (16, 16, 16)
    polys = []
    for b in bx:
        for n, pts in faces(b):
            if not b[3] and dot(n, fwd) >= 0:
                continue
            if b[3] and dot(n, fwd) >= 0 and False:
                continue
            c = tuple(sum(p[i] for p in pts) / 4 for i in range(3))
            sp = [((dot(tuple(p[i]-center[i] for i in range(3)), right)) * scale + w / 2,
                   h / 2 - dot(tuple(p[i]-center[i] for i in range(3)), upv) * scale) for p in pts]
            shade = 1.0 - 0.18 * abs(n[0]) - 0.1 * (1 if n[1] < 0 else 0) if not b[3] else 1.0
            polys.append((dot(c, fwd), sp, b[2], b[3], shade))
    polys.sort(key=lambda p: -p[0])
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h+24}"><rect width="100%" height="100%" fill="#f6f4ee"/>']
    out.append(f'<text x="{w/2}" y="{h+16}" text-anchor="middle" font-family="sans-serif" font-size="14" fill="#333">{title}</text>')
    for d, sp, col, glass, shade in polys:
        pts = " ".join(f"{x:.1f},{y:.1f}" for x, y in sp)
        if glass:
            out.append(f'<polygon points="{pts}" fill="{col}" fill-opacity="0.22" stroke="#4a7fa8" stroke-opacity="0.25" stroke-width="0.4"/>')
        else:
            r, g, bl = int(col[1:3], 16), int(col[3:5], 16), int(col[5:7], 16)
            fill = f"rgb({int(r*shade)},{int(g*shade)},{int(bl*shade)})"
            out.append(f'<polygon points="{pts}" fill="{fill}" stroke="#222" stroke-width="0.5"/>')
    out.append("</svg>")
    return "".join(out)

def page(name, bx):
    views = [("Vorderansicht (Spieler-Seite)", (0, 0, 1), (0, 1, 0)), ("Rückansicht", (0, 0, -1), (0, 1, 0)),
             ("Seite links (West)", (1, 0, 0), (0, 1, 0)), ("Seite rechts (Ost)", (-1, 0, 0), (0, 1, 0)),
             ("Draufsicht (oben = Front)", (0, -1, 0), (0, 0, -1)),
             ("Isometrisch von vorne links", (1, -0.8, 1), (0, 1, 0)),
             ("Isometrisch von hinten rechts", (-1, -0.8, -1), (0, 1, 0)),
             ("Von vorne unten (Blick durchs Fenster)", (0.3, 0.25, 1), (0, 1, 0))]
    cells = "".join(f'<div>{render(bx, f, u, scale=(4.4 if "Isometrisch" in t or "unten" in t else 6), title=t)}</div>' for t, f, u in views)
    return f'<html><body style="margin:0;background:#f6f4ee;font-family:sans-serif"><h3 style="margin:8px 12px">{name}</h3><div style="display:grid;grid-template-columns:repeat(4,330px);gap:4px;padding:4px">{cells}</div></body></html>'

out = sys.argv[1] if len(sys.argv) > 1 else "."
os.makedirs(out, exist_ok=True)
open(os.path.join(out, "futuristic.html"), "w", encoding="utf-8").write(
    page("Futuristische Station (Stasisfeld, Eisenrahmen)", boxes(True, "#c9ced4", "#b0b4b8", "#78bfff")))
open(os.path.join(out, "medieval.html"), "w", encoding="utf-8").write(
    page("Mittelalterliche Station (Eiche, klares Glas)", boxes(False, "#b8945f", "#9a9a9a", "#cfe6ee")))
