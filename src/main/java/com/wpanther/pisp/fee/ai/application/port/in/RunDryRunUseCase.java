package com.wpanther.pisp.fee.ai.application.port.in;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import java.math.BigDecimal;
import java.util.UUID;
public interface RunDryRunUseCase {
    AiDraft dryRun(UUID id, String bearerToken, BigDecimal amount, String currency);
}
