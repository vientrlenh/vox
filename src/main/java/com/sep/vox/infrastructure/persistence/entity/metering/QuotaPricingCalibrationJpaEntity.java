package com.sep.vox.infrastructure.persistence.entity.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "quota_pricing_calibrations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuotaPricingCalibrationJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
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
}
