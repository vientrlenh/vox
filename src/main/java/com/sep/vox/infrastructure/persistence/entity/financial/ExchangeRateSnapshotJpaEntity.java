package com.sep.vox.infrastructure.persistence.entity.financial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
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
@Table(name = "exchange_rate_snapshots")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRateSnapshotJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "currency_code", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exchange_rate_snapshots_currency_code_valid", 
            constraint = "currency_code = 'USD'"
        )
    })
    private String currencyCode;

    // An exchange rate is a MULTIPLIER, not money -- it shares numeric(12,4) with every fx_rate_used column
    // (school_balance_entries, ai_usage_records) so that one concept has exactly one numeric shape.
    @Column(name = "exchange_rate_to_vnd", nullable = false, updatable = false, precision = 12, scale = 4)
    private BigDecimal exchangeRateToVnd;

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt;

    @Column(name = "source_url", nullable = false, updatable = false)
    private String sourceUrl;

    
}
