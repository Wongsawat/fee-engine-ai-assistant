package com.wpanther.pisp.fee.ai.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

public class FeeEngineClientException extends RuntimeException {

    private final int status;
    private final transient JsonNode problemDetailBody;

    public FeeEngineClientException(int status, JsonNode problemDetailBody) {
        super("Fee-engine returned status " + status);
        this.status = status;
        this.problemDetailBody = problemDetailBody;
    }

    public int status() { return status; }
    public JsonNode problemDetailBody() { return problemDetailBody; }
}
