-- Daily AI request counters, used to cap LLM spend per user and for the whole app.
CREATE TABLE ai_usage (
    user_id        BIGINT  NOT NULL REFERENCES users (id),
    usage_date     DATE    NOT NULL,
    request_count  INTEGER NOT NULL,
    PRIMARY KEY (user_id, usage_date)
);

CREATE INDEX idx_ai_usage_date ON ai_usage (usage_date);
