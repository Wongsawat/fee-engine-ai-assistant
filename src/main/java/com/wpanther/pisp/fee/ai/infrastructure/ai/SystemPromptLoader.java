package com.wpanther.pisp.fee.ai.infrastructure.ai;

import com.wpanther.pisp.fee.ai.infrastructure.config.AiSchemaProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Component
public class SystemPromptLoader {

    private final String prompt;

    public SystemPromptLoader(AiSchemaProperties schemaProperties) {
        this.prompt = load().replace("${schemaVersion}",
                schemaProperties.getFeeEngineSchemaVersion());
    }

    private String load() {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("ai/system-prompt.txt").getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load system-prompt.txt", e);
        }
    }

    public String prompt() { return prompt; }
}
