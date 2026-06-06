package com.wpanther.pisp.fee.ai.adapter.in.rest.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ReviewRequest(
        @NotBlank(message = "ruleJson is required") @Size(max = 50_000, message = "ruleJson must be 50000 characters or fewer") String ruleJson) {}
