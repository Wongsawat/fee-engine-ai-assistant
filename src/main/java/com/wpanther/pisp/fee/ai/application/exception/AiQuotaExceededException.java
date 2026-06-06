package com.wpanther.pisp.fee.ai.application.exception;

public class AiQuotaExceededException extends RuntimeException {
    public AiQuotaExceededException(String message) {
        super(message);
    }
}
