package com.wpanther.pisp.fee.ai.adapter.in.rest;

import com.wpanther.pisp.fee.ai.application.exception.*;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEngineClientException;
import com.wpanther.pisp.fee.ai.infrastructure.json.CanonicalisationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var d = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        var first = ex.getBindingResult().getFieldError();
        d.setDetail(first != null ? first.getDefaultMessage() : "Request validation failed");
        return d;
    }

    @ExceptionHandler(DraftNotFoundException.class)
    public ProblemDetail handleDraftNotFound(DraftNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TargetRuleNotFoundException.class)
    public ProblemDetail handleTargetNotFound(TargetRuleNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidDraftStatusException.class)
    public ProblemDetail handleInvalidStatus(InvalidDraftStatusException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidDraftRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidDraftRequestException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Draft was modified concurrently; reload and retry");
    }

    @ExceptionHandler(AiDisabledException.class)
    public ProblemDetail handleAiDisabled(AiDisabledException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ProblemDetail handleQuota(AiQuotaExceededException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler({AiOutputParseException.class, AiInputTooLargeException.class, CanonicalisationException.class})
    public ProblemDetail handleAiOutput(RuntimeException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(FeeEnginePermissionDeniedException.class)
    public ProblemDetail handleFeeEnginePermission(FeeEnginePermissionDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(FeeEngineClientException.class)
    public ProblemDetail handleFeeEngine(FeeEngineClientException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null || ex.status() == 400 || ex.status() == 409) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        }
        var d = problem(status, "Fee-engine rejected the request");
        if (ex.problemDetailBody() != null) {
            d.setProperty("feeEngineProblem", ex.problemDetailBody());
        }
        return d;
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        var d = ProblemDetail.forStatus(status);
        d.setDetail(detail);
        return d;
    }
}
