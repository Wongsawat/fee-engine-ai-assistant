package com.wpanther.pisp.fee.ai.infrastructure.config;

import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RetentionJobTest {
    @Test
    void purgesTerminalDraftsAndRedactsPrompts() {
        var repo = mock(AiDraftJpaRepository.class);
        var props = new RetentionProperties();
        props.setPromptRedactDays(30);
        props.setPurgeDays(90);
        var config = new RetentionConfig(repo, props);
        config.runRetention();
        ArgumentCaptor<List<String>> statuses = ArgumentCaptor.forClass(List.class);
        verify(repo).redactPromptsOlderThan(statuses.capture(), any(Instant.class));
        verify(repo).deleteByStatusInAndUpdatedAtBefore(eq(List.of("APPROVED", "REJECTED")), any(Instant.class));
        assertThat(statuses.getValue()).containsExactlyInAnyOrder("APPROVED", "REJECTED");
    }
}
