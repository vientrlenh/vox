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

    @Column(name = "assessment_session_id", nullable = false, updatable = false)
    private UUID assessmentSessionId;

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

    // NULL = this expense has not been deducted from the school wallet yet. The ONLY column of the table that is not updatable=false,
    // because it is the only thing that changes after the row is written -- and deliberately NOT in the constructor: a new
    // row always starts in the uncharged state, and nowhere may a "charged" row be built ready-made.
    // It is stamped by a bulk UPDATE (markChargedByAssessmentSessionId), not by saving the entity.
    //
    // WARNING if a path is later added that rewrites an EXISTING row: AiUsageRecord (domain) does not carry this
    // column, so the mapper builds a new entity with chargedAt = null and a merge would ERASE the charged stamp -- that
    // session would be charged again at the next grading. It is safe now because save() is only called with new rows
    // (RecordAiUsageUseCase); adding an update path requires bringing this column into the domain model first.
    @Column(name = "charged_at")
    private Instant chargedAt;

    /**
     * NULL = chưa quyết định; có giá trị = khoản chi này SẼ KHÔNG BAO GIỜ được thu của trường.
     *
     * <p>Tách khỏi {@code chargedAt} vì trước V9 một mình cột đó phải mang hai nghĩa lẫn nhau ("chưa
     * thu" và "sẽ không thu"), và câu giành tiền chỉ nhìn NULL nên nó nuốt luôn phần token đã đốt ở
     * lượt chấm hỏng. Giữ MỐC thay vì cờ boolean để trả lời được "sự cố hôm đó tốn nền tảng bao
     * nhiêu".
     *
     * <p>Cùng cảnh báo như {@code chargedAt}: domain model không mang cột này, nên một đường ghi lại
     * dòng đã tồn tại sẽ xóa dấu miễn.
     */
    @Column(name = "waived_at")
    private Instant waivedAt;

    protected AiUsageRecordJpaEntity() {}

    public AiUsageRecordJpaEntity(UUID id, UUID assessmentSessionId, UUID turnId, UUID usageEventId, String usageType,
            String provider, String modelName, Integer inputTokens, Integer outputTokens,
            Integer cacheCreationInputTokens, Integer cacheReadInputTokens, Long durationMs, String unitPriceJson,
            BigDecimal costUsd, BigDecimal costVnd, BigDecimal fxRateUsed, Instant occurredAt) {
        this.id = id;
        this.assessmentSessionId = assessmentSessionId;
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

    public Instant getChargedAt() {
        return chargedAt;
    }

    public void setChargedAt(Instant chargedAt) {
        this.chargedAt = chargedAt;
    }

    public Instant getWaivedAt() {
        return waivedAt;
    }

    public void setWaivedAt(Instant waivedAt) {
        this.waivedAt = waivedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
    }

    public void setAssessmentSessionId(UUID assessmentSessionId) {
        this.assessmentSessionId = assessmentSessionId;
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