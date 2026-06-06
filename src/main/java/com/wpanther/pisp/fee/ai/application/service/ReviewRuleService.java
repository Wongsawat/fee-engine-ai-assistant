package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.port.in.ReviewRuleUseCase;
import com.wpanther.pisp.fee.ai.application.port.out.AiChatPort;
import org.springframework.stereotype.Service;

@Service
public class ReviewRuleService implements ReviewRuleUseCase {
    private final AiChatPort aiChatPort;
    public ReviewRuleService(AiChatPort aiChatPort) { this.aiChatPort = aiChatPort; }
    @Override
    public String review(String ruleJson) { return aiChatPort.review(ruleJson); }
}
