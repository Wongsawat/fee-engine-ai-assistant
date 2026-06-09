package com.wpanther.pisp.fee.ai.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        canonicalRule = normaliseNumericFields(canonicalRule);
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
        String preview = ruleJson == null ? "<null>" : ruleJson.substring(0, Math.min(500, ruleJson.length()));
        FeeRuleSchema schema;
        try {
            schema = objectMapper.readValue(ruleJson, FeeRuleSchema.class);
        } catch (AiOutputParseException e) {
            throw e;
        } catch (Exception e) {
            throw new AiOutputParseException(
                    "AI output could not be parsed as a fee rule: " + e.getMessage()
                    + ". Rule JSON (500 chars): " + preview);
        }

        Set<ConstraintViolation<FeeRuleSchema>> violations = validator.validate(schema);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new AiOutputParseException(
                    "AI output failed validation: " + msg + ". Rule JSON (500 chars): " + preview);
        }

        validateFeeTypeConstraints(schema, preview);
    }

    private void validateFeeTypeConstraints(FeeRuleSchema s, String preview) {
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
            case "TIERED_SLAB", "TIERED_STEP" -> {
                validateTiers(s.tiers(), ft);
                requireAbsent(s.flatAmount(), "feeType " + ft + " must not set flatAmount");
                requireAbsent(s.percentage(), "feeType " + ft + " must not set percentage");
                requireAbsent(s.minFee(), "feeType " + ft + " must not set minFee");
                requireAbsent(s.maxFee(), "feeType " + ft + " must not set maxFee");
            }
            case "FREE" -> {
                requireAbsent(s.flatAmount(), "feeType FREE must not set flatAmount");
                requireAbsent(s.percentage(), "feeType FREE must not set percentage");
                requireAbsent(s.tiers(), "feeType FREE must not set tiers");
                requireAbsent(s.minFee(), "feeType FREE must not set minFee");
                requireAbsent(s.maxFee(), "feeType FREE must not set maxFee");
            }
            // @NotBlank ensures feeType is non-blank but does not validate enumeration.
            // Reject unknown values here rather than forwarding to fee-engine (fail fast).
            default -> throw new AiOutputParseException(
                    "AI output invalid: unknown feeType '" + ft + "'. Rule JSON (500 chars): " + preview);
        }
    }

    private void validateTiers(java.util.List<FeeRuleSchema.Tier> tiers, String feeType) {
        if (tiers == null || tiers.isEmpty())
            throw new AiOutputParseException("AI output invalid: feeType " + feeType + " requires at least one tier");
        for (FeeRuleSchema.Tier tier : tiers) {
            String rt = tier.rateType();
            switch (rt) {
                case "FIXED" -> {
                    if (tier.amount() == null || tier.amount().isBlank())
                        throw new AiOutputParseException("AI output invalid: FIXED tier requires amount");
                    if (tier.percentage() != null && !tier.percentage().isBlank())
                        throw new AiOutputParseException("AI output invalid: FIXED tier must not set percentage");
                }
                case "PERCENTAGE" -> {
                    if (tier.percentage() == null || tier.percentage().isBlank())
                        throw new AiOutputParseException("AI output invalid: PERCENTAGE tier requires percentage");
                    if (tier.amount() != null && !tier.amount().isBlank())
                        throw new AiOutputParseException("AI output invalid: PERCENTAGE tier must not set amount");
                }
                case "HYBRID" -> {
                    if (tier.amount() == null || tier.amount().isBlank())
                        throw new AiOutputParseException("AI output invalid: HYBRID tier requires amount");
                    if (tier.percentage() == null || tier.percentage().isBlank())
                        throw new AiOutputParseException("AI output invalid: HYBRID tier requires percentage");
                }
                case "GREATER_OF" -> {
                    if (tier.amount() == null || tier.amount().isBlank())
                        throw new AiOutputParseException("AI output invalid: GREATER_OF tier requires amount");
                    if (tier.percentage() == null || tier.percentage().isBlank())
                        throw new AiOutputParseException("AI output invalid: GREATER_OF tier requires percentage");
                }
                default -> throw new AiOutputParseException(
                        "AI output invalid: unknown tier rateType '" + rt + "'");
            }
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

    private String normaliseNumericFields(String ruleJson) {
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(ruleJson);
            coerceToNumber(node, "flatAmount");
            coerceToNumber(node, "percentage");
            coerceToNumber(node, "minFee");
            coerceToNumber(node, "maxFee");
            JsonNode tiers = node.get("tiers");
            if (tiers != null && tiers.isArray()) {
                for (JsonNode tier : tiers) {
                    if (tier instanceof ObjectNode t) {
                        coerceToNumber(t, "min");
                        coerceToNumber(t, "max");
                        coerceToNumber(t, "amount");
                        coerceToNumber(t, "percentage");
                    }
                }
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return ruleJson;
        }
    }

    private void coerceToNumber(ObjectNode node, String field) {
        JsonNode val = node.get(field);
        if (val != null && val.isTextual()) {
            try {
                node.put(field, new java.math.BigDecimal(val.asText()));
            } catch (NumberFormatException ignored) {}
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
