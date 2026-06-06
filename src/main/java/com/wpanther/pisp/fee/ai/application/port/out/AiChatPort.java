package com.wpanther.pisp.fee.ai.application.port.out;

public interface AiChatPort {
    GenerationResult generate(String naturalLanguage, String enrichment);
    String review(String ruleJson);
}
