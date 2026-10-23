-- Gemini 일일(RPD) 쿼터 사용량을 JVM 메모리 대신 DB에 저장
CREATE TABLE IF NOT EXISTS gemini_daily_usage (
    usage_date    DATE        NOT NULL,
    quota_key     VARCHAR(64) NOT NULL,
    request_count INT         NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    CONSTRAINT gemini_daily_usage_pkey PRIMARY KEY (usage_date, quota_key)
);
