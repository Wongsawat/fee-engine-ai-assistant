package com.wpanther.pisp.fee.ai.adapter.out.persistence.jpa;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_drafts")
@EntityListeners(AuditingEntityListener.class)
public class AiDraftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "type", nullable = false) private String type;
    @Column(name = "target_rule_id") private UUID targetRuleId;
    @Column(name = "prompt", nullable = false) private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode ruleJson;

    @Column(name = "explanation") private String explanation;
    @Column(name = "status", nullable = false) private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dry_run_result", columnDefinition = "jsonb")
    private JsonNode dryRunResult;

    @Column(name = "fee_rule_id") private UUID feeRuleId;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @CreatedBy
    @Column(name = "created_by") private String createdBy;
    @LastModifiedBy
    @Column(name = "updated_by") private String updatedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public UUID getTargetRuleId() { return targetRuleId; }
    public void setTargetRuleId(UUID v) { this.targetRuleId = v; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String v) { this.prompt = v; }
    public JsonNode getRuleJson() { return ruleJson; }
    public void setRuleJson(JsonNode v) { this.ruleJson = v; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String v) { this.explanation = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public JsonNode getDryRunResult() { return dryRunResult; }
    public void setDryRunResult(JsonNode v) { this.dryRunResult = v; }
    public UUID getFeeRuleId() { return feeRuleId; }
    public void setFeeRuleId(UUID v) { this.feeRuleId = v; }
    public int getVersion() { return version; }
    public void setVersion(int v) { this.version = v; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
