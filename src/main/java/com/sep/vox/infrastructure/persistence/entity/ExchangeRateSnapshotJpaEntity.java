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
@Table(name = "exchange_rate_snapshot")
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

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt;

    @Column(name = "usd_to_vnd_rate", nullable = false, updatable = false, precision = 12, scale = 4)
    private BigDecimal usdToVndRate;

    @Column(name = "source", nullable = false, updatable = false)
    private String source;

    protected ExchangeRateSnapshotJpaEntity() {}

    public ExchangeRateSnapshotJpaEntity(UUID id, Instant fetchedAt, BigDecimal usdToVndRate, String source) {
        this.id = id;
        this.fetchedAt = fetchedAt;
        this.usdToVndRate = usdToVndRate;
        this.source = source;
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

    public BigDecimal getUsdToVndRate() {
        return usdToVndRate;
    }

    public void setUsdToVndRate(BigDecimal usdToVndRate) {
        this.usdToVndRate = usdToVndRate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
