package com.wpanther.pisp.fee.ai.adapter.in.rest.dto;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
public record DraftResponse(
        UUID id, String type, UUID targetRuleId, String prompt,
        JsonNode ruleJson, String explanation, String status,
        JsonNode dryRunResult, UUID feeRuleId,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy, int version) {}
