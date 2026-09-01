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
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Sổ chi phí AI của trường — xem {@code SchoolAiSpendEntry} và V10 cho lý do bảng này tồn tại bên
 * cạnh {@code ai_usage_records} và {@code school_balance_entries}.
 *
 * <p>Mọi cột {@code updatable = false}: sổ chỉ-ghi-thêm, không có đường sửa nào.
 */
@Entity
@Table(
    name = "school_ai_spend_entries",
    indexes = {
        @Index(name = "idx_school_ai_spend_entries_school_occurred", columnList = "school_id, occurred_at")
    },
    check = {
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_quota_type_valid",
            constraint = "quota_type IN ('EXAM', 'PRACTICE')"
        ),
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_amount_positive",
            constraint = "amount_vnd > 0"
        ),
        @CheckConstraint(
            name = "chk_school_ai_spend_entries_source_matches_quota_type",
            constraint = "(quota_type = 'EXAM' AND exam_session_id IS NOT NULL AND practice_session_id IS NULL)"
                + " OR (quota_type = 'PRACTICE' AND practice_session_id IS NOT NULL AND exam_session_id IS NULL)"
        )
    }
)
public class SchoolAiSpendEntryJpaEntity {

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

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "quota_type", nullable = false, updatable = false, length = 20)
    private String quotaType;

    /** NULL = khoản chi của cả trường (kỳ thi tập trung), không phải dữ liệu thiếu. */
    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "exam_session_id", updatable = false)
    private UUID examSessionId;

    @Column(name = "practice_session_id", updatable = false)
    private UUID practiceSessionId;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 18, scale = 6)
    private BigDecimal amountVnd;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SchoolAiSpendEntryJpaEntity() {
    }

    public SchoolAiSpendEntryJpaEntity(UUID schoolId, UUID subscriptionId, String quotaType, UUID userId,
            UUID examSessionId, UUID practiceSessionId, BigDecimal amountVnd, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.examSessionId = examSessionId;
        this.practiceSessionId = practiceSessionId;
        this.amountVnd = amountVnd;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public String getQuotaType() {
        return quotaType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
