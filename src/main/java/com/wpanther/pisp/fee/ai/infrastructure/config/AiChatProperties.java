package com.wpanther.pisp.fee.ai.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai-assistant.chat")
public class AiChatProperties {

    private boolean enabled = true;

    @Min(1) @Max(300)
    private int timeoutSeconds = 30;

    @Min(1) @Max(50000)
    private int maxInputChars = 20000;

    @Min(0) @Max(100_000_000)
    private long dailyTokenLimit = 0;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int v) { this.timeoutSeconds = v; }
    public int getMaxInputChars() { return maxInputChars; }
    public void setMaxInputChars(int v) { this.maxInputChars = v; }
    public long getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(long v) { this.dailyTokenLimit = v; }
}
