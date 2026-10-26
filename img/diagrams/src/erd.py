from lib import *

W, H = 1824, 1216
s = Svg("erd", W, H, "TechInsights final database schema",
        "Entity relationship diagram of the final PostgreSQL schema after Flyway B001 and V002 to V032: content tables "
        "around posts, users and refresh-token families, and the GitHub trending tables, with primary keys, foreign "
        "keys and the important unique and partial indexes.")

RH = 16
def table(x, y, w, name, rows, kind="step"):
    h = 36 + len(rows) * RH + 12
    s.boxes.append((x, y, w, h))
    fill, stroke, dash = {"step": ("#ffffff", INK, ""), "async": ("rgba(45,49,66,0.02)", "rgba(45,49,66,0.30)", ' stroke-dasharray="4,3"'),
                          "focal": ("rgba(235,108,54,0.08)", ACCENT, "")}[kind]
    out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{PAPER}"/>',
           f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="{fill}" stroke="{stroke}" stroke-width="1"{dash}/>',
           f'<text x="{x + 12}" y="{y + 24}" fill="{INK}" font-size="{16 if len(name) < 23 else 12}" font-weight="600" font-family="{SANS}">{escape(name)}</text>',
           f'<line x1="{x}" y1="{y + 32}" x2="{x + w}" y2="{y + 32}" stroke="rgba(45,49,66,0.12)" stroke-width="0.8"/>']
    for i, r in enumerate(rows):
        tag, rest = r[:2], r[3:]
        ty = y + 52 + i * RH
        col = {"PK": INK, "FK": LINK, "UQ": ACCENT, "ix": SOFT}.get(tag)
        if col:
            out.append(f'<text x="{x + 12}" y="{ty}" fill="{col}" font-size="12" font-weight="600" font-family="{MONO}">{tag}</text>')
        out.append(f'<text x="{x + 12 + 3 * CHAR_W:g}" y="{ty}" fill="{MUTED}" font-size="12" font-family="{MONO}">{escape(rest)}</text>')
    s.nodes.append("".join(out))
    return h

def rel(pts, dashed=False):
    s.arrow(pts, dashed=dashed)
    def put(p, q, ch, toward):
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = abs(dx) + abs(dy)
        dx, dy = dx / n, dy / n
        px, py = p[0] + dx * 14 - dy * 10, p[1] + dy * 14 + dx * 10
        s.text(round(px), round(py + 4), ch, 12, SOFT, anchor="middle")
    put(pts[0], pts[1], "N", False)
    put(pts[-1], pts[-2], "1", True)

# zones
s.zone(32, 48, 896, 816, "CONTENT")
s.zone(960, 48, 368, 784, "USERS · AUTH")
s.zone(1360, 48, 432, 1040, "GITHUB TRENDING")

CA, CB, CC = 48, 312, 648
# --- content ---
table(CA, 88, 232, "company", ["PK id", "UQ company_name", "   blog_url", "   rss_supported", "   post_count",
                               "   total_view_count", "   deleted_at (soft delete)"])
table(CB, 88, 264, "posts", ["PK id", "FK company_id → company", "UQ url  (uq_posts_url)", "   title · preview · content",
                             "   published_at · thumbnail", "   is_summary · is_embedding", "   view_count · like_count",
                             "   summary_failure_count", "   created_at · updated_at", "   deleted_at (soft delete)",
                             "ix partial: is_summary cursors", "ix partial: not embedded", "ix partial: popular summaries",
                             "ix partial: failure retry", "ix gin trgm: title · content", "ix company_id · deleted_at"], "focal")
table(CC, 88, 264, "post_categories", ["PK id", "FK post_id → posts", "UQ post_id, category"])
table(CC, 200, 264, "post_embedding", ["PK post_id · logical 1:1", "   embedding_vector", "ix hnsw halfvec(3072) cosine"], "step")
table(CC, 312, 264, "post_view", ["PK id", "FK post_id → posts", "UQ post, user_or_ip, date"])
table(CB, 440, 264, "post_likes", ["PK id", "FK post_id → posts", "FK user_id → users (nullable)", "   anonymous_id (nullable)",
                                   "UQ partial: post, user_id", "UQ partial: post, anonymous_id"])
