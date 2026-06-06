package com.wpanther.pisp.fee.ai.adapter.in.rest.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
public record GenerateRequest(
        @NotBlank(message = "prompt is required") @Size(max = 2000, message = "prompt must be 2000 characters or fewer") String prompt,
        String type,
        UUID targetRuleId) {}
