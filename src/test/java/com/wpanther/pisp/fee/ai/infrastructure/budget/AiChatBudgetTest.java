package com.wpanther.pisp.fee.ai.infrastructure.budget;

import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AiChatBudgetTest {
    private AiChatBudget budget(long limit) {
        var props = new AiChatProperties();
        props.setDailyTokenLimit(limit);
        return new AiChatBudget(props, new SimpleMeterRegistry());
    }
    @Test
    void notExceededWhenLimitIsZero() {
        var b = budget(0); b.record(1_000_000);
        assertThat(b.isExceeded()).isFalse();
    }
    @Test
    void exceededOnceLimitReached() {
        var b = budget(100);
        assertThat(b.isExceeded()).isFalse();
        b.record(100);
        assertThat(b.isExceeded()).isTrue();
    }
    @Test
    void resetZeroesCounter() {
        var b = budget(100); b.record(100); b.reset();
        assertThat(b.isExceeded()).isFalse();
        assertThat(b.current()).isZero();
    }
}
