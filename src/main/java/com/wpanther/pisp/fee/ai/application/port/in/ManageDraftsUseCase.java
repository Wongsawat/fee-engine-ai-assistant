package com.wpanther.pisp.fee.ai.application.port.in;

import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ManageDraftsUseCase {
    AiDraft get(UUID id);
    Page<AiDraft> list(DraftStatus status, String createdBy, Pageable pageable);
    AiDraft updateRuleJson(UUID id, String ruleJson);
    void delete(UUID id);
}
