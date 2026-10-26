from lib import *

W, H = 1648, 1188
s = Svg("batch-server", W, H, "TechInsights batch server jobs",
        "Spring Batch jobs of the batch module: how a workflow launches a job after a validate-only Flyway check, the "
        "reader, processor and writer of every job, their retry and skip policy, and the shared database-backed Gemini "
        "daily quota.")

# top band: launch path
TW, TH, TY = 232, 88, 56
tx = lambda i: 48 + i * 280
s.zone(32, 24, 1584, 152, "LAUNCH")
for i in range(3):
    s.arrow([(tx(i) + TW, TY + 56), (tx(i + 1), TY + 56)])
N = s.node
N(tx(0), TY + 12, TW, TH, "GitHub Actions", ["cron schedule, UTC", "one workflow per job"], "ext")
N(tx(1), TY + 12, TW, TH, "SSM runner", ["_batch-ssm-runner.yml", "--spring.batch.job.name"], "step")
N(tx(2), TY + 12, TW, TH, "Batch app", ["Spring Batch + Flyway", "RunIdIncrementer"], "step")
N(tx(3), TY + 12, TW, TH, "Flyway validate", ["validate-only at startup", "fails if DB behind/ahead"], "focal")

# job rows
ZY = 208
s.zone(32, ZY, 1584, 716, "JOBS · SCHEDULE ORDER")
cols = {"label": 64, "r": 376, "p": 660, "w": 992, "n": 1324}
widths = {"r": 252, "p": 300, "w": 300, "n": 252}
for k, t in (("r", "READER"), ("p", "PROCESSOR"), ("w", "WRITER"), ("n", "POLICY")):
    s.text(cols[k], ZY + 52, t, 12, SOFT, spacing="0.14em")
rows = [
    ("githubTrendingJob", "00:00 UTC", "GithubRepoReader", "GithubRepoProcessor", "GithubRepoWriter",
     ["chunk · skip ≤10", "on DataIntegrityViolation"], "step"),
    ("githubReadmeSummaryJob", "01:00 UTC", "UnsummarizedRepoReader", "GithubReadmeFetchProcessor",
     "GithubReadmeBatchSummaryWriter", ["Gemini summary", "quota: README_SUMMARY"], "step"),
    ("githubReadmeEmbeddingJob", "05:00 UTC", "UnembeddedRepoReader", "GithubReadmeEmbeddingProcessor",
     "GithubReadmeEmbeddingWriter", ["retry 3 · skip ≤10", "quota: EMBEDDING"], "step"),
    ("crawlPostJob", "06:00 UTC", "CompanyReader", "RawPostProcessor", "RawPostWriter",
     ["threaded · retry · skip ≤10", "UnsafeUrl: no retry"], "focal"),
    ("optimizedSummaryJob", "07:00 UTC · 2 steps", "BatchAwarePostReader", "AsyncBatchPostSummaryProcessor",
     "FlatteningPostWriter", ["quota: BATCH_SUMMARY", "step 2: post_count reconcile"], "step"),
    ("postVectorEmbeddingJob", "08:00 UTC", "BatchSummarizedPostReader", "BatchPostEmbeddingProcessor",
     "BatchPostEmbeddingWriter", ["retry 3 · skip ≤10", "quota: EMBEDDING"], "step"),
    ("communityCollectJob", "no workflow in repo", "CommunityCollectReader", "CommunityCollectProcessor",
     "CommunityCollectWriter", ["collect community posts", "per GitHub repository"], "step"),
    ("communityAnalyzeJob", "no workflow in repo", "CommunityAnalyzeReader", "CommunityAnalyzeProcessor",
     "CommunityAnalyzeWriter", ["Gemini sentiment", "quota: COMMUNITY_ANALYSIS"], "step"),
    ("refreshTokenCleanupJob", "no workflow in repo", None, "RefreshTokenCleanupTasklet", None,
     ["tasklet, single step", "expired + retired hashes"], "step"),
]
RY, PITCH, RH = ZY + 68, 72, 48
for i, (name, sched, rd, pr, wr, pol, kind) in enumerate(rows):
    y = RY + i * PITCH
    if rd:
        s.arrow([(cols["r"] + widths["r"], y + 24), (cols["p"], y + 24)])
        s.arrow([(cols["p"] + widths["p"], y + 24), (cols["w"], y + 24)])
for i, (name, sched, rd, pr, wr, pol, kind) in enumerate(rows):
    y = RY + i * PITCH
    s.text(cols["label"], y + 20, name, 16, INK, SANS, weight=600)
    s.text(cols["label"], y + 40, sched, 12, MUTED)
    if rd:
        s.chip(cols["r"], y, widths["r"], RH, rd)
        s.chip(cols["p"], y, widths["p"], RH, pr, kind)
        s.chip(cols["w"], y, widths["w"], RH, wr)
    else:
        s.chip(cols["r"], y, cols["w"] + widths["w"] - cols["r"], RH, pr)
    s.chip(cols["n"], y, widths["n"], RH, "", "async", lines=pol)
    if i < len(rows) - 1:
        s.line(64, y + PITCH - 12, 1584, y + PITCH - 12, "rgba(45,49,66,0.08)")

# shared infrastructure
SY = 952
s.zone(32, SY, 1584, 156, "SHARED")
s.arrow([(48 + 3 * 280 + 116, TY + 12 + TH), (48 + 3 * 280 + 116, ZY)], color="accent")
s.arrow([(tx(0) + TW, SY + 88), (tx(1), SY + 88)])
N(tx(0), SY + 40, TW, 96, "Daily quota", ["gemini_daily_usage upsert", "WHERE count < limit", "RETURNING → reserved"], "focal")
N(tx(1), SY + 40, TW, 96, "PostgreSQL", ["posts · github_* tables", "Spring Batch metadata", "ON CONFLICT writes"], "store")
N(tx(2), SY + 40, TW, 96, "Rate limiters", ["Resilience4j per domain", "Gemini limiters", "circuit breakers"], "step")
N(tx(3), SY + 40, TW, 96, "Gemini API", ["summary · embedding", "sentiment analysis"], "ext")

s.legend(1156, [("focal", "Focal"), ("step", "Step"), ("store", "Store"), ("ext", "External"),
                 ("async", "Policy note"), ("arrow", "Data / control flow")])
assert not s.check(), s.check()
