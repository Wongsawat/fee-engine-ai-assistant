package com.wpanther.pisp.fee.ai.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

public record DryRunResult(boolean passed, JsonNode body) {}
