package com.wpanther.pisp.fee.ai.application.port.in;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import java.util.UUID;
public interface ApproveDraftUseCase {
    AiDraft approve(UUID id, String bearerToken);
    AiDraft reject(UUID id);
}
