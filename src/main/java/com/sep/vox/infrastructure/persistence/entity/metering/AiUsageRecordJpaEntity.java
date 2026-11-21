package com.sep.vox.infrastructure.persistence.entity.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ai_usage_records", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ai_usage_record_usage_event_id", columnNames = "usage_event_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsageRecordJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
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

    // The cost of ONE AI call converted to VND -- the smallest value in the whole system (one speaking turn may cost only a
    // few hundredths of a dong). This is the input that accumulates into school_subscription_quota_records
    // .used_amount_vnd, so it must keep numeric(18,6): rounding each row to 2 digits would wipe out the deduction, and the
    // HALF_UP rounding error would accumulate in one direction over thousands of calls.
    @Column(name = "cost_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal costVnd;

    // The exchange rate used for the conversion -- numeric(12,4), matching school_balance_entries.fx_rate_used.
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
     * NULL = not decided yet; a value = this charge WILL NEVER be collected from the school.
     *
     * <p>Separate from {@code chargedAt} because before V9 that column alone had to carry two conflicting meanings ("not
     * collected yet" and "will not be collected"), and the claiming statement only looked at NULL, so it swallowed the
     * tokens burned by a failed grading run as well. A TIMESTAMP is kept instead of a boolean flag so we can answer "how
     * much did that day's incident cost the platform".
     *
     * <p>Same warning as {@code chargedAt}: the domain model does not carry this column, so a write path that saves an
     * existing row again would erase the waiver mark.
     */
    @Column(name = "waived_at")
    private Instant waivedAt;

    
}