package com.wpanther.pisp.fee.ai.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.application.exception.AiOutputParseException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftRequestException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.model.FeeRuleSchema;
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
        FeeRuleSchema schema;
        try {
            schema = objectMapper.readValue(ruleJson, FeeRuleSchema.class);
        } catch (AiOutputParseException e) {
            throw e;
        } catch (Exception e) {
            throw new AiOutputParseException("AI output could not be parsed as a fee rule: " + e.getMessage());
        }

        Set<ConstraintViolation<FeeRuleSchema>> violations = validator.validate(schema);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new AiOutputParseException("AI output failed validation: " + msg);
        }

        validateFeeTypeConstraints(schema);
    }

    private void validateFeeTypeConstraints(FeeRuleSchema s) {
        String ft = s.feeType();
        switch (ft) {
            case "FLAT" -> {
                requirePresent(s.flatAmount(), "feeType FLAT requires flatAmount");
                requireAbsent(s.percentage(), "feeType FLAT must not set percentage");
                requireAbsent(s.tiers(), "feeType FLAT must not set tiers");
                requireAbsent(s.minFee(), "feeType FLAT must not set minFee");
                requireAbsent(s.maxFee(), "feeType FLAT must not set maxFee");
            }
            case "PERCENTAGE" -> {
                requirePresent(s.percentage(), "feeType PERCENTAGE requires percentage");
                requireAbsent(s.flatAmount(), "feeType PERCENTAGE must not set flatAmount");
                requireAbsent(s.tiers(), "feeType PERCENTAGE must not set tiers");
            }
            case "TIERED" -> {
                if (s.tiers() == null || s.tiers().isEmpty())
                    throw new AiOutputParseException("AI output invalid: feeType TIERED requires at least one tier");
                requireAbsent(s.flatAmount(), "feeType TIERED must not set flatAmount");
                requireAbsent(s.percentage(), "feeType TIERED must not set percentage");
                requireAbsent(s.minFee(), "feeType TIERED must not set minFee");
                requireAbsent(s.maxFee(), "feeType TIERED must not set maxFee");
            }
            case "FREE" -> {
                requireAbsent(s.flatAmount(), "feeType FREE must not set flatAmount");
                requireAbsent(s.percentage(), "feeType FREE must not set percentage");
                requireAbsent(s.tiers(), "feeType FREE must not set tiers");
                requireAbsent(s.minFee(), "feeType FREE must not set minFee");
                requireAbsent(s.maxFee(), "feeType FREE must not set maxFee");
            }
            // Unknown feeType values are caught upstream by bean validation (@NotBlank covers presence;
            // fee-engine validates the enumerated values — we don't duplicate that enum list here).
        }
    }

    private static void requirePresent(String value, String message) {
        if (value == null || value.isBlank())
            throw new AiOutputParseException("AI output invalid: " + message);
    }

    private static void requireAbsent(String value, String message) {
        if (value != null && !value.isBlank())
            throw new AiOutputParseException("AI output invalid: " + message);
    }

    private static void requireAbsent(java.util.List<?> value, String message) {
        if (value != null && !value.isEmpty())
            throw new AiOutputParseException("AI output invalid: " + message);
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
