package com.wpanther.pisp.fee.ai.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateFeeRuleRequest(
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
        @Valid List<TierRequest> tiers,
        @NotBlank String currency,
        Integer priority
) {
    public record TierRequest(@NotNull String min, @NotNull String max, @NotNull String amount) {}
}
