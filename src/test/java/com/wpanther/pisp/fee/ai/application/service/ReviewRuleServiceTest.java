package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.port.out.AiChatPort;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReviewRuleServiceTest {
    @Test
    void delegatesToAiChatPort() {
        AiChatPort port = mock(AiChatPort.class);
        when(port.review("{\"feeType\":\"FREE\"}")).thenReturn("Matches FREE payments.");
        var service = new ReviewRuleService(port);
        assertThat(service.review("{\"feeType\":\"FREE\"}")).isEqualTo("Matches FREE payments.");
        verify(port).review("{\"feeType\":\"FREE\"}");
    }
}
