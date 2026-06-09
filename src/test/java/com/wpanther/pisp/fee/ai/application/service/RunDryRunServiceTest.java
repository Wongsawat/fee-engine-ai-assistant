package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftStatusException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.out.*;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RunDryRunServiceTest {
    private DraftRepository repo;
    private FeeEnginePort feeEngine;
    private RunDryRunService service;
    private final UUID id = UUID.randomUUID();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repo = mock(DraftRepository.class);
        feeEngine = mock(FeeEnginePort.class);
        service = new RunDryRunService(repo, feeEngine);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AiDraft draft(DraftType type, DraftStatus status, UUID target) {
        return new AiDraft(id, type, target, "p", "{\"feeType\":\"FREE\"}", "e",
                status, null, null, null, "u", null, "u", 1);
    }

    @Test
    void passedDryRunSetsDryRunPassed() throws Exception {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.PENDING, null)));
        when(feeEngine.dryRun(any(), eq("tok"), any(), any()))
                .thenReturn(new DryRunResult(true, mapper.readTree("{\"charges\":[]}")));
        AiDraft result = service.dryRun(id, "tok", null, null);
        assertThat(result.status()).isEqualTo(DraftStatus.DRY_RUN_PASSED);
        assertThat(result.dryRunResult()).contains("charges");
    }

    @Test
    void failedDryRunSetsDryRunFailed() throws Exception {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.PENDING, null)));
        when(feeEngine.dryRun(any(), any(), any(), any()))
                .thenReturn(new DryRunResult(false, mapper.readTree("{\"detail\":\"bad\"}")));
        AiDraft result = service.dryRun(id, "tok", null, null);
        assertThat(result.status()).isEqualTo(DraftStatus.DRY_RUN_FAILED);
    }

    @Test
    void updateDraftWithDeletedTargetThrows404() {
        UUID target = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.UPDATE, DraftStatus.PENDING, target)));
        when(feeEngine.fetchRule(eq(target), any())).thenThrow(new FeeEngineClientException(404, null));
        assertThatThrownBy(() -> service.dryRun(id, "tok", null, null)).isInstanceOf(TargetRuleNotFoundException.class);
    }

    @Test
    void reDryRunFromDryRunFailedRecomputesStatus() throws Exception {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.DRY_RUN_FAILED, null)));
        when(feeEngine.dryRun(any(), any(), any(), any()))
                .thenReturn(new DryRunResult(true, mapper.readTree("{\"charges\":[]}")));
        AiDraft result = service.dryRun(id, "tok", null, null);
        assertThat(result.status()).isEqualTo(DraftStatus.DRY_RUN_PASSED);
    }

    @Test
    void dryRunFromTerminalStatusThrows409() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.APPROVED, null)));
        assertThatThrownBy(() -> service.dryRun(id, "tok", null, null)).isInstanceOf(InvalidDraftStatusException.class);
    }

    @Test
    void reDryRunFromDryRunPassedRecomputesResult() throws Exception {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.DRY_RUN_PASSED, null)));
        when(feeEngine.dryRun(any(), any(), any(), any()))
                .thenReturn(new DryRunResult(true, mapper.readTree("{\"charges\":[{\"amount\":\"25.00\"}]}")));
        AiDraft result = service.dryRun(id, "tok", null, null);
        assertThat(result.status()).isEqualTo(DraftStatus.DRY_RUN_PASSED);
        assertThat(result.dryRunResult()).contains("25.00");
    }
}
