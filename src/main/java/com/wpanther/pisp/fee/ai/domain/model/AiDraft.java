package com.wpanther.pisp.fee.ai.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AiDraft(
        UUID id,
        DraftType type,
        UUID targetRuleId,
        String prompt,
        String ruleJson,
        String explanation,
        DraftStatus status,
        String dryRunResult,
        UUID feeRuleId,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        int version
) {}
