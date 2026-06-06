package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.DraftNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.out.DraftRepository;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.infrastructure.json.JsonCanonicaliser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ManageDraftsServiceTest {
    private DraftRepository repo;
    private ManageDraftsService service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(DraftRepository.class);
        service = new ManageDraftsService(repo, new JsonCanonicaliser());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AiDraft existing(DraftStatus status, String ruleJson, String dryRunResult) {
        return new AiDraft(id, DraftType.GENERATE, null, "p", ruleJson, "e",
                status, dryRunResult, null, null, "u", null, "u", 1);
    }

    @Test
    void getMissingThrows() {
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(DraftNotFoundException.class);
    }

    @Test
    void editWithChangedJsonResetsToPendingAndClearsDryRunResult() {
        when(repo.findById(id)).thenReturn(Optional.of(
                existing(DraftStatus.DRY_RUN_PASSED, "{\"a\":1}", "{\"charges\":[]}")));
        service.updateRuleJson(id, "{\"a\":2}");
        ArgumentCaptor<AiDraft> saved = ArgumentCaptor.forClass(AiDraft.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(DraftStatus.PENDING);
        assertThat(saved.getValue().dryRunResult()).isNull();
    }

    @Test
    void byteEqualEditIsNoOpAndDoesNotSave() {
        when(repo.findById(id)).thenReturn(Optional.of(
                existing(DraftStatus.DRY_RUN_PASSED, "{\"a\":1}", "{\"charges\":[]}")));
        AiDraft result = service.updateRuleJson(id, "{ \"a\": 1 }");
        verify(repo, never()).save(any());
        assertThat(result.status()).isEqualTo(DraftStatus.DRY_RUN_PASSED);
    }
}
