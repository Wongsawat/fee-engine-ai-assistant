package com.wpanther.pisp.fee.ai.infrastructure.budget;

import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AiChatBudget {

    private final AiChatProperties properties;
    private final AtomicLong dailyTokens = new AtomicLong(0);

    public AiChatBudget(AiChatProperties properties, MeterRegistry registry) {
        this.properties = properties;
        registry.gauge("ai.assistant.chat.tokens.daily", dailyTokens);
    }

    public boolean isExceeded() {
        long limit = properties.getDailyTokenLimit();
        return limit > 0 && dailyTokens.get() >= limit;
    }

    public void record(long tokens) {
        dailyTokens.addAndGet(tokens);
    }

    public void reset() {
        dailyTokens.set(0);
    }

    public long current() {
        return dailyTokens.get();
    }
}
