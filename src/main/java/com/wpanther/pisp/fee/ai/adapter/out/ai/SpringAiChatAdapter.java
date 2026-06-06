package com.wpanther.pisp.fee.ai.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.application.exception.*;
import com.wpanther.pisp.fee.ai.application.port.out.AiChatPort;
import com.wpanther.pisp.fee.ai.application.port.out.GenerationResult;
import com.wpanther.pisp.fee.ai.infrastructure.ai.SystemPromptLoader;
import com.wpanther.pisp.fee.ai.infrastructure.budget.AiChatBudget;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class SpringAiChatAdapter implements AiChatPort {

    private final ChatModel chatModel;
    private final SystemPromptLoader systemPrompt;
    private final AiChatProperties properties;
    private final AiChatBudget budget;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpringAiChatAdapter(ChatModel chatModel, SystemPromptLoader systemPrompt,
                               AiChatProperties properties, AiChatBudget budget,
                               MeterRegistry meterRegistry) {
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
        this.properties = properties;
        this.budget = budget;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public GenerationResult generate(String naturalLanguage, String enrichment) {
        String user = (enrichment == null || enrichment.isBlank())
                ? "Generate a fee rule for: " + naturalLanguage
                : "Update this existing fee rule:\n" + enrichment
                  + "\nAdmin's change: " + naturalLanguage
                  + "\nReturn the complete updated rule.";
        return call(user, "generate", this::parseGeneration);
    }

    @Override
    public String review(String ruleJson) {
        String user = "Review this fee rule JSON: (1) what payments it matches, "
                + "(2) schema constraint violations, (3) potential conflicts.\n\n" + ruleJson;
        return call(user, "review", s -> s);
    }

    // postProcess runs inside the try block so AiOutputParseException is captured in the timer outcome.
    private <T> T call(String userMessage, String operation, Function<String, T> postProcess) {
        if (!properties.isEnabled()) {
            countError(operation, "disabled");
            throw new AiDisabledException("AI assistant is currently disabled");
        }
        if (budget.isExceeded()) {
            countError(operation, "quota_exceeded");
            throw new AiQuotaExceededException("Daily AI token quota exceeded");
        }
        String system = systemPrompt.prompt();
        int total = system.length() + userMessage.length();
        if (total > properties.getMaxInputChars()) {
            countError(operation, "input_too_large");
            throw new AiInputTooLargeException(
                    "Combined prompt too large: " + total
                    + " chars (limit " + properties.getMaxInputChars() + ")");
        }
        Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(userMessage)));
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();
            recordMetrics(response, operation, system.length() + userMessage.length(), safeLen(text));
            return postProcess.apply(text);
        } catch (AiDisabledException | AiQuotaExceededException | AiInputTooLargeException e) {
            throw e;
        } catch (AiOutputParseException e) {
            outcome = "parse_error";
            countError(operation, "parse_error");
            throw e;
        } catch (Exception e) {
            outcome = "server_error";
            countError(operation, e.getClass().getSimpleName());
            throw e;
        } finally {
            sample.stop(Timer.builder("ai.assistant.chat.duration")
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private void countError(String operation, String errorType) {
        meterRegistry.counter("ai.assistant.chat.errors",
                "operation", operation, "error_type", errorType).increment();
    }

    private void recordMetrics(ChatResponse response, String operation,
                               int inputChars, int outputChars) {
        long inputTokens = 0, outputTokens = 0, totalTokens = 0;
        try {
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                if (usage.getPromptTokens() != null) inputTokens = usage.getPromptTokens();
                if (usage.getCompletionTokens() != null) outputTokens = usage.getCompletionTokens();
                if (usage.getTotalTokens() != null) totalTokens = usage.getTotalTokens();
            }
        } catch (Exception ignored) {}
        long estimatedInput = inputChars / 4L;
        long estimatedOutput = outputChars / 4L;
        budget.record(totalTokens > 0 ? totalTokens : estimatedInput + estimatedOutput);

        DistributionSummary.builder("ai.assistant.chat.tokens.input")
                .tag("operation", operation).register(meterRegistry).record(inputTokens);
        DistributionSummary.builder("ai.assistant.chat.tokens.output")
                .tag("operation", operation).register(meterRegistry).record(outputTokens);
        DistributionSummary.builder("ai.assistant.chat.tokens.input.estimated")
                .tag("operation", operation).register(meterRegistry).record(estimatedInput);
        DistributionSummary.builder("ai.assistant.chat.tokens.output.estimated")
                .tag("operation", operation).register(meterRegistry).record(estimatedOutput);
    }

    private GenerationResult parseGeneration(String content) {
        String preview = content == null ? "<null>" : content.substring(0, Math.min(500, content.length()));
        JsonNode root;
        try {
            root = mapper.readTree(content);
        } catch (Exception e) {
            throw new AiOutputParseException("AI returned non-JSON output. Raw (500 chars): " + preview);
        }
        if (root == null || !root.has("rule") || !root.has("explanation")) {
            throw new AiOutputParseException(
                    "AI response missing 'rule' or 'explanation' field. Raw (500 chars): " + preview);
        }
        return new GenerationResult(root.get("rule").toString(),
                root.get("explanation").asText());
    }

    private int safeLen(String s) { return s == null ? 0 : s.length(); }
}
