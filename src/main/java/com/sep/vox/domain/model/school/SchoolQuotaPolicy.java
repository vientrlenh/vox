package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The quota distribution ceiling of a school, for ONE quota type.
 *
 * <p>Answers exactly one question: out of the quota wallet that the plan grants, what maximum percentage may the
 * school hand out to individual people. The rest is a RESERVE -- it is not held anywhere, it is simply the part of the
 * wallet that nobody has a spending ceiling to spend from, kept to grant extra mid-period to whoever really needs it.
 *
 * <p>Belongs to the SCHOOL, not to the subscription period: the quota record is rebuilt every period (see
 * {@code OrderSettlementService.seedQuotaRecords}), so putting the policy there would make it disappear after every
 * renewal. This is the same reason the self-funded balance lives in {@link SchoolBalance} rather than per period.
 */
public class SchoolQuotaPolicy {

    /** The whole wallet can be distributed -- the default, and the behaviour of the system before this policy existed. */
    public static final BigDecimal FULLY_DISTRIBUTABLE = BigDecimal.ONE;

    private UUID id;
    private UUID schoolId;
    private QuotaType quotaType;
    /** From 0 to 1. The database has a CHECK that keeps it within this range. */
    private BigDecimal distributableRatio;
    private Instant createdAt;
    private Instant updatedAt;

    public SchoolQuotaPolicy() {}



    /**
     * The default policy for a school that has never set anything -- everything can be distributed.
     *
     * <p>Do NOT write this row to the database on the read path: having no row and having a row with ratio 1.0 mean the
     * same thing, so creating it just for reading would spawn a row for every school merely because someone opened a page.
     */
    public static SchoolQuotaPolicy fullyDistributable(UUID schoolId, QuotaType quotaType) {
        return builder()
            .schoolId(schoolId)
            .quotaType(quotaType)
            .distributableRatio(FULLY_DISTRIBUTABLE)
            .build();
    }

    /**
     * The part of the wallet that may be handed out to individual people.
     *
     * <p>Rounded DOWN to the scale of the money column (6 decimal places): rounding up would allow distributing a few
     * millionths of a dong more than the ceiling, which breaks the very constraint this policy sets.
     */
    public BigDecimal distributableAmountOf(BigDecimal totalAllocatedVnd) {
        if (totalAllocatedVnd == null) {
            return BigDecimal.ZERO;
        }
        return totalAllocatedVnd
            .multiply(distributableRatio == null ? FULLY_DISTRIBUTABLE : distributableRatio)
            .setScale(6, java.math.RoundingMode.DOWN);
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

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getDistributableRatio() {
        return distributableRatio;
    }

    public void setDistributableRatio(BigDecimal distributableRatio) {
        this.distributableRatio = distributableRatio;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolQuotaPolicy target = new SchoolQuotaPolicy();

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

        public Builder quotaType(QuotaType quotaType) {
            target.quotaType = quotaType;
            return this;
        }

        public Builder distributableRatio(BigDecimal distributableRatio) {
            target.distributableRatio = distributableRatio;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            target.updatedAt = updatedAt;
            return this;
        }

        public SchoolQuotaPolicy build() {
            return target;
        }

    }
}
