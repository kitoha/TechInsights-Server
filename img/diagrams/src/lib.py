"""Shared drawing helpers for the TechInsights diagrams (diagram-design default skin, 4px grid)."""
from html import escape

PAPER = "#f5f5f5"
INK = "#2d3142"
MUTED = "#4f5d75"
SOFT = "#7a8399"
ACCENT = "#eb6c36"
LINK = "#2e5aa8"
SANS = "'Geist', 'Noto Sans KR', sans-serif"
MONO = "'Geist Mono', ui-monospace, monospace"
SERIF = "'Instrument Serif', serif"
FONTS = ("https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600"
         "&family=Geist+Mono:wght@400;500;600&display=swap")

CHAR_W = 7.4  # approx advance of Geist Mono at 12px


class Svg:
    def __init__(self, slug, w, h, title, desc):
        self.slug, self.w, self.h, self.title, self.desc = slug, w, h, title, desc
        self.zones, self.arrows, self.nodes, self.labels, self.extra = [], [], [], [], []
        self.boxes = []  # (x, y, w, h) of nodes, for the label/node overlap check
        self.masks = []  # (x, y, w, h) of label masks

    # ---- primitives -------------------------------------------------
    def zone(self, x, y, w, h, label, accent=False):
        stroke = "rgba(235,108,54,0.50)" if accent else "rgba(45,49,66,0.14)"
        fill = "rgba(235,108,54,0.04)" if accent else "rgba(45,49,66,0.025)"
        dash = ' stroke-dasharray="4,4"' if accent else ""
        lw = len(label) * 9 + 16
        self.zones.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="{fill}" stroke="{stroke}" stroke-width="1"{dash}/>'
            f'<rect x="{x + 12}" y="{y + 8}" width="{lw}" height="16" rx="2" fill="{PAPER}"/>'
            f'<text x="{x + 20}" y="{y + 20}" fill="{SOFT}" font-size="12" font-family="{MONO}" '
            f'letter-spacing="0.12em">{escape(label)}</text>')

    def node(self, x, y, w, h, name, subs=(), kind="step", tag=None):
        fill, stroke, dash, sw = {
            "step": ("#ffffff", INK, "", 1),
            "focal": ("rgba(235,108,54,0.08)", ACCENT, "", 1.4),
            "store": ("rgba(45,49,66,0.05)", MUTED, "", 1),
            "ext": ("rgba(45,49,66,0.03)", "rgba(45,49,66,0.40)", "", 1),
            "async": ("rgba(45,49,66,0.02)", "rgba(45,49,66,0.30)", ' stroke-dasharray="4,3"', 1),
        }[kind]
        self.boxes.append((x, y, w, h))
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{PAPER}"/>',
               f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}" '
               f'stroke-width="{sw}"{dash}/>']
        ty = y + 28
        if tag:
            out.append(f'<text x="{x + w - 8}" y="{y + 16}" fill="{SOFT}" font-size="8" font-family="{MONO}" '
                       f'text-anchor="end" letter-spacing="0.14em">{escape(tag)}</text>')
        out.append(f'<text x="{x + 12}" y="{ty}" fill="{INK}" font-size="16" font-weight="600" '
                   f'font-family="{SANS}">{escape(name)}</text>')
        for i, s in enumerate(subs):
            out.append(f'<text x="{x + 12}" y="{ty + 24 + 16 * i}" fill="{MUTED}" font-size="12" '
                       f'font-family="{MONO}">{escape(s)}</text>')
        self.nodes.append("".join(out))

    def chip(self, x, y, w, h, s, kind="step", lines=None):
        fill, stroke = {"step": ("#ffffff", INK), "focal": ("rgba(235,108,54,0.08)", ACCENT),
                        "store": ("rgba(45,49,66,0.05)", MUTED), "async": ("rgba(45,49,66,0.02)", "rgba(45,49,66,0.30)")}[kind]
        self.boxes.append((x, y, w, h))
        out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{PAPER}"/>',
               f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="1"/>']
        ls = lines or [s]
        base = y + h / 2 - (len(ls) - 1) * 9 + 4
        for i, l in enumerate(ls):
            out.append(f'<text x="{x + 12}" y="{base + i * 18:g}" fill="{INK if i == 0 else MUTED}" font-size="12" '
                       f'font-family="{MONO}">{escape(l)}</text>')
        self.nodes.append("".join(out))

    def text(self, x, y, s, size=12, fill=MUTED, family=MONO, anchor="start", weight=400, spacing=None):
        sp = f' letter-spacing="{spacing}"' if spacing else ""
        self.extra.append(f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" font-weight="{weight}" '
                          f'font-family="{family}" text-anchor="{anchor}"{sp}>{escape(s)}</text>')

    def rect(self, x, y, w, h, fill="none", stroke=INK, sw=1, rx=6, dash=None, opacity=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.extra.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
                          f'stroke="{stroke}" stroke-width="{sw}"{d}/>')

    def line(self, x1, y1, x2, y2, stroke="rgba(45,49,66,0.12)", sw=0.8, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        self.extra.append(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"{d}/>')

    def arrow(self, pts, color="muted", dashed=False, r=8, marker=True):
        """Orthogonal rounded polyline through pts; every segment must be axis-aligned."""
        for (ax, ay), (bx, by) in zip(pts, pts[1:]):
            assert ax == bx or ay == by, f"diagonal segment {pts}"
        d = f"M {pts[0][0]},{pts[0][1]}"
        for i in range(1, len(pts) - 1):
            (px, py), (cx, cy), (nx, ny) = pts[i - 1], pts[i], pts[i + 1]
            def toward(ax, ay, bx, by):
                dx, dy = bx - ax, by - ay
                n = abs(dx) + abs(dy)
                return (ax + dx / n * r, ay + dy / n * r)
            s = toward(cx, cy, px, py)
            e = toward(cx, cy, nx, ny)
            d += f" L {s[0]:g},{s[1]:g} Q {cx},{cy} {e[0]:g},{e[1]:g}"
        d += f" L {pts[-1][0]},{pts[-1][1]}"
        stroke = {"muted": MUTED, "accent": ACCENT, "link": LINK}[color]
        mk = {"muted": "arrow", "accent": "arrow-accent", "link": "arrow-link"}[color]
        da = ' stroke-dasharray="5,4"' if dashed else ""
        m = f' marker-end="url(#{self.slug}-{mk})"' if marker else ""
        self.arrows.append(f'<path d="{d}" fill="none" stroke="{stroke}" stroke-width="1.2"{da}{m}/>')

    def label(self, cx, cy, s, color=SOFT):
        w = int(len(s) * CHAR_W + 12)
        w += w % 2
        x, y = int(cx - w / 2), int(cy - 10)
        self.masks.append((x, y, w, 20))
        self.labels.append(f'<rect x="{x}" y="{y}" width="{w}" height="20" rx="2" fill="{PAPER}"/>'
                           f'<text x="{cx:g}" y="{cy + 4:g}" fill="{color}" font-size="12" font-family="{MONO}" '
                           f'text-anchor="middle" letter-spacing="0.04em">{escape(s)}</text>')

    def legend(self, y, items):
        self.line(40, y - 16, self.w - 40, y - 16, "rgba(45,49,66,0.10)")
        self.text(40, y + 8, "LEGEND", 12, MUTED, spacing="0.14em")
        x = 140
        for kind, name in items:
            if kind == "dash":
                self.line(x, y + 4, x + 32, y + 4, MUTED, 1.2, "5,4")
                self.text(x + 40, y + 8, name, 12)
                x += 40 + int(len(name) * CHAR_W) + 32
                continue
            if kind == "arrow":
                self.line(x, y + 4, x + 32, y + 4, MUTED, 1.2)
                self.text(x + 40, y + 8, name, 12)
                x += 40 + int(len(name) * CHAR_W) + 32
                continue
            fill, stroke, dash = {
                "step": ("#ffffff", INK, None),
                "focal": ("rgba(235,108,54,0.08)", ACCENT, None),
                "store": ("rgba(45,49,66,0.05)", MUTED, None),
                "ext": ("rgba(45,49,66,0.03)", "rgba(45,49,66,0.40)", None),
                "async": ("rgba(45,49,66,0.02)", "rgba(45,49,66,0.30)", "4,3"),
            }[kind]
            self.rect(x, y - 4, 24, 16, fill, stroke, 1, 4, dash)
            self.text(x + 32, y + 8, name, 12)
            x += 32 + int(len(name) * CHAR_W) + 32

    # ---- output -----------------------------------------------------
    def check(self):
        bad = []
        for (mx, my, mw, mh) in self.masks:
            for (nx, ny, nw, nh) in self.boxes:
                if mx < nx + nw and mx + mw > nx and my < ny + nh and my + mh > ny:
                    inside = mx >= nx and mx + mw <= nx + nw and my >= ny and my + mh <= ny + nh
                    if not inside:
                        bad.append((self.slug, (mx, my, mw, mh), (nx, ny, nw, nh)))
        return bad

    def svg(self):
        s = self.slug
        mk = lambda n, c: (f'<marker id="{s}-{n}" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">'
                           f'<polygon points="0 0, 8 3, 0 6" fill="{c}"/></marker>')
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {self.w} {self.h}" width="{self.w}" '
                f'height="{self.h}" role="img" aria-labelledby="{s}-title {s}-desc">'
                f'<title id="{s}-title">{escape(self.title)}</title><desc id="{s}-desc">{escape(self.desc)}</desc>'
                f'<defs>{mk("arrow", MUTED)}{mk("arrow-accent", ACCENT)}{mk("arrow-link", LINK)}</defs>'
                f'<rect width="100%" height="100%" fill="{PAPER}"/>'
                + "".join(self.zones) + "".join(self.arrows) + "".join(self.labels)
                + "".join(self.nodes) + "".join(self.extra) + '</svg>')

    def html(self, eyebrow, heading):
        return f'''<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{escape(heading)}</title>
<link href="{FONTS}" rel="stylesheet">
<style>*{{box-sizing:border-box;margin:0;padding:0}}body{{background:{PAPER};color:{INK};font-family:{SANS};padding:2rem}}
.eyebrow{{font-family:{MONO};font-size:.66rem;letter-spacing:.18em;text-transform:uppercase;color:{MUTED};margin-bottom:.5rem}}
h1{{font-family:{SERIF};font-weight:400;font-size:1.75rem;margin-bottom:1.5rem}}svg{{max-width:100%;height:auto;display:block}}</style></head>
<body><p class="eyebrow">{escape(eyebrow)}</p><h1>{escape(heading)}</h1>
{self.svg()}
</body></html>'''
