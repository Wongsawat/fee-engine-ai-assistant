package com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiDraftJpaRepository extends JpaRepository<AiDraftEntity, UUID> {

    Page<AiDraftEntity> findByStatus(String status, Pageable pageable);
    Page<AiDraftEntity> findByCreatedBy(String createdBy, Pageable pageable);
    Page<AiDraftEntity> findByStatusAndCreatedBy(String status, String createdBy, Pageable pageable);

    @Modifying
    @Query("delete from AiDraftEntity d where d.status in :statuses and d.updatedAt < :cutoff")
    int deleteByStatusInAndUpdatedAtBefore(@Param("statuses") List<String> statuses,
                                           @Param("cutoff") Instant cutoff);

    @Modifying
    @Query("update AiDraftEntity d set d.prompt = '[redacted]' "
         + "where d.status in :statuses and d.updatedAt < :cutoff and d.prompt <> '[redacted]'")
    int redactPromptsOlderThan(@Param("statuses") List<String> statuses,
                               @Param("cutoff") Instant cutoff);
}
