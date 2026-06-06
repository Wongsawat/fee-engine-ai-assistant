package com.wpanther.pisp.fee.ai.adapter.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.adapter.in.rest.dto.DraftResponse;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import org.springframework.stereotype.Component;

@Component
public class DraftDtoMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    public DraftResponse toResponse(AiDraft d) {
        return new DraftResponse(d.id(), d.type().name(), d.targetRuleId(), d.prompt(),
                parse(d.ruleJson()), d.explanation(), d.status().name(),
                parse(d.dryRunResult()), d.feeRuleId(),
                d.createdAt(), d.createdBy(), d.updatedAt(), d.updatedBy(), d.version());
    }

    private JsonNode parse(String json) {
        if (json == null) return null;
        try { return mapper.readTree(json); }
        catch (Exception e) { return null; }
    }
}
