"""Builds every diagram: HTML source, standalone SVG, PNG (Chrome headless). Run from img/diagrams/src."""
import importlib, pathlib, re, subprocess, sys, tempfile
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from lib import FONTS

OUT = pathlib.Path(__file__).resolve().parent.parent
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
META = {
    "api": ("api-server", "Request flow", "API server"),
    "batch": ("batch-server", "Job topology", "Batch server"),
    "crawl": ("crawling-sequence", "Sequence", "One crawling run"),
    "erd": ("erd", "Final schema · Flyway B001 + V002–V032", "Entity relationships"),
}

def standalone(svg):
    svg = re.sub(r'(fill|stroke)="rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d*\.?\d+)\s*\)"',
                 lambda m: '{0}="#{1:02x}{2:02x}{3:02x}" {0}-opacity="{4}"'.format(
                     m.group(1), int(m.group(2)), int(m.group(3)), int(m.group(4)), m.group(5)), svg)
    svg = re.sub(r'(fill|stroke)="transparent"', r'\1="none"', svg)
    style = f"<style>@import url('{FONTS.replace('&', '&amp;')}');</style>"
    svg = svg.replace("<defs>", "<defs>" + style, 1)
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + svg

names = sys.argv[1:] or list(META)
for key in names:
    mod = importlib.import_module(key)
    s = mod.s
    slug, eyebrow, heading = META[key]
    (OUT / "src" / f"{slug}.html").write_text(s.html(eyebrow, heading))
    (OUT / f"{slug}.svg").write_text(standalone(s.svg()))
    page = pathlib.Path(tempfile.gettempdir()) / f"{slug}-shot.html"
    page.write_text(f'<html><head><link href="{FONTS}" rel="stylesheet"><style>body{{margin:0}}svg{{display:block}}</style></head><body>{s.svg()}</body></html>')
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=2",
                    f"--window-size={s.w},{s.h}", "--virtual-time-budget=8000",
                    f"--screenshot={OUT / (slug + '.png')}", f"file://{page}"], check=True, capture_output=True)
    print("built", slug, s.w, s.h)
