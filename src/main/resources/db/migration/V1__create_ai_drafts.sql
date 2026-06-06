CREATE TABLE ai_drafts (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(20)  NOT NULL,
    target_rule_id   UUID,
    prompt           TEXT         NOT NULL,
    rule_json        JSONB        NOT NULL,
    explanation      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    dry_run_result   JSONB,
    fee_rule_id      UUID,
    version          INT          NOT NULL DEFAULT 0,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_prompt_length   CHECK (char_length(prompt) <= 2000),
    CONSTRAINT chk_rule_json_size  CHECK (length(rule_json::text) <= 50000)
);

CREATE INDEX idx_ai_drafts_status     ON ai_drafts (status);
CREATE INDEX idx_ai_drafts_created_at ON ai_drafts (created_at DESC);
CREATE INDEX idx_ai_drafts_created_by ON ai_drafts (created_by);
