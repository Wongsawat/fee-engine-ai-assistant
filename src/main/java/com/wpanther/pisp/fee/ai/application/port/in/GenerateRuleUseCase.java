package com.wpanther.pisp.fee.ai.application.port.in;

import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import java.util.UUID;

public interface GenerateRuleUseCase {
    AiDraft generate(GenerateCommand command, String bearerToken);
    record GenerateCommand(String prompt, DraftType type, UUID targetRuleId) {}
}
