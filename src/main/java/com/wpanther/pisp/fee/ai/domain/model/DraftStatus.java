package com.wpanther.pisp.fee.ai.domain.model;

import java.util.Set;

public enum DraftStatus {
    PENDING,
    DRY_RUN_PASSED,
    DRY_RUN_FAILED,
    APPROVED,
    REJECTED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED;
    }

    public boolean canTransitionTo(DraftStatus target) {
        return switch (this) {
            case PENDING        -> Set.of(DRY_RUN_PASSED, DRY_RUN_FAILED, REJECTED).contains(target);
            case DRY_RUN_PASSED -> Set.of(APPROVED, PENDING, REJECTED).contains(target);
            case DRY_RUN_FAILED -> Set.of(PENDING, REJECTED).contains(target);
            case APPROVED, REJECTED -> false;
        };
    }
}
