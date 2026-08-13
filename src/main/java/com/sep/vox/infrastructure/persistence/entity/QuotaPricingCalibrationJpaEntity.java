package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "quota_pricing_calibration")
public class QuotaPricingCalibrationJpaEntity {

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

    @Column(name = "computed_at", nullable = false, updatable = false)
    private Instant computedAt;

    @Column(name = "window_days", nullable = false, updatable = false)
    private int windowDays;

    @Column(name = "session_count", nullable = false, updatable = false)
    private int sessionCount;

    @Column(name = "total_cost_usd", nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal totalCostUsd;

    @Column(name = "total_answered_seconds", nullable = false, updatable = false)
    private long totalAnsweredSeconds;

    @Column(name = "raw_rate_usd_per_second", nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal rawRateUsdPerSecond;

    @Column(name = "applied_rate_usd_per_second", nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal appliedRateUsdPerSecond;

    @Column(name = "note", updatable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "pricing_source", nullable = false, updatable = false, length = 16)
    private String pricingSource;

    protected QuotaPricingCalibrationJpaEntity() {}

    public QuotaPricingCalibrationJpaEntity(UUID id, Instant computedAt, int windowDays, int sessionCount,
            BigDecimal totalCostUsd, long totalAnsweredSeconds, BigDecimal rawRateUsdPerSecond,
            BigDecimal appliedRateUsdPerSecond, String note, String pricingSource) {
        this.id = id;
        this.computedAt = computedAt;
        this.windowDays = windowDays;
        this.sessionCount = sessionCount;
        this.totalCostUsd = totalCostUsd;
        this.totalAnsweredSeconds = totalAnsweredSeconds;
        this.rawRateUsdPerSecond = rawRateUsdPerSecond;
        this.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
        this.note = note;
        this.pricingSource = pricingSource;
    }

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

    public String getPricingSource() {
        return pricingSource;
    }

    public void setPricingSource(String pricingSource) {
        this.pricingSource = pricingSource;
    }
}
