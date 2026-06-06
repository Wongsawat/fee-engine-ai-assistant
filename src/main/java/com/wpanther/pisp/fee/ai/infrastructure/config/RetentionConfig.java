package com.wpanther.pisp.fee.ai.infrastructure.config;

import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftJpaRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
public class RetentionConfig {
    private static final List<String> TERMINAL = List.of("APPROVED", "REJECTED");
    private final AiDraftJpaRepository repository;
    private final RetentionProperties properties;

    public RetentionConfig(AiDraftJpaRepository repository, RetentionProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runRetention() {
        Instant redactCutoff = Instant.now().minus(properties.getPromptRedactDays(), ChronoUnit.DAYS);
        Instant purgeCutoff = Instant.now().minus(properties.getPurgeDays(), ChronoUnit.DAYS);
        // Bypasses DraftRepository port intentionally: bulk JPQL avoids per-row @PreUpdate callbacks
        // so updated_at is NOT bumped on redaction (bumping would extend the purge window from T+90 to T+120).
        repository.redactPromptsOlderThan(TERMINAL, redactCutoff);
        repository.deleteByStatusInAndUpdatedAtBefore(TERMINAL, purgeCutoff);
    }
}
