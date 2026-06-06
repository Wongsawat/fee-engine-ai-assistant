package com.wpanther.pisp.fee.ai.adapter.in.rest;

import com.wpanther.pisp.fee.ai.adapter.in.rest.dto.*;
import com.wpanther.pisp.fee.ai.application.exception.InvalidDraftRequestException;
import com.wpanther.pisp.fee.ai.application.port.in.*;
import com.wpanther.pisp.fee.ai.application.port.in.GenerateRuleUseCase.GenerateCommand;
import com.wpanther.pisp.fee.ai.domain.model.AiDraft;
import com.wpanther.pisp.fee.ai.domain.model.DraftStatus;
import com.wpanther.pisp.fee.ai.domain.model.DraftType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai/drafts")
public class DraftController {

    private final GenerateRuleUseCase generateRuleUseCase;
    private final ReviewRuleUseCase reviewRuleUseCase;
    private final ManageDraftsUseCase manageDraftsUseCase;
    private final RunDryRunUseCase runDryRunUseCase;
    private final ApproveDraftUseCase approveDraftUseCase;
    private final DraftDtoMapper mapper;

    public DraftController(GenerateRuleUseCase generateRuleUseCase,
                           ReviewRuleUseCase reviewRuleUseCase,
                           ManageDraftsUseCase manageDraftsUseCase,
                           RunDryRunUseCase runDryRunUseCase,
                           ApproveDraftUseCase approveDraftUseCase,
                           DraftDtoMapper mapper) {
        this.generateRuleUseCase = generateRuleUseCase;
        this.reviewRuleUseCase = reviewRuleUseCase;
        this.manageDraftsUseCase = manageDraftsUseCase;
        this.runDryRunUseCase = runDryRunUseCase;
        this.approveDraftUseCase = approveDraftUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/generate")
    public ResponseEntity<DraftResponse> generate(@RequestBody @Valid GenerateRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        DraftType type = parseType(request.type());
        AiDraft draft = generateRuleUseCase.generate(
                new GenerateCommand(request.prompt(), type, request.targetRuleId()),
                jwt.getTokenValue());
        return ResponseEntity.created(URI.create("/ai/drafts/" + draft.id()))
                .body(mapper.toResponse(draft));
    }

    @PostMapping("/review")
    public ReviewResponse review(@RequestBody @Valid ReviewRequest request) {
        return new ReviewResponse(reviewRuleUseCase.review(request.ruleJson()));
    }

    @GetMapping
    public List<DraftResponse> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String createdBy,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        if (size < 1) throw new InvalidDraftRequestException("size must be >= 1 (got: " + size + ")");
        DraftStatus statusEnum = parseStatus(status);
        Page<AiDraft> result = manageDraftsUseCase.list(statusEnum, createdBy,
                PageRequest.of(page, size));
        return result.map(mapper::toResponse).getContent();
    }

    @GetMapping("/{id}")
    public DraftResponse get(@PathVariable UUID id) {
        return mapper.toResponse(manageDraftsUseCase.get(id));
    }

    @PutMapping("/{id}")
    public DraftResponse update(@PathVariable UUID id, @RequestBody @Valid UpdateDraftRequest request) {
        return mapper.toResponse(manageDraftsUseCase.updateRuleJson(id, request.ruleJson()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        manageDraftsUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/dry-run")
    public DraftResponse dryRun(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(runDryRunUseCase.dryRun(id, jwt.getTokenValue()));
    }

    @PostMapping("/{id}/approve")
    public DraftResponse approve(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(approveDraftUseCase.approve(id, jwt.getTokenValue()));
    }

    @PostMapping("/{id}/reject")
    public DraftResponse reject(@PathVariable UUID id) {
        return mapper.toResponse(approveDraftUseCase.reject(id));
    }

    private static DraftType parseType(String raw) {
        if (raw == null) return DraftType.GENERATE;
        try { return DraftType.valueOf(raw); }
        catch (IllegalArgumentException e) { throw new InvalidDraftRequestException("Invalid type: " + raw); }
    }

    private static DraftStatus parseStatus(String raw) {
        if (raw == null) return null;
        try { return DraftStatus.valueOf(raw); }
        catch (IllegalArgumentException e) { throw new InvalidDraftRequestException("Invalid status: " + raw); }
    }
}
