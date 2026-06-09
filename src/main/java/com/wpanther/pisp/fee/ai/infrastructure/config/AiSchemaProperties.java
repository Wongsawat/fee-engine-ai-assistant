package com.wpanther.pisp.fee.ai.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai-assistant.schema")
public class AiSchemaProperties {

    @NotBlank
    private String feeEngineSchemaVersion = "V8";

    public String getFeeEngineSchemaVersion() { return feeEngineSchemaVersion; }
    public void setFeeEngineSchemaVersion(String v) { this.feeEngineSchemaVersion = v; }
}
