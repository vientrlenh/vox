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
@Table(name = "exchange_rate_snapshots")
public class ExchangeRateSnapshotJpaEntity {

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

    @Column(name = "currency_code", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exchange_rate_snapshots_currency_code_valid", 
            constraint = "currency_code = 'USD'"
        )
    })
    private String currencyCode;

    @Column(name = "exchange_rate_to_vnd", nullable = false, updatable = false, precision = 18, scale = 2)
    private BigDecimal exchangeRateToVnd;

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt;

    @Column(name = "source_url", nullable = false, updatable = false)
    private String sourceUrl;

    protected ExchangeRateSnapshotJpaEntity() {}

    public ExchangeRateSnapshotJpaEntity(UUID id, String currencyCode, BigDecimal exchangeRateToVnd, Instant fetchedAt, String sourceUrl) {
        this.id = id;
        this.currencyCode = currencyCode;
        this.exchangeRateToVnd = exchangeRateToVnd;
        this.fetchedAt = fetchedAt;
        this.sourceUrl = sourceUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getExchangeRateToVnd() {
        return exchangeRateToVnd;
    }

    public void setExchangeRateToVnd(BigDecimal exchangeRateToVnd) {
        this.exchangeRateToVnd = exchangeRateToVnd;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    
}
