package com.wpanther.pisp.fee.ai.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(AiChatProperties.class)
    static class TestConfig {}

    @Test
    void acceptsValidConfig() {
        runner.withPropertyValues(
                "ai-assistant.chat.max-input-chars=20000",
                "ai-assistant.chat.daily-token-limit=1000000")
            .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void rejectsMaxInputCharsAboveDbCeiling() {
        runner.withPropertyValues("ai-assistant.chat.max-input-chars=60000")
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                    .rootCause().isInstanceOf(BindValidationException.class);
            });
    }

    @Test
    void rejectsNegativeDailyTokenLimit() {
        runner.withPropertyValues("ai-assistant.chat.daily-token-limit=-1")
            .run(ctx -> assertThat(ctx).hasFailed());
    }
}
