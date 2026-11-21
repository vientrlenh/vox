package com.sep.vox.domain.model.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class QuotaPricingCalibration {
    private UUID id;
    // The moment the job computed this row -- used to pick the latest row (ORDER BY computed_at DESC).
    private Instant computedAt;
    // The trailing window (number of days looked back) used WHEN this row was computed -- stored so the history shows how
    // many days the configuration used, in case .env is changed later.
    // E.g. "only collect sessions that took place WITHIN the last 90 days, skip older sessions".
    private int windowDays;
    // Number of sessions ACTUALLY used for the computation -- only sessions that match both cost_usd and
    // duration_seconds are counted (missing either one excludes the session from the sample).
    private int sessionCount;
    // Sum of cost_usd accumulated from exactly the sessionCount sessions above.
    private BigDecimal totalCostUsd;
    // Sum of REAL answered seconds (not configured seconds) accumulated from exactly those sessions.
    private long totalAnsweredSeconds;
    // The DIRECT number, with no processing: totalCostUsd / totalAnsweredSeconds.
    private BigDecimal rawRateUsdPerSecond;
    // The number ACTUALLY used by the guard (QuotaPricingService reads this field) -- rawRateUsdPerSecond after
    // smoothing (not deviating more than maxChangeRatio from the previous applied) and the safety bound
    // (clamped within [minRateBound, maxRateBound]).
    private BigDecimal appliedRateUsdPerSecond;
    // The reason applied differs from raw (smoothed or bound-clamped) -- null if the two numbers are equal and no
    // adjustment was needed.
    private String note;
    // The data source used to calibrate this row -- ASSESSMENT (join assessment_item_responses) or PRACTICE
    // (join practice_response_turns), see QuotaPricingSource.
    private QuotaPricingSource pricingSource;

    public QuotaPricingCalibration() {}



    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public BigDecimal getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(BigDecimal totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public long getTotalAnsweredSeconds() {
        return totalAnsweredSeconds;
    }

    public void setTotalAnsweredSeconds(long totalAnsweredSeconds) {
        this.totalAnsweredSeconds = totalAnsweredSeconds;
    }

    public BigDecimal getRawRateUsdPerSecond() {
        return rawRateUsdPerSecond;
    }

    public void setRawRateUsdPerSecond(BigDecimal rawRateUsdPerSecond) {
        this.rawRateUsdPerSecond = rawRateUsdPerSecond;
    }

    public BigDecimal getAppliedRateUsdPerSecond() {
        return appliedRateUsdPerSecond;
    }

    public void setAppliedRateUsdPerSecond(BigDecimal appliedRateUsdPerSecond) {
        this.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public QuotaPricingSource getPricingSource() {
        return pricingSource;
    }

    public void setPricingSource(QuotaPricingSource pricingSource) {
        this.pricingSource = pricingSource;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final QuotaPricingCalibration target = new QuotaPricingCalibration();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder computedAt(Instant computedAt) {
            target.computedAt = computedAt;
            return this;
        }

        public Builder windowDays(int windowDays) {
            target.windowDays = windowDays;
            return this;
        }

        public Builder sessionCount(int sessionCount) {
            target.sessionCount = sessionCount;
            return this;
        }

        public Builder totalCostUsd(BigDecimal totalCostUsd) {
            target.totalCostUsd = totalCostUsd;
            return this;
        }

        public Builder totalAnsweredSeconds(long totalAnsweredSeconds) {
            target.totalAnsweredSeconds = totalAnsweredSeconds;
            return this;
        }

        public Builder rawRateUsdPerSecond(BigDecimal rawRateUsdPerSecond) {
            target.rawRateUsdPerSecond = rawRateUsdPerSecond;
            return this;
        }

        public Builder appliedRateUsdPerSecond(BigDecimal appliedRateUsdPerSecond) {
            target.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
            return this;
        }

        public Builder note(String note) {
            target.note = note;
            return this;
        }

        public Builder pricingSource(QuotaPricingSource pricingSource) {
            target.pricingSource = pricingSource;
            return this;
        }

        public QuotaPricingCalibration build() {
            return target;
        }

    }
}
