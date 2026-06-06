package com.wpanther.pisp.fee.ai;

import com.wpanther.pisp.fee.ai.infrastructure.config.AiChatProperties;
import com.wpanther.pisp.fee.ai.infrastructure.config.AiSchemaProperties;
import com.wpanther.pisp.fee.ai.infrastructure.config.RetentionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        AiChatProperties.class,
        AiSchemaProperties.class,
        RetentionProperties.class
})
public class FeeEngineAiAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeeEngineAiAssistantApplication.class, args);
    }
}
