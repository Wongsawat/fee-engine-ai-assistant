package com.wpanther.pisp.fee.ai.application.exception;

import java.util.UUID;

public class FeeEnginePermissionDeniedException extends RuntimeException {
    public FeeEnginePermissionDeniedException(UUID targetRuleId) {
        super("Caller does not have permission to modify rule " + targetRuleId);
    }
}
