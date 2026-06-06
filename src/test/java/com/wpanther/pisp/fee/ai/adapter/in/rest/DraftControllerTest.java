package com.wpanther.pisp.fee.ai.adapter.in.rest;

import com.wpanther.pisp.fee.ai.application.exception.DraftNotFoundException;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftStatusException;
import com.wpanther.pisp.fee.ai.application.port.in.*;
import com.wpanther.pisp.fee.ai.application.port.in.GenerateRuleUseCase.GenerateCommand;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import com.wpanther.pisp.fee.ai.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DraftController.class)
@Import({DraftDtoMapper.class, GlobalExceptionHandler.class, SecurityConfig.class})
class DraftControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GenerateRuleUseCase generateRuleUseCase;
    @MockitoBean ReviewRuleUseCase reviewRuleUseCase;
    @MockitoBean ManageDraftsUseCase manageDraftsUseCase;
    @MockitoBean RunDryRunUseCase runDryRunUseCase;
    @MockitoBean ApproveDraftUseCase approveDraftUseCase;
    @MockitoBean JwtDecoder jwtDecoder;

    private static final UUID ID = UUID.randomUUID();

    private AiDraft sample(DraftStatus status) {
        return new AiDraft(ID, DraftType.GENERATE, null, "p", "{\"feeType\":\"FREE\"}",
                "e", status, null, null, Instant.now(), "admin", Instant.now(), "admin", 0);
    }

    @Test
    void generateRequiresWriteScope() throws Exception {
        when(generateRuleUseCase.generate(any(GenerateCommand.class), any()))
                .thenReturn(sample(DraftStatus.PENDING));
        mockMvc.perform(post("/ai/drafts/generate")
                        .with(jwt().jwt(j -> j.claim("sub", "admin"))
                                .authorities(() -> "SCOPE_ai-rules:write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"0.5% SWIFT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void reviewAllowedWithReadScope() throws Exception {
        when(reviewRuleUseCase.review(any())).thenReturn("analysis text");
        mockMvc.perform(post("/ai/drafts/review")
                        .with(jwt().jwt(j -> j.claim("sub", "admin"))
                                .authorities(() -> "SCOPE_ai-rules:read"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("analysis text"));
    }

    @Test
    void getMissingDraftReturns404() throws Exception {
        when(manageDraftsUseCase.get(ID)).thenThrow(new DraftNotFoundException("Draft " + ID + " not found"));
        mockMvc.perform(get("/ai/drafts/{id}", ID)
                        .with(jwt().authorities(() -> "SCOPE_ai-rules:read")))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveFromWrongStatusReturns409() throws Exception {
        when(approveDraftUseCase.approve(any(), any()))
                .thenThrow(new InvalidDraftStatusException("Cannot approve a draft in status PENDING"));
        mockMvc.perform(post("/ai/drafts/{id}/approve", ID)
                        .with(jwt().jwt(j -> j.claim("sub", "admin"))
                                .authorities(() -> "SCOPE_ai-rules:write")))
                .andExpect(status().isConflict());
    }

    @Test
    void generateWithBlankPromptReturns400() throws Exception {
        mockMvc.perform(post("/ai/drafts/generate")
                        .with(jwt().jwt(j -> j.claim("sub", "admin"))
                                .authorities(() -> "SCOPE_ai-rules:write"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void writeEndpointRejectsReadOnlyToken() throws Exception {
        mockMvc.perform(post("/ai/drafts/{id}/approve", ID)
                        .with(jwt().authorities(() -> "SCOPE_ai-rules:read")))
                .andExpect(status().isForbidden());
    }
}
