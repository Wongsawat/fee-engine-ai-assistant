package com.wpanther.pisp.fee.ai.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.adapter.in.rest.dto.CreateFeeRuleRequest;
import com.wpanther.pisp.fee.ai.application.exception.AiOutputParseException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftRequestException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.in.GenerateRuleUseCase;
import com.wpanther.pisp.fee.ai.application.port.out.*;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.infrastructure.json.JsonCanonicaliser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GenerateRuleService implements GenerateRuleUseCase {

    private final AiChatPort aiChatPort;
    private final FeeEnginePort feeEnginePort;
    private final DraftRepository draftRepository;
    private final JsonCanonicaliser canonicaliser;
    private final Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenerateRuleService(AiChatPort aiChatPort, FeeEnginePort feeEnginePort,
                               DraftRepository draftRepository, JsonCanonicaliser canonicaliser,
                               Validator validator) {
        this.aiChatPort = aiChatPort;
        this.feeEnginePort = feeEnginePort;
        this.draftRepository = draftRepository;
        this.canonicaliser = canonicaliser;
        this.validator = validator;
    }

    @Override
    public AiDraft generate(GenerateCommand command, String bearerToken) {
        validate(command);
        String enrichment = "";
        if (command.type() == DraftType.UPDATE) {
            enrichment = fetchEnrichment(command.targetRuleId(), bearerToken);
        }
        GenerationResult result = aiChatPort.generate(command.prompt(), enrichment);
        String canonicalRule = canonicaliser.canonicalise(result.ruleJson());
        validateAiOutput(canonicalRule);

        AiDraft draft = new AiDraft(
                null, command.type(), command.targetRuleId(),
                command.prompt(), canonicalRule, result.explanation(),
                DraftStatus.PENDING, null, null,
                null, null, null, null, 0);
        return draftRepository.save(draft);
    }

    private void validate(GenerateCommand command) {
        if (command.type() == DraftType.UPDATE && command.targetRuleId() == null) {
            throw new InvalidDraftRequestException("targetRuleId is required for UPDATE drafts");
        }
        if (command.type() == DraftType.GENERATE && command.targetRuleId() != null) {
            throw new InvalidDraftRequestException("targetRuleId must be null for GENERATE drafts");
        }
    }

    private void validateAiOutput(String ruleJson) {
        try {
            CreateFeeRuleRequest req = objectMapper.readValue(ruleJson, CreateFeeRuleRequest.class);
            Set<ConstraintViolation<CreateFeeRuleRequest>> violations = validator.validate(req);
            if (!violations.isEmpty()) {
                String msg = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining("; "));
                throw new AiOutputParseException("AI output failed validation: " + msg);
            }
        } catch (AiOutputParseException e) {
            throw e;
        } catch (Exception e) {
            throw new AiOutputParseException("AI output could not be parsed as CreateFeeRuleRequest: " + e.getMessage());
        }
    }

    private String fetchEnrichment(java.util.UUID targetRuleId, String bearerToken) {
        try {
            FeeRuleResult rule = feeEnginePort.fetchRule(targetRuleId, bearerToken);
            return rule.body() == null ? "" : rule.body().toString();
        } catch (FeeEngineClientException e) {
            if (e.status() == 404) {
                throw new TargetRuleNotFoundException("Target rule " + targetRuleId + " no longer exists");
            }
            throw e;
        }
    }
}
