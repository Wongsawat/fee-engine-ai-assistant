package com.wpanther.pisp.fee.ai.application.port.out;

import java.util.UUID;

public interface FeeEnginePort {
    DryRunResult dryRun(String ruleJson, String bearerToken);
    FeeRuleResult create(String ruleJson, String bearerToken);
    FeeRuleResult update(UUID ruleId, String ruleJson, String bearerToken);
    FeeRuleResult fetchRule(UUID ruleId, String bearerToken);
}
