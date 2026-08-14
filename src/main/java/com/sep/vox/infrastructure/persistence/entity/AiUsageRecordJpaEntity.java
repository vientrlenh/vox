package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ai_usage_record", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ai_usage_record_usage_event_id", columnNames = "usage_event_id")
})
public class AiUsageRecordJpaEntity {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id",
        nullable = false,
        updatable = false,
        insertable = false,
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "exam_session_id", nullable = false, updatable = false)
    private UUID examSessionId;

    @Column(name = "turn_id", nullable = false, updatable = false)
    private UUID turnId;

    @Column(name = "usage_event_id", nullable = false, updatable = false)
    private UUID usageEventId;

    @Column(name = "usage_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_ai_usage_record_usage_type_valid",
            constraint = "usage_type IN ('LLM_TOKEN', 'DURATION')"
        )
    })
    private String usageType;

    @Column(name = "provider", nullable = false, updatable = false, length = 50)
    private String provider;

    @Column(name = "model_name", updatable = false, length = 100)
    private String modelName;

    @Column(name = "input_tokens", updatable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", updatable = false)
    private Integer outputTokens;

    @Column(name = "cache_creation_input_tokens", updatable = false)
    private Integer cacheCreationInputTokens;

    @Column(name = "cache_read_input_tokens", updatable = false)
    private Integer cacheReadInputTokens;

    @Column(name = "duration_ms", updatable = false)
    private Long durationMs;

    @Column(name = "unit_price_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String unitPriceJson;

    @Column(name = "cost_usd", nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AiUsageRecordJpaEntity() {}

    public AiUsageRecordJpaEntity(UUID id, UUID examSessionId, UUID turnId, UUID usageEventId, String usageType,
            String provider, String modelName, Integer inputTokens, Integer outputTokens,
            Integer cacheCreationInputTokens, Integer cacheReadInputTokens, Long durationMs, String unitPriceJson,
            BigDecimal costUsd, Instant occurredAt) {
        this.id = id;
        this.examSessionId = examSessionId;
        this.turnId = turnId;
        this.usageEventId = usageEventId;
        this.usageType = usageType;
        this.provider = provider;
        this.modelName = modelName;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.durationMs = durationMs;
        this.unitPriceJson = unitPriceJson;
        this.costUsd = costUsd;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getTurnId() {
        return turnId;
    }

    public void setTurnId(UUID turnId) {
        this.turnId = turnId;
    }

    public UUID getUsageEventId() {
        return usageEventId;
    }

    public void setUsageEventId(UUID usageEventId) {
        this.usageEventId = usageEventId;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getCacheCreationInputTokens() {
        return cacheCreationInputTokens;
    }

    public void setCacheCreationInputTokens(Integer cacheCreationInputTokens) {
        this.cacheCreationInputTokens = cacheCreationInputTokens;
    }

    public Integer getCacheReadInputTokens() {
        return cacheReadInputTokens;
    }

    public void setCacheReadInputTokens(Integer cacheReadInputTokens) {
        this.cacheReadInputTokens = cacheReadInputTokens;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getUnitPriceJson() {
        return unitPriceJson;
    }

    public void setUnitPriceJson(String unitPriceJson) {
        this.unitPriceJson = unitPriceJson;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}