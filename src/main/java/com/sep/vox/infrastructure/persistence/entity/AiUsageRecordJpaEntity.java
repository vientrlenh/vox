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
@Table(name = "ai_usage_records", uniqueConstraints = {
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

    // Chi phí MỘT lời gọi AI quy sang VND -- giá trị lẻ nhất trong cả hệ thống (một lượt nói có thể
    // chỉ tốn vài phần trăm đồng). Đây là đầu vào cộng dồn cho school_subscription_quota_records
    // .used_amount_vnd nên phải giữ nguyên numeric(18,6): làm tròn từng dòng về 2 chữ số là mất
    // trắng khoản trừ, và sai số làm tròn HALF_UP tích lũy lệch một chiều qua hàng nghìn lượt.
    @Column(name = "cost_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal costVnd;

    // Tỷ giá đã dùng lúc quy đổi -- numeric(12,4), trùng school_balance_entries.fx_rate_used.
    @Column(name = "fx_rate_used", nullable = false, updatable = false, precision = 12, scale = 4)
    private BigDecimal fxRateUsed;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AiUsageRecordJpaEntity() {}

    public AiUsageRecordJpaEntity(UUID id, UUID examSessionId, UUID turnId, UUID usageEventId, String usageType,
            String provider, String modelName, Integer inputTokens, Integer outputTokens,
            Integer cacheCreationInputTokens, Integer cacheReadInputTokens, Long durationMs, String unitPriceJson,
            BigDecimal costUsd, BigDecimal costVnd, BigDecimal fxRateUsed, Instant occurredAt) {
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
        this.costVnd = costVnd;
        this.fxRateUsed = fxRateUsed;
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

    public BigDecimal getCostVnd() {
        return costVnd;
    }

    public void setCostVnd(BigDecimal costVnd) {
        this.costVnd = costVnd;
    }

    public BigDecimal getFxRateUsed() {
        return fxRateUsed;
    }

    public void setFxRateUsed(BigDecimal fxRateUsed) {
        this.fxRateUsed = fxRateUsed;
    }

    
}