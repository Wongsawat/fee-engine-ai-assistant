package com.wpanther.pisp.fee.ai.application.service;

import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftRequestException;
import com.wpanther.pisp.fee.ai.application.exception.TargetRuleNotFoundException;
import com.wpanther.pisp.fee.ai.application.port.in.GenerateRuleUseCase.GenerateCommand;
import com.wpanther.pisp.fee.ai.application.port.out.*;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.infrastructure.json.JsonCanonicaliser;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GenerateRuleServiceTest {
    private AiChatPort aiChatPort;
    private FeeEnginePort feeEnginePort;
    private DraftRepository draftRepository;
    private GenerateRuleService service;
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        aiChatPort = mock(AiChatPort.class);
        feeEnginePort = mock(FeeEnginePort.class);
        draftRepository = mock(DraftRepository.class);
        service = new GenerateRuleService(aiChatPort, feeEnginePort, draftRepository, new JsonCanonicaliser(), VALIDATOR);
        when(draftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generatesPendingDraftForGenerateType() {
        when(aiChatPort.generate(eq("0.5% SWIFT"), eq("")))
                .thenReturn(new GenerationResult(
                        "{\"paymentType\":\"INTERNATIONAL\",\"scheme\":\"SWIFT\",\"chargeBearer\":\"Shared\","
                        + "\"chargeType\":\"ServiceCharge\",\"feeType\":\"PERCENTAGE\","
                        + "\"percentage\":\"0.005\",\"currency\":\"GBP\"}", "explains"));
        AiDraft draft = service.generate(new GenerateCommand("0.5% SWIFT", DraftType.GENERATE, null), "tok");
        assertThat(draft.status()).isEqualTo(DraftStatus.PENDING);
        assertThat(draft.explanation()).isEqualTo("explains");
    }

    @Test
    void updateTypeEnrichesPromptWithExistingRule() {
        UUID target = UUID.randomUUID();
        var node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("feeType", "FLAT");
        when(feeEnginePort.fetchRule(eq(target), eq("tok"))).thenReturn(new FeeRuleResult(target, node));
        when(aiChatPort.generate(any(), any()))
                .thenReturn(new GenerationResult(
                        "{\"paymentType\":\"DOMESTIC\",\"scheme\":\"FPS\",\"chargeBearer\":\"BorneByDebtor\","
                        + "\"chargeType\":\"ServiceCharge\",\"feeType\":\"FLAT\","
                        + "\"flatAmount\":\"6.00\",\"currency\":\"GBP\"}", "updated"));
        service.generate(new GenerateCommand("make it 6 GBP", DraftType.UPDATE, target), "tok");
        ArgumentCaptor<String> enrichment = ArgumentCaptor.forClass(String.class);
        verify(aiChatPort).generate(eq("make it 6 GBP"), enrichment.capture());
        assertThat(enrichment.getValue()).contains("FLAT");
    }

    @Test
    void updateTypeWithoutTargetRuleIdIsRejected() {
        assertThatThrownBy(() -> service.generate(new GenerateCommand("x", DraftType.UPDATE, null), "tok"))
                .isInstanceOf(InvalidDraftRequestException.class);
    }

    @Test
    void generateTypeWithTargetRuleIdIsRejected() {
        assertThatThrownBy(() -> service.generate(new GenerateCommand("x", DraftType.GENERATE, UUID.randomUUID()), "tok"))
                .isInstanceOf(InvalidDraftRequestException.class);
    }

    @Test
    void missingTargetRuleDuringEnrichmentThrows404() {
        UUID target = UUID.randomUUID();
        when(feeEnginePort.fetchRule(eq(target), any())).thenThrow(new FeeEngineClientException(404, null));
        assertThatThrownBy(() -> service.generate(new GenerateCommand("x", DraftType.UPDATE, target), "tok"))
                .isInstanceOf(TargetRuleNotFoundException.class);
    }

    @Test
    void aiOutputMissingRequiredFieldsThrowsAiOutputParseException() {
        when(aiChatPort.generate(any(), any()))
                .thenReturn(new GenerationResult("{\"feeType\":\"FLAT\",\"flatAmount\":\"5.00\"}", "oops"));
        assertThatThrownBy(() -> service.generate(new GenerateCommand("x", DraftType.GENERATE, null), "tok"))
                .isInstanceOf(com.wpanther.pisp.fee.ai.application.exception.AiOutputParseException.class);
    }
}
