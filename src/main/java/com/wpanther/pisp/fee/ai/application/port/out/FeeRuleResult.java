package com.wpanther.pisp.fee.ai.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record FeeRuleResult(UUID id, JsonNode body) {}
