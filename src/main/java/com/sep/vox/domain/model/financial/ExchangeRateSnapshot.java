package com.sep.vox.domain.model.financial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExchangeRateSnapshot {
    private UUID id;
    private CurrencyCode currencyCode;
    private BigDecimal exchangeRateToVnd;
    // Thời điểm ExchangeRateRefreshJob lấy được row này -- dùng để lấy row mới nhất
    // (ORDER BY fetched_at DESC).
    private Instant fetchedAt;
    // Nguồn đã dùng để fetch row này (vd base URL của API tỷ giá) -- phòng khi sau này đổi provider,
    // vẫn tra được row cũ lấy từ đâu.
    private String sourceUrl;

    public ExchangeRateSnapshot() {}

    public ExchangeRateSnapshot(UUID id, CurrencyCode currencyCode, BigDecimal exchangeRateToVnd, Instant fetchedAt, String sourceUrl) {
        this.id = id;
        this.currencyCode = currencyCode;
        this.exchangeRateToVnd = exchangeRateToVnd;
        this.fetchedAt = fetchedAt;
        this.sourceUrl = sourceUrl;
    }

    public ExchangeRateSnapshot(CurrencyCode currencyCode, BigDecimal exchangeRateToVnd, Instant fetchedAt, String sourceUrl) {
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

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getExchangeRateToVnd() {
        return exchangeRateToVnd;
    }

    public void setExchangeRateToVnd(BigDecimal exchangeRateToVnd) {
        this.exchangeRateToVnd = exchangeRateToVnd;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    
}
