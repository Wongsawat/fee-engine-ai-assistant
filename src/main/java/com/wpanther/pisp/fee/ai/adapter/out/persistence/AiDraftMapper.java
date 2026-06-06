package com.wpanther.pisp.fee.ai.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftEntity;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.infrastructure.json.CanonicalisationException;
import org.springframework.stereotype.Component;

@Component
public class AiDraftMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public AiDraft toDomain(AiDraftEntity e) {
        return new AiDraft(
                e.getId(),
                DraftType.valueOf(e.getType()),
                e.getTargetRuleId(),
                e.getPrompt(),
                e.getRuleJson() == null ? null : e.getRuleJson().toString(),
                e.getExplanation(),
                DraftStatus.valueOf(e.getStatus()),
                e.getDryRunResult() == null ? null : e.getDryRunResult().toString(),
                e.getFeeRuleId(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getUpdatedAt(),
                e.getUpdatedBy(),
                e.getVersion());
    }

    public void applyToEntity(AiDraft d, AiDraftEntity e) {
        e.setType(d.type().name());
        e.setTargetRuleId(d.targetRuleId());
        e.setPrompt(d.prompt());
        e.setRuleJson(parse(d.ruleJson()));
        e.setExplanation(d.explanation());
        e.setStatus(d.status().name());
        e.setDryRunResult(d.dryRunResult() == null ? null : parse(d.dryRunResult()));
        e.setFeeRuleId(d.feeRuleId());
    }

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception ex) {
            throw new CanonicalisationException("Cannot store invalid JSON", ex);
        }
    }
}
