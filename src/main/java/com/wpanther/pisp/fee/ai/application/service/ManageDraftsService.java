package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.DraftNotFoundException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftRequestException;
import com.wpanther.pisp.fee.ai.application.port.in.ManageDraftsUseCase;
import com.wpanther.pisp.fee.ai.application.port.out.DraftRepository;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.infrastructure.json.JsonCanonicaliser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ManageDraftsService implements ManageDraftsUseCase {

    private final DraftRepository repository;
    private final JsonCanonicaliser canonicaliser;

    public ManageDraftsService(DraftRepository repository, JsonCanonicaliser canonicaliser) {
        this.repository = repository;
        this.canonicaliser = canonicaliser;
    }

    @Override
    public AiDraft get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DraftNotFoundException("Draft " + id + " not found"));
    }

    @Override
    public Page<AiDraft> list(DraftStatus status, String createdBy, Pageable pageable) {
        return repository.find(status, createdBy, pageable);
    }

    @Override
    @Transactional
    public AiDraft updateRuleJson(UUID id, String ruleJson) {
        AiDraft draft = get(id);
        String canonical = canonicaliser.canonicalise(ruleJson);
        if (canonical.equals(canonicaliser.canonicalise(draft.ruleJson()))) {
            return draft; // no-op: identical after canonicalisation
        }
        AiDraft updated = new AiDraft(
                draft.id(), draft.type(), draft.targetRuleId(),
                draft.prompt(), canonical, draft.explanation(),
                DraftStatus.PENDING, null, draft.feeRuleId(),
                draft.createdAt(), draft.createdBy(),
                draft.updatedAt(), draft.updatedBy(), draft.version());
        return repository.save(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AiDraft draft = get(id);
        if (draft.status() == DraftStatus.APPROVED) {
            throw new InvalidDraftRequestException("APPROVED drafts cannot be deleted");
        }
        repository.deleteById(id);
    }
}