table(CB, 632, 264, "post_bookmarks", ["PK id", "FK post_id → posts · CASCADE", "FK user_id → users · CASCADE", "UQ post_id, user_id"])
# peripheral, name-only
s.text(CA, 292, "PERIPHERAL · KEY COLUMNS ONLY", 12, SOFT, spacing="0.1em")
per = [("post_summary_failures", ["FK post_id → posts · CASCADE", "   error_type · failed_at"]),
       ("summary_retry_queue", ["PK post_id (no FK)", "   next_retry_at · retry_count"]),
       ("anonymous_user_read_history", ["   anonymous_id · post_id", "   read_at"]),
       ("gemini_daily_usage", ["PK usage_date, quota_key", "   request_count"]),
       ("batch_* (Spring Batch)", ["   batch_job_*, batch_step_*", "   framework metadata"])]
for i, (n, r) in enumerate(per):
    table(CA, 312 + i * 112, 232, n, r, "async")

# --- users ---
UX, UW = 984, 328
table(UX, 88, UW, "user_read_history", ["PK id", "FK user_id → users · CASCADE", "   post_id (no FK)", "UQ user_id, post_id"])
table(UX, 248, UW, "users", ["PK id", "UQ email", "UQ provider, provider_id", "UQ lower(nickname) · not deleted",
                             "   role · status · provider", "   last_login_at · marketing_agreed", "   deleted_at (soft delete)"], "focal")
table(UX, 456, UW, "refresh_tokens", ["PK id", "FK user_id → users", "UQ family_id", "UQ token_hash",
                                      "   device_id · expiry_at", "ix user_id, device_id", "ix expiry_at"])
table(UX, 664, UW, "refresh_token_retired_hashes", ["PK token_hash", "FK family_id → refresh_tokens", "   ON DELETE CASCADE",
                                                    "   successor_ciphertext · retired_at", "ix retired_at · family_id"])

# --- github ---
GX, GW = 1408, 352
table(GX, 248, GW, "github_bookmarks", ["PK id", "FK repo_id → repositories · CASCADE", "FK user_id → users · CASCADE", "UQ repo_id, user_id"])
table(GX, 408, GW, "github_repositories", ["PK id", "UQ full_name", "   repo_name · owner_name · html_url",
                                           "   description · topics · language", "   star_count · fork_count",
                                           "   daily / weekly star delta", "   pushed_at · fetched_at",
                                           "   deleted_at (soft delete)", "ix partial: star_count · deltas",
                                           "ix partial: language + star/delta"], "focal")
table(GX, 664, GW, "github_repository_readme", ["PK repo_id (FK → repositories)", "   readme_summary · embedding", "ix hnsw halfvec(3072) cosine"])
table(GX, 808, GW, "github_repository_community", ["PK repo_id (FK → repositories)", "   status · counts · jsonb insights", "ix partial: PENDING · retry"])
table(GX, 952, GW, "community_posts", ["PK id", "FK repo_id → repositories", "UQ repo_id, url", "ix partial: unanalyzed"])

# --- relations ---
rel([(CB, 120), (CA + 232, 120)])
rel([(CC, 136), (CB + 264, 136)])
rel([(CC, 248), (CB + 264, 248)], dashed=True)
rel([(CC, 360), (CB + 264, 360)])
rel([(444, 440), (444, 392)])
rel([(CB, 688), (292, 688), (292, 376), (CB, 376)])
rel([(CB + 264, 484), (936, 484), (936, 300), (UX, 300)])
rel([(CB + 264, 688), (948, 688), (948, 348), (UX, 348)])
rel([(1148, 200), (1148, 248)])
rel([(1148, 456), (1148, 408)])
rel([(1148, 664), (1148, 616)])
rel([(GX, 304), (UX + UW, 304)])
rel([(1584, 360), (1584, 408)])
rel([(1584, 664), (1584, 616)])
rel([(GX, 856), (1384, 856), (1384, 456), (GX, 456)])
rel([(GX + GW, 1008), (1776, 1008), (1776, 520), (GX + GW, 520)])

s.text(48, 1132, "Key columns, constraints and indexes only. Plain non-key columns are summarised on one line; "
                 "every table also carries created_at / updated_at.", 12, MUTED)
s.legend(1180, [("step", "Table"), ("focal", "Core aggregate"), ("async", "Peripheral (name-only)"),
                ("arrow", "FK  N → 1"), ("dash", "Logical link, no FK")])
assert not s.check(), s.check()
