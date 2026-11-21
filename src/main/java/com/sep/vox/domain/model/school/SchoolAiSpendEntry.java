package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * One time a school was charged for AI cost. Append-only ledger: never edited, never deleted.
 *
 * <p>Answers the question the other two money ledgers cannot -- how much the school has spent, per DAY, and on AI:
 *
 * <ul>
 *   <li>{@code ai_usage_records} is the PLATFORM's cost of goods, and since V9 it deliberately differs from what the
 *       school is charged (the charge for a failed grading run is waived). It also knows nothing about the speaking
 *       practice path.
 *   <li>{@code school_balance_entries} only records the spending that EXCEEDS the quota, so summing it misses exactly
 *       the largest part.
 * </ul>
 *
 * <p>Records the FULL amount of the charge, including the part that fits entirely within the quota.
 */
public class SchoolAiSpendEntry {

    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private UUID assessmentSessionId;
    private UUID practiceSessionId;
    private BigDecimal amountVnd;
    private Instant occurredAt;

    public SchoolAiSpendEntry() {
    }


    /**
     * A charge for an EXAM session.
     *
     * @param userId the author of the in-class test, or {@code null} for a centralized exam -- that exam is run by the
     *               school, so it does not belong to anyone's spending ceiling. null here is an ANSWER, not missing
     *               data.
     */
    public static SchoolAiSpendEntry forAssessment(UUID schoolId, UUID subscriptionId, UUID assessmentSessionId,
            UUID userId, BigDecimal amountVnd, Instant occurredAt) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .quotaType(QuotaType.ASSESSMENT)
            .userId(userId)
            .assessmentSessionId(assessmentSessionId)
            .amountVnd(amountVnd)
            .occurredAt(occurredAt)
            .build();
    }

    /** A charge for a SPEAKING PRACTICE session -- always belongs to the student who spoke. */
    public static SchoolAiSpendEntry forPractice(UUID schoolId, UUID subscriptionId,
            UUID practiceSessionId, UUID userId, BigDecimal amountVnd, Instant occurredAt) {
        return builder()
            .schoolId(schoolId)
            .subscriptionId(subscriptionId)
            .quotaType(QuotaType.PRACTICE)
            .userId(userId)
            .practiceSessionId(practiceSessionId)
            .amountVnd(amountVnd)
            .occurredAt(occurredAt)
            .build();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAssessmentSessionId() {
        return assessmentSessionId;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolAiSpendEntry target = new SchoolAiSpendEntry();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder schoolId(UUID schoolId) {
            target.schoolId = schoolId;
            return this;
        }

        public Builder subscriptionId(UUID subscriptionId) {
            target.subscriptionId = subscriptionId;
            return this;
        }

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder assessmentSessionId(UUID assessmentSessionId) {
            target.assessmentSessionId = assessmentSessionId;
            return this;
        }

        public Builder practiceSessionId(UUID practiceSessionId) {
            target.practiceSessionId = practiceSessionId;
            return this;
        }

        public Builder amountVnd(BigDecimal amountVnd) {
            target.amountVnd = amountVnd;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public SchoolAiSpendEntry build() {
            return target;
        }

    }
}
