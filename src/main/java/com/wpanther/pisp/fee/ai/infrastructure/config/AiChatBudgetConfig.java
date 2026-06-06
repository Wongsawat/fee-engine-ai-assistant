package com.wpanther.pisp.fee.ai.infrastructure.config;

import com.wpanther.pisp.fee.ai.infrastructure.budget.AiChatBudget;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class AiChatBudgetConfig {
    private final AiChatBudget budget;
    public AiChatBudgetConfig(AiChatBudget budget) { this.budget = budget; }
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyBudget() { budget.reset(); }
}
