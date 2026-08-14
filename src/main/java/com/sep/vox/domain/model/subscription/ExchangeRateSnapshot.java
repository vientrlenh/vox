package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExchangeRateSnapshot {
    private UUID id;
    // Thời điểm ExchangeRateRefreshJob lấy được row này -- dùng để lấy row mới nhất
    // (ORDER BY fetched_at DESC).
    private Instant fetchedAt;
    private BigDecimal usdToVndRate;
    // Nguồn đã dùng để fetch row này (vd base URL của API tỷ giá) -- phòng khi sau này đổi provider,
    // vẫn tra được row cũ lấy từ đâu.
    private String source;

    public ExchangeRateSnapshot() {}

    public ExchangeRateSnapshot(UUID id, Instant fetchedAt, BigDecimal usdToVndRate, String source) {
        this.id = id;
        this.fetchedAt = fetchedAt;
        this.usdToVndRate = usdToVndRate;
        this.source = source;
    }

    public ExchangeRateSnapshot(Instant fetchedAt, BigDecimal usdToVndRate, String source) {
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
