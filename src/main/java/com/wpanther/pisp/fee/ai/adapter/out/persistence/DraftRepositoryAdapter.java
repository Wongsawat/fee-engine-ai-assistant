package com.wpanther.pisp.fee.ai.adapter.out.persistence;

import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftEntity;
import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftJpaRepository;
import com.wpanther.pisp.fee.ai.application.port.out.DraftRepository;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DraftRepositoryAdapter implements DraftRepository {

    private final AiDraftJpaRepository jpa;
    private final AiDraftMapper mapper;

    public DraftRepositoryAdapter(AiDraftJpaRepository jpa, AiDraftMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public AiDraft save(AiDraft draft) {
        AiDraftEntity entity;
        if (draft.id() == null) {
            entity = new AiDraftEntity();
        } else {
            entity = jpa.findById(draft.id()).orElseThrow(() ->
                    new ObjectOptimisticLockingFailureException(AiDraftEntity.class, draft.id()));
            entity.setVersion(draft.version());
        }
        mapper.applyToEntity(draft, entity);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<AiDraft> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<AiDraft> find(DraftStatus status, String createdBy, Pageable pageable) {
        Page<AiDraftEntity> page;
        String s = status == null ? null : status.name();
        if (s != null && createdBy != null) {
            page = jpa.findByStatusAndCreatedBy(s, createdBy, pageable);
        } else if (s != null) {
            page = jpa.findByStatus(s, pageable);
        } else if (createdBy != null) {
            page = jpa.findByCreatedBy(createdBy, pageable);
        } else {
            page = jpa.findAll(pageable);
        }
        return page.map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
