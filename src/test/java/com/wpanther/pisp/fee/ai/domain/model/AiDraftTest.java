package com.wpanther.pisp.fee.ai.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AiDraftTest {

    @Test
    void pendingCanTransitionToDryRunStates() {
        assertThat(DraftStatus.PENDING.canTransitionTo(DraftStatus.DRY_RUN_PASSED)).isTrue();
        assertThat(DraftStatus.PENDING.canTransitionTo(DraftStatus.DRY_RUN_FAILED)).isTrue();
        assertThat(DraftStatus.PENDING.canTransitionTo(DraftStatus.REJECTED)).isTrue();
    }

    @Test
    void dryRunPassedCanApproveOrResetOrReject() {
        assertThat(DraftStatus.DRY_RUN_PASSED.canTransitionTo(DraftStatus.APPROVED)).isTrue();
        assertThat(DraftStatus.DRY_RUN_PASSED.canTransitionTo(DraftStatus.PENDING)).isTrue();
        assertThat(DraftStatus.DRY_RUN_PASSED.canTransitionTo(DraftStatus.REJECTED)).isTrue();
    }

    @Test
    void approvedAndRejectedAreTerminal() {
        assertThat(DraftStatus.APPROVED.isTerminal()).isTrue();
        assertThat(DraftStatus.REJECTED.isTerminal()).isTrue();
        assertThat(DraftStatus.APPROVED.canTransitionTo(DraftStatus.PENDING)).isFalse();
        assertThat(DraftStatus.REJECTED.canTransitionTo(DraftStatus.PENDING)).isFalse();
    }

    @Test
    void pendingCannotJumpStraightToApproved() {
        assertThat(DraftStatus.PENDING.canTransitionTo(DraftStatus.APPROVED)).isFalse();
    }
}
