from lib import *

W, H = 1648, 772
s = Svg("api-server", W, H, "TechInsights API server request flow",
        "Request path from the client through Cloudflare and nginx, the Spring Security chain, the Aid interceptor and "
        "controllers in the api module, into domain services, repositories and PostgreSQL with pgvector, plus the "
        "fail-fast Gemini query-embedding path and ProblemDetail error handling.")

cx = lambda i: 48 + (i - 1) * 228
NW, NH = 184, 128
A, B, C = 88, 328, 520

s.zone(32, 48, 672, 184, "EDGE")
s.zone(716, 48, 900, 184, "MODULE · API")
s.zone(944, 284, 444, 380, "MODULE · DOMAIN")

# arrows (drawn before nodes)
mid = lambda y: y + NH // 2
for i in range(1, 6):
    s.arrow([(cx(i) + NW, mid(A)), (cx(i + 1), mid(A))])
s.arrow([(cx(6) + NW, mid(A)), (cx(7), mid(A))], dashed=True)
s.arrow([(cx(6) + 92, A + NH), (cx(6) + 92, B)])
s.arrow([(cx(6), mid(B)), (cx(5) + NW, mid(B))])
s.arrow([(cx(5), mid(B)), (cx(4) + NW, mid(B))])
s.arrow([(cx(6) + 92, B + NH), (cx(6) + 92, C)])
s.arrow([(cx(6), mid(C)), (cx(5) + NW, mid(C))], color="accent")
s.arrow([(cx(5), mid(C)), (cx(4) + NW, mid(C))], color="accent")
s.label(cx(6) - 22, mid(C) - 18, "MISS")

N = s.node
N(cx(1), A, NW, NH, "Client", ["web browser", "JWT + aid cookies"], "ext")
N(cx(2), A, NW, NH, "Cloudflare", ["TLS · proxy", "CF-Connecting-IP"], "ext")
N(cx(3), A, NW, NH, "nginx", ["auth/refresh: 10 r/m", "search: 30 r/m", "502/504 → 503 page"], "step")
N(cx(4), A, NW, NH, "Security chain", ["CorsFilter", "CsrfOriginFilter", "JwtAuthenticationFilter", "authorizeHttpRequests"], "focal")
N(cx(5), A, NW, NH, "Aid interceptor", ["/api/** only", "skips bots, OPTIONS", "ensures aid cookie"], "step")
N(cx(6), A, NW, NH, "Controllers", ["Post · Company · Search", "Recommend · Category", "Github* · Auth · User", "Bookmark · Like"], "step")
N(cx(7), A, NW, NH, "Exception handler", ["GlobalExceptionHandler", "problem+json body", "stable code field"], "async")
N(cx(6), B, NW, NH, "Domain services", ["Post · Company · Search", "Recommendation", "Bookmark · Like · User"], "step")
N(cx(5), B, NW, NH, "Repositories", ["Spring Data JPA", "QueryDSL", "native SQL"], "step")
N(cx(4), B, NW, NH, "PostgreSQL", ["pgvector", "HNSW halfvec(3072)", "Flyway migrate"], "store")
N(cx(6), C, NW, NH, "Embedding cache", ["Caffeine · 1h TTL", "key: normalized query"], "store")
N(cx(5), C, NW, NH, "Rate limiter", ["geminiQueryEmbedding", "timeout 0 → fail-fast"], "focal")
N(cx(4), C, NW, NH, "Gemini API", ["query embedding", "semantic search"], "ext")

# error mapping panel
s.rect(48, B, 640, 320, "#ffffff", "rgba(45,49,66,0.30)", 1, 6)
s.text(64, B + 28, "ERROR MAPPING", 12, MUTED, spacing="0.14em")
s.line(64, B + 44, 672, B + 44)
rows = [("Gemini limiter rejects", "429 TOO_MANY_REQUESTS"),
        ("Circuit breaker open", "503 SERVICE_UNAVAILABLE"),
        ("nginx auth / search limit", "429 TOO_MANY_REQUESTS"),
        ("Upstream 502 / 504", "503 maintenance body"),
        ("Foreign Origin / Referer", "403 CSRF_ORIGIN_NOT_ALLOWED"),
        ("Missing or invalid JWT", "401 via authentication entry point"),
        ("Domain exception", "404 / 400 ProblemDetail")]
for i, (a, b) in enumerate(rows):
    y = B + 76 + i * 32
    s.text(64, y, a, 12, INK)
    s.text(300, y, "→", 12, SOFT)
    s.text(328, y, b, 12, MUTED)

s.legend(B + 320 + 56, [("focal", "Focal path"), ("step", "Service"), ("store", "Store / cache"),
                         ("ext", "External"), ("async", "Error path"), ("arrow", "Call"), ("dash", "Throws")])
assert not s.check(), s.check()
