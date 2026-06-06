package com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.pisp.fee.ai.support.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class AiDraftJpaRepositoryTest extends PostgresTestSupport {

    @Autowired AiDraftJpaRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    private AiDraftEntity newDraft(String status) throws Exception {
        var e = new AiDraftEntity();
        e.setType("GENERATE");
        e.setPrompt("0.5% for SWIFT");
        e.setRuleJson(mapper.readTree("{\"feeType\":\"PERCENTAGE\"}"));
        e.setStatus(status);
        return e;
    }

    @Test
    void persistsAndQueriesByStatus() throws Exception {
        repository.saveAndFlush(newDraft("PENDING"));
        var page = repository.findByStatus("PENDING",
                org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getVersion()).isZero();
    }

    @Test
    void rejectsPromptLongerThan2000Chars() throws Exception {
        var e = newDraft("PENDING");
        e.setPrompt("x".repeat(2001));
        assertThatThrownBy(() -> repository.saveAndFlush(e))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
