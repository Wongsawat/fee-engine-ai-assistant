package com.wpanther.pisp.fee.ai.adapter.out.ai;

import com.wpanther.pisp.fee.ai.application.exception.AiInputTooLargeException;
import com.wpanther.pisp.fee.ai.application.exception.AiOutputParseException;
import com.wpanther.pisp.fee.ai.application.port.out.GenerationResult;
import com.wpanther.pisp.fee.ai.infrastructure.ai.SystemPromptLoader;
import com.wpanther.pisp.fee.ai.infrastructure.budget.AiChatBudget;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiSchemaProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiChatAdapterTest {

    private ChatModel chatModel;
    private SpringAiChatAdapter adapter;
    private AiChatProperties properties;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        var schema = new AiSchemaProperties();
        schema.setFeeEngineSchemaVersion("V7");
        var loader = new SystemPromptLoader(schema);
        properties = new AiChatProperties();
        var budget = new AiChatBudget(properties, new SimpleMeterRegistry());
        adapter = new SpringAiChatAdapter(chatModel, loader, properties, budget, new SimpleMeterRegistry());
    }

    private void stubResponse(String text) {
        var resp = new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        when(chatModel.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(resp);
    }

    @Test
    void parsesRuleAndExplanation() {
        stubResponse("{\"rule\":{\"feeType\":\"FREE\"},\"explanation\":\"free\"}");
        GenerationResult result = adapter.generate("free for FPS", "");
        assertThat(result.ruleJson()).contains("FREE");
        assertThat(result.explanation()).isEqualTo("free");
    }

    @Test
    void throwsOnNonJson() {
        stubResponse("I cannot do that");
        assertThatThrownBy(() -> adapter.generate("x", ""))
                .isInstanceOf(AiOutputParseException.class);
    }

    @Test
    void throwsWhenRuleKeyMissing() {
        stubResponse("{\"explanation\":\"no rule here\"}");
        assertThatThrownBy(() -> adapter.generate("x", ""))
                .isInstanceOf(AiOutputParseException.class);
    }

    @Test
    void rejectsOversizedCombinedPrompt() {
        properties.setMaxInputChars(10);
        assertThatThrownBy(() -> adapter.generate("a very long natural language prompt", ""))
                .isInstanceOf(AiInputTooLargeException.class);
    }
}
