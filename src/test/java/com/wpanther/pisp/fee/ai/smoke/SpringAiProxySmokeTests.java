package com.wpanther.pisp.fee.ai.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.adapter.out.ai.SpringAiChatAdapter;
import com.wpanther.pisp.fee.ai.application.port.out.GenerationResult;
import com.wpanther.pisp.fee.ai.infrastructure.ai.SystemPromptLoader;
import com.wpanther.pisp.fee.ai.infrastructure.budget.AiChatBudget;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiSchemaProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_AUTH_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_BASE_URL", matches = ".+")
class SpringAiProxySmokeTests {

    static final int SMOKE_TEST_MAX_PROMPT_CHARS = 500;

    private SpringAiChatAdapter realAdapter() {
        AnthropicApi api = AnthropicApi.builder()
                .baseUrl(System.getenv("ANTHROPIC_BASE_URL"))
                .apiKey(System.getenv("ANTHROPIC_AUTH_TOKEN"))
                .build();
        AnthropicChatModel model = AnthropicChatModel.builder().anthropicApi(api).build();
        var schema = new AiSchemaProperties();
        schema.setFeeEngineSchemaVersion("V7");
        var props = new AiChatProperties();
        return new SpringAiChatAdapter(model, new SystemPromptLoader(schema),
                props, new AiChatBudget(props, new SimpleMeterRegistry()), new SimpleMeterRegistry());
    }

    private void assertGeneratesValidRule(String prompt) throws Exception {
        assertThat(prompt.length()).isLessThanOrEqualTo(SMOKE_TEST_MAX_PROMPT_CHARS);
        GenerationResult result = realAdapter().generate(prompt, "");
        var node = new ObjectMapper().readTree(result.ruleJson());
        assertThat(node.has("feeType")).isTrue();
    }

    @Test
    void generatesFlatRule() throws Exception {
        assertGeneratesValidRule("Flat 5 GBP fee for domestic FPS payments");
    }

    @Test
    void generatesPercentageRuleWithCaps() throws Exception {
        assertGeneratesValidRule("0.5% fee for SWIFT international payments, minimum 1 GBP, maximum 50 GBP");
    }

    @Test
    void generatesTieredRule() throws Exception {
        assertGeneratesValidRule("Tiered fee: 1 GBP up to 100, 3 GBP above 100, for CHAPS");
    }
}
