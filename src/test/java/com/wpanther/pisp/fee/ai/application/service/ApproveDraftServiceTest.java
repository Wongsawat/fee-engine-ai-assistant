package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.FeeEnginePermissionDeniedException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftStatusException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.out.*;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApproveDraftServiceTest {
    private DraftRepository repo;
    private FeeEnginePort feeEngine;
    private ApproveDraftService service;
    private final UUID id = UUID.randomUUID();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repo = mock(DraftRepository.class);
        feeEngine = mock(FeeEnginePort.class);
        service = new ApproveDraftService(repo, feeEngine);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AiDraft draft(DraftType type, DraftStatus status, UUID target) {
        return new AiDraft(id, type, target, "p", "{\"feeType\":\"FREE\"}", "e",
                status, "{\"charges\":[]}", null, null, "u", null, "u", 1);
    }

    @Test
    void approveGenerateCallsCreate() throws Exception {
        UUID newId = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.DRY_RUN_PASSED, null)));
        when(feeEngine.create(any(), eq("tok")))
                .thenReturn(new FeeRuleResult(newId, mapper.readTree("{\"id\":\"" + newId + "\"}")));
        AiDraft result = service.approve(id, "tok");
        assertThat(result.status()).isEqualTo(DraftStatus.APPROVED);
        assertThat(result.feeRuleId()).isEqualTo(newId);
        verify(feeEngine).create(any(), eq("tok"));
    }

    @Test
    void approveUpdateCallsUpdate() throws Exception {
        UUID target = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.UPDATE, DraftStatus.DRY_RUN_PASSED, target)));
        when(feeEngine.update(eq(target), any(), eq("tok")))
                .thenReturn(new FeeRuleResult(target, mapper.readTree("{\"id\":\"" + target + "\"}")));
        AiDraft result = service.approve(id, "tok");
        assertThat(result.status()).isEqualTo(DraftStatus.APPROVED);
        verify(feeEngine).update(eq(target), any(), eq("tok"));
    }

    @Test
    void approveFromPendingThrows409() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.PENDING, null)));
        assertThatThrownBy(() -> service.approve(id, "tok")).isInstanceOf(InvalidDraftStatusException.class);
    }

    @Test
    void approveAlreadyApprovedIsIdempotent() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.APPROVED, null)));
        AiDraft result = service.approve(id, "tok");
        assertThat(result.status()).isEqualTo(DraftStatus.APPROVED);
        verifyNoInteractions(feeEngine);
    }

    @Test
    void approveUpdateTargetDeletedAutoResetsToPending() {
        UUID target = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.UPDATE, DraftStatus.DRY_RUN_PASSED, target)));
        when(feeEngine.update(eq(target), any(), any())).thenThrow(new FeeEngineClientException(404, null));
        assertThatThrownBy(() -> service.approve(id, "tok")).isInstanceOf(TargetRuleNotFoundException.class);
        ArgumentCaptor<AiDraft> saved = ArgumentCaptor.forClass(AiDraft.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(DraftStatus.PENDING);
        assertThat(saved.getValue().dryRunResult()).isNull();
    }

    @Test
    void approveGenerateDraftFeeEngine403ThrowsWithCreateMessage() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.DRY_RUN_PASSED, null)));
        when(feeEngine.create(any(), any())).thenThrow(new FeeEngineClientException(403, null));
        assertThatThrownBy(() -> service.approve(id, "tok"))
                .isInstanceOf(FeeEnginePermissionDeniedException.class)
                .hasMessageContaining("create fee rules");
    }

    @Test
    void approveUpdateDraftFeeEngine403ThrowsWithRuleId() {
        UUID target = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.UPDATE, DraftStatus.DRY_RUN_PASSED, target)));
        when(feeEngine.update(eq(target), any(), any())).thenThrow(new FeeEngineClientException(403, null));
        assertThatThrownBy(() -> service.approve(id, "tok"))
                .isInstanceOf(FeeEnginePermissionDeniedException.class)
                .hasMessageContaining(target.toString());
    }

    @Test
    void rejectFromPendingSetsRejected() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.PENDING, null)));
        AiDraft result = service.reject(id);
        assertThat(result.status()).isEqualTo(DraftStatus.REJECTED);
    }

    @Test
    void rejectTerminalThrows409() {
        when(repo.findById(id)).thenReturn(Optional.of(draft(DraftType.GENERATE, DraftStatus.APPROVED, null)));
        assertThatThrownBy(() -> service.reject(id)).isInstanceOf(InvalidDraftStatusException.class);
    }
}
