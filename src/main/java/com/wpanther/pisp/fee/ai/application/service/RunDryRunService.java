package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.DraftNotFoundException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftStatusException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.in.RunDryRunUseCase;
import com.wpanther.pisp.fee.ai.application.port.out.DraftRepository;
import com.wpanther.pisp.fee.ai.application.port.out.DryRunResult;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEngineClientException;
import com.wpanther.pisp.fee.ai.application.port.out.FeeEnginePort;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RunDryRunService implements RunDryRunUseCase {

    private final DraftRepository repository;
    private final FeeEnginePort feeEnginePort;

    public RunDryRunService(DraftRepository repository, FeeEnginePort feeEnginePort) {
        this.repository = repository;
        this.feeEnginePort = feeEnginePort;
    }

    @Override
    @Transactional
    public AiDraft dryRun(UUID id, String bearerToken, BigDecimal amount, String currency) {
        AiDraft draft = repository.findById(id)
                .orElseThrow(() -> new DraftNotFoundException("Draft " + id + " not found"));

        if (draft.status() != DraftStatus.PENDING
                && draft.status() != DraftStatus.DRY_RUN_FAILED
                && draft.status() != DraftStatus.DRY_RUN_PASSED) {
            throw new InvalidDraftStatusException("Cannot dry-run a draft in status " + draft.status());
        }

        if (draft.type() == DraftType.UPDATE) {
            verifyTargetExists(draft.targetRuleId(), bearerToken);
        }

        // Re-running from DRY_RUN_FAILED: new status/dryRunResult fully overwrite the prior
        // failed result; no separate PENDING write is persisted.
        DryRunResult result = feeEnginePort.dryRun(draft.ruleJson(), bearerToken, amount, currency);
        DraftStatus newStatus = result.passed() ? DraftStatus.DRY_RUN_PASSED : DraftStatus.DRY_RUN_FAILED;

        AiDraft updated = new AiDraft(
                draft.id(), draft.type(), draft.targetRuleId(),
                draft.prompt(), draft.ruleJson(), draft.explanation(),
                newStatus, result.body() == null ? null : result.body().toString(),
                draft.feeRuleId(), draft.createdAt(), draft.createdBy(),
                draft.updatedAt(), draft.updatedBy(), draft.version());
        return repository.save(updated);
    }

    private void verifyTargetExists(UUID targetRuleId, String bearerToken) {
        try {
            feeEnginePort.fetchRule(targetRuleId, bearerToken);
        } catch (FeeEngineClientException e) {
            if (e.status() == 404) {
                throw new TargetRuleNotFoundException("Target rule " + targetRuleId + " no longer exists");
            }
            throw e;
        }
    }
}
