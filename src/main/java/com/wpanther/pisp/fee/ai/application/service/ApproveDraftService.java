package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.DraftNotFoundException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftStatusException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.in.ApproveDraftUseCase;
import com.wpanther.pisp.fee.ai.application.port.out.DraftRepository;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEngineClientException;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEnginePort;
import com.wpanther.pisp.fee.ai.application.port.out.FeeRuleResult;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ApproveDraftService implements ApproveDraftUseCase {

    private final DraftRepository repository;
    private final FeeEnginePort feeEnginePort;

    public ApproveDraftService(DraftRepository repository, FeeEnginePort feeEnginePort) {
        this.repository = repository;
        this.feeEnginePort = feeEnginePort;
    }

    @Override
    @Transactional
    public AiDraft approve(UUID id, String bearerToken) {
        AiDraft draft = load(id);
        if (draft.status() == DraftStatus.APPROVED) {
            return draft; // idempotent
        }
        if (draft.status() != DraftStatus.DRY_RUN_PASSED) {
            throw new InvalidDraftStatusException("Cannot approve a draft in status " + draft.status());
        }
        try {
            FeeRuleResult result = (draft.type() == DraftType.UPDATE)
                    ? feeEnginePort.update(draft.targetRuleId(), draft.ruleJson(), bearerToken)
                    : feeEnginePort.create(draft.ruleJson(), bearerToken);
            return repository.save(withStatus(draft, DraftStatus.APPROVED, result.id()));
        } catch (FeeEngineClientException e) {
            if (e.status() == 404 && draft.type() == DraftType.UPDATE) {
                repository.save(resetToPending(draft));
                throw new TargetRuleNotFoundException(
                        "Target rule " + draft.targetRuleId() + " was deleted; draft reset to PENDING — re-run dry-run or reject");
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public AiDraft reject(UUID id) {
        AiDraft draft = load(id);
        if (draft.status().isTerminal()) {
            throw new InvalidDraftStatusException("Cannot reject a draft in terminal status " + draft.status());
        }
        return repository.save(withStatus(draft, DraftStatus.REJECTED, draft.feeRuleId()));
    }

    private AiDraft load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DraftNotFoundException("Draft " + id + " not found"));
    }

    private AiDraft withStatus(AiDraft d, DraftStatus status, UUID feeRuleId) {
        return new AiDraft(d.id(), d.type(), d.targetRuleId(), d.prompt(), d.ruleJson(),
                d.explanation(), status, d.dryRunResult(), feeRuleId,
                d.createdAt(), d.createdBy(), d.updatedAt(), d.updatedBy(), d.version());
    }

    private AiDraft resetToPending(AiDraft d) {
        return new AiDraft(d.id(), d.type(), d.targetRuleId(), d.prompt(), d.ruleJson(),
                d.explanation(), DraftStatus.PENDING, null, d.feeRuleId(),
                d.createdAt(), d.createdBy(), d.updatedAt(), d.updatedBy(), d.version());
    }
}
