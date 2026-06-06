package com.wpanther.pisp.fee.ai.application.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Structural schema for a fee-engine rule payload. Used to validate AI-generated
 * rule JSON before persisting a draft. Mirrors the fields fee-engine's create/update
 * endpoints accept; cross-field fee-type constraints are enforced in GenerateRuleService.
 */
public record FeeRuleSchema(
        @NotBlank String paymentType,
        @NotBlank String scheme,
        @NotBlank String chargeBearer,
        String accountIdentification,
        String destinationCountry,
        @NotBlank String chargeType,
        @NotBlank String feeType,
        String flatAmount,
        String percentage,
        String minFee,
        String maxFee,
        @Valid List<Tier> tiers,
        @NotBlank String currency,
        Integer priority
) {
    public record Tier(@NotNull String min, @NotNull String max, @NotNull String amount) {}
}
