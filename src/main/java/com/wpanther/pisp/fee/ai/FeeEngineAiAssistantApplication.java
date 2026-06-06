package com.wpanther.pisp.fee.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FeeEngineAiAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeeEngineAiAssistantApplication.class, args);
    }
}
