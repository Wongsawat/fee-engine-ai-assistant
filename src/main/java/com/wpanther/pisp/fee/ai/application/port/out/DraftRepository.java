package com.wpanther.pisp.fee.ai.application.port.out;

import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DraftRepository {
    AiDraft save(AiDraft draft);
    Optional<AiDraft> findById(UUID id);
    Page<AiDraft> find(DraftStatus status, String createdBy, Pageable pageable);
    void deleteById(UUID id);
}
