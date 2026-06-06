package com.wpanther.pisp.fee.ai.adapter.out.persistence;

import com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa.AiDraftJpaRepository;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({DraftRepositoryAdapter.class, AiDraftMapper.class})
class DraftRepositoryAdapterTest extends PostgresTestSupport {

    @Autowired DraftRepositoryAdapter adapter;
    @Autowired AiDraftJpaRepository jpa;
    @Autowired TestEntityManager em;

    private AiDraft newDraft() {
        return new AiDraft(null, DraftType.GENERATE, null, "0.5% SWIFT",
                "{\"feeType\":\"PERCENTAGE\"}", "explains it",
                DraftStatus.PENDING, null, null,
                null, null, null, null, 0);
    }

    @Test
    void savesAndReadsBack() {
        AiDraft saved = adapter.save(newDraft());
        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findById(saved.id())).isPresent();
    }

    @Test
    void filtersByStatus() {
        adapter.save(newDraft());
        var page = adapter.find(DraftStatus.PENDING, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void savingAnExistingDraftWhoseRowWasDeletedThrows() {
        AiDraft saved = adapter.save(newDraft());
        adapter.deleteById(saved.id());
        em.flush();
        em.clear();
        assertThatThrownBy(() -> adapter.save(saved))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
