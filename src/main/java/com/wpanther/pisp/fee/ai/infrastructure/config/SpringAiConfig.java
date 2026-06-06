package com.wpanther.pisp.fee.ai.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring AI's Anthropic auto-configuration provides the ChatModel bean from
 * spring.ai.anthropic.* properties. The HTTP read timeout is governed by the
 * Anthropic client; ai-assistant.chat.timeout-seconds documents the intended bound.
 */
@Configuration
public class SpringAiConfig {}
