package com.wpanther.pisp.fee.ai.infrastructure.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai-assistant.retention")
public class RetentionProperties {

    @Min(1)
    private int promptRedactDays = 30;
    @Min(1)
    private int purgeDays = 90;

    public int getPromptRedactDays() { return promptRedactDays; }
    public void setPromptRedactDays(int v) { this.promptRedactDays = v; }
    public int getPurgeDays() { return purgeDays; }
    public void setPurgeDays(int v) { this.purgeDays = v; }
}
