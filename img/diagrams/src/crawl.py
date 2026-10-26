from lib import *

W, H = 1648, 1096
s = Svg("crawling-sequence", W, H, "One crawlPostJob run",
        "Sequence of one crawling run: the scheduled workflow starts crawlPostJob, each company's feed URL passes the "
        "SSRF guard and a per-domain rate limiter before the feed is fetched and parsed, and new posts are inserted "
        "with ON CONFLICT (url) DO NOTHING.")

X = {"sch": 200, "step": 520, "guard": 840, "feed": 1160, "db": 1480}
AY, AH, AW = 40, 80, 240
BOTTOM = 900
FR_Y, FR_B = 336, 876

# frame + lifelines + activation bar (underneath arrows)
s.zones.append(f'<rect x="{X["step"] - 56}" y="{FR_Y}" width="{X["db"] - X["step"] + 112}" height="{FR_B - FR_Y}" rx="4" '
               f'fill="rgba(45,49,66,0.02)" stroke="rgba(45,49,66,0.22)" stroke-width="1"/>'
               f'<rect x="{X["step"] - 56}" y="{FR_Y}" width="56" height="20" rx="2" fill="{PAPER}" '
               f'stroke="rgba(45,49,66,0.22)" stroke-width="1"/>'
               f'<text x="{X["step"] - 28}" y="{FR_Y + 14}" fill="{MUTED}" font-size="12" font-family="{MONO}" '
               f'text-anchor="middle" letter-spacing="0.12em">LOOP</text>'
               f'<text x="{X["step"] + 20}" y="{FR_Y + 14}" fill="{MUTED}" font-size="12" font-family="{MONO}">'
               f'[for each company · parallel threads, 0–2s start jitter]</text>')
for k, x in X.items():
    s.zones.append(f'<line x1="{x}" y1="{AY + AH}" x2="{x}" y2="{BOTTOM}" stroke="rgba(45,49,66,0.20)" '
                   f'stroke-width="1" stroke-dasharray="3,3"/>')
s.zones.append(f'<rect x="{X["step"] - 4}" y="184" width="8" height="{FR_B - 184 + 8}" fill="rgba(45,49,66,0.06)" '
               f'stroke="{MUTED}" stroke-width="0.8"/>')

SR, SL = X["step"] + 4, X["step"] - 4

def msg(x1, x2, y, text, kind="call", color="muted"):
    dashed = kind == "return"
    s.arrow([(x1, y), (x2, y)], color=color, dashed=dashed)
    w = len(text) * CHAR_W + 12
    cx = x1 + 16 + w / 2 if x2 > x1 else x1 - 16 - w / 2
    s.label(round(cx / 2) * 2, y - 18, text)

msg(X["sch"], SL, 184, "cron 06:00 UTC · crawlPostJob")
msg(SR, X["db"], 240, "CompanyReader.findAll()")
msg(X["db"], SR, 296, "companies", "return")
msg(SR, X["guard"], 436, "isSafe(blogUrl): SSRF guard")
msg(X["guard"], SR, 492, "ok · else UnsafeUrlException", "return")
msg(SR, X["guard"], 548, "domain rate-limit permit")
msg(SR, X["feed"], 604, "GET feed")
msg(X["feed"], SR, 660, "RSS / Atom XML", "return")
s.arrow([(SR, 716), (SR + 44, 716), (SR + 44, 748), (SR, 748)])
s.text(SR + 56, 736, "parse entries · fetch content · skip blank · distinct url", 12, MUTED)
msg(SR, X["db"], 808, "INSERT … ON CONFLICT (url) DO NOTHING", color="accent")
msg(X["db"], SR, 852, "inserted count", "return")

N = s.node
N(X["sch"] - AW // 2, AY, AW, AH, "GitHub Actions", ["cron → SSM runner", "--spring.batch.job.name"], "ext")
N(X["step"] - AW // 2, AY, AW, AH, "crawlPostJob step", ["CompanyReader", "RawPostProcessor / Writer"], "step")
N(X["guard"] - AW // 2, AY, AW, AH, "Guards", ["UrlValidator", "DomainRateLimiterManager"], "step")
N(X["feed"] - AW // 2, AY, AW, AH, "Blog feed", ["company.blog_url", "external host"], "ext")
N(X["db"] - AW // 2, AY, AW, AH, "PostgreSQL", ["company · posts", "uq_posts_url"], "store")

s.chip(48, 920, 1552, 56, "", "async",
       lines=["Later, separate runs: optimizedSummaryJob (07:00 UTC) summarises posts with is_summary = false, "
              "then postVectorEmbeddingJob (08:00 UTC)", "embeds summarised posts into post_embedding."])
s.legend(1048, [("step", "Component"), ("ext", "External"), ("store", "Store"), ("arrow", "Call"), ("dash", "Return")])
assert not s.check(), s.check()
