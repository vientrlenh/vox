package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.sep.vox.domain.common.ZoneConstant;

import com.sep.vox.domain.model.BaseModel;

public class SubscriptionPlan extends BaseModel {
    private String name;
    private String tagline;
    private BigDecimal priceVnd;
    private SubscriptionPlanPeriod periodType;
    private Integer periodCount;
    private Integer maxTimePerAttemptMin;
    private SubscriptionPlanStatus status;
    private Long version;
    private UUID replacedByPlanId;

    public SubscriptionPlan() {}



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public BigDecimal getPriceVnd() {
        return priceVnd;
    }

    public void setPriceVnd(BigDecimal priceVnd) {
        this.priceVnd = priceVnd;
    }

    public SubscriptionPlanPeriod getPeriodType() {
        return periodType;
    }

    public void setPeriodType(SubscriptionPlanPeriod periodType) {
        this.periodType = periodType;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(Integer periodCount) {
        this.periodCount = periodCount;
    }

    public Integer getMaxTimePerAttemptMin() {
        return maxTimePerAttemptMin;
    }

    public void setMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
    }

    public SubscriptionPlanStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionPlanStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UUID getReplacedByPlanId() {
        return replacedByPlanId;
    }

    public void setReplacedByPlanId(UUID replacedByPlanId) {
        this.replacedByPlanId = replacedByPlanId;
    }

    public static SubscriptionPlan create(String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount, Integer maxTimePerAttemptMin) {
        return builder()
            .name(name)
            .tagline(tagline)
            .priceVnd(priceVnd)
            .periodType(periodType)
            .periodCount(periodCount)
            .maxTimePerAttemptMin(maxTimePerAttemptMin)
            .status(SubscriptionPlanStatus.DRAFT)
            .build();
    }

    /**
     * The moment the plan expires if it starts running from {@code start}.
     *
     * <p>Added by the CALENDAR, not by a fixed number of days: a 1-month plan bought on 31/01 must expire on 28/02,
     * whereas adding 30 days gives 02/03 -- the school would get two extra days in every short month. This is also why
     * periodType/periodCount replaced the old validityDays.
     *
     * <p>Converted to the business time zone before adding because "one month" is a local-calendar concept: adding on UTC
     * would shift the date for every moment that falls between 17:00 and 23:59 Vietnam time.
     */
    public Instant endDateFrom(Instant start) {
        if (start == null || periodType == null || periodCount == null) {
            throw new IllegalStateException("Gói thiếu chu kỳ (periodType/periodCount), không tính được hạn dùng");
        }
        ZonedDateTime local = start.atZone(ZoneConstant.BUSINESS_ZONE);
        ZonedDateTime end = switch (periodType) {
            case DAY -> local.plusDays(periodCount);
            case MONTH -> local.plusMonths(periodCount);
            case YEAR -> local.plusYears(periodCount);
        };
        return end.toInstant();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<SubscriptionPlan, Builder> {

        private Builder() {
            super(new SubscriptionPlan());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder tagline(String tagline) {
            target.tagline = tagline;
            return this;
        }

        public Builder priceVnd(BigDecimal priceVnd) {
            target.priceVnd = priceVnd;
            return this;
        }

        public Builder periodType(SubscriptionPlanPeriod periodType) {
            target.periodType = periodType;
            return this;
        }

        public Builder periodCount(Integer periodCount) {
            target.periodCount = periodCount;
            return this;
        }

        public Builder maxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
            target.maxTimePerAttemptMin = maxTimePerAttemptMin;
            return this;
        }

        public Builder status(SubscriptionPlanStatus status) {
            target.status = status;
            return this;
        }

        public Builder version(Long version) {
            target.version = version;
            return this;
        }

        public Builder replacedByPlanId(UUID replacedByPlanId) {
            target.replacedByPlanId = replacedByPlanId;
            return this;
        }

    }
}
