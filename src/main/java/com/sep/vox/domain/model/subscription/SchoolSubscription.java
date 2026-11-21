package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolSubscription {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionPlanId;
    private Instant startDate;
    private Instant endDate;
    private SchoolSubscriptionStatus status;
    private BigDecimal pricePaidSnapshot;
    private Instant cancelledAt;
    private Instant createdAt;
    private Long version;
    // Forced suspension (SUSPENDED) by the System Admin -- different from cancelledAt (which only turns off renewal). All
    // 3 are null when not suspended, and are reset to null when the suspension is lifted (UnsuspendSubscriptionUseCase) --
    // the history of who suspended/lifted when and why is stored durably in SchoolSubscriptionEvent, not in these 3 columns.
    private Instant suspendedAt;
    private String suspendedReason;
    private UUID suspendedBy;

    public SchoolSubscription() {}



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

    public UUID getSubscriptionPlanId() {
        return subscriptionPlanId;
    }

    public void setSubscriptionPlanId(UUID subscriptionPlanId) {
        this.subscriptionPlanId = subscriptionPlanId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public SchoolSubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolSubscriptionStatus status) {
        this.status = status;
    }

    public BigDecimal getPricePaidSnapshot() {
        return pricePaidSnapshot;
    }

    public void setPricePaidSnapshot(BigDecimal pricePaidSnapshot) {
        this.pricePaidSnapshot = pricePaidSnapshot;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(Instant suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public String getSuspendedReason() {
        return suspendedReason;
    }

    public void setSuspendedReason(String suspendedReason) {
        this.suspendedReason = suspendedReason;
    }

    public UUID getSuspendedBy() {
        return suspendedBy;
    }

    public void setSuspendedBy(UUID suspendedBy) {
        this.suspendedBy = suspendedBy;
    }

    /**
     * A NEW subscription period, created as soon as the order's money has arrived.
     *
     * <p>{@code startsAt} is what tells a new purchase from an early renewal:
     *
     * <ul>
     *   <li>The school has no period in force -> {@code startsAt = now}. NOT the time the order was placed: an order
     *       can wait up to 24 hours (Order.PENDING_TTL), and counting the term from the order time would eat exactly
     *       that waiting time from the school.
     *   <li>The school renews while the old period is still running -> {@code startsAt = the old period's endDate}. The
     *       new period CHAINS instead of overlapping: a school that paid for a full period must receive a full period.
     *       This is why the system needs no proration of leftover days -- no day is lost, so none has to be made up.
     * </ul>
     *
     * <p>A future period still carries status ACTIVE from the start, and that does NOT make it take effect early: every
     * place that asks for "the current period" goes through findActiveBySchoolId, which filters by date. The status here
     * answers "has it been paid", while the date range answers "is it running" -- keeping those two questions apart is
     * what lets an early renewal avoid an extra SCHEDULED status and a job to flip it.
     *
     * <p>pricePaidSnapshot freezes the price at purchase time -- the plan may change price or be ARCHIVED afterwards, but
     * an invoice already issued and a period already sold must keep the number the school actually paid.
     */
    public static SchoolSubscription activate(UUID schoolId, SubscriptionPlan plan, BigDecimal pricePaidVnd,
            Instant startsAt, Instant now) {
        return builder()
            .schoolId(schoolId)
            .subscriptionPlanId(plan.getId())
            .startDate(startsAt)
            .endDate(plan.endDateFrom(startsAt))
            .status(SchoolSubscriptionStatus.ACTIVE)
            .pricePaidSnapshot(pricePaidVnd)
            .createdAt(now)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SchoolSubscription target = new SchoolSubscription();

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

        public Builder subscriptionPlanId(UUID subscriptionPlanId) {
            target.subscriptionPlanId = subscriptionPlanId;
            return this;
        }

        public Builder startDate(Instant startDate) {
            target.startDate = startDate;
            return this;
        }

        public Builder endDate(Instant endDate) {
            target.endDate = endDate;
            return this;
        }

        public Builder status(SchoolSubscriptionStatus status) {
            target.status = status;
            return this;
        }

        public Builder pricePaidSnapshot(BigDecimal pricePaidSnapshot) {
            target.pricePaidSnapshot = pricePaidSnapshot;
            return this;
        }

        public Builder cancelledAt(Instant cancelledAt) {
            target.cancelledAt = cancelledAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder version(Long version) {
            target.version = version;
            return this;
        }

        public Builder suspendedAt(Instant suspendedAt) {
            target.suspendedAt = suspendedAt;
            return this;
        }

        public Builder suspendedReason(String suspendedReason) {
            target.suspendedReason = suspendedReason;
            return this;
        }

        public Builder suspendedBy(UUID suspendedBy) {
            target.suspendedBy = suspendedBy;
            return this;
        }

        public SchoolSubscription build() {
            return target;
        }

    }
}
