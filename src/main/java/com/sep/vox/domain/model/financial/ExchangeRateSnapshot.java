package com.sep.vox.domain.model.financial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExchangeRateSnapshot {
    private UUID id;
    private CurrencyCode currencyCode;
    private BigDecimal exchangeRateToVnd;
    // The moment ExchangeRateRefreshJob obtained this row -- used to pick the latest row
    // (ORDER BY fetched_at DESC).
    private Instant fetchedAt;
    // The source used to fetch this row (e.g. the base URL of the exchange-rate API) -- in case the provider is changed
    // later, the origin of an old row can still be traced.
    private String sourceUrl;

    public ExchangeRateSnapshot() {}



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

    

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ExchangeRateSnapshot target = new ExchangeRateSnapshot();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder currencyCode(CurrencyCode currencyCode) {
            target.currencyCode = currencyCode;
            return this;
        }

        public Builder exchangeRateToVnd(BigDecimal exchangeRateToVnd) {
            target.exchangeRateToVnd = exchangeRateToVnd;
            return this;
        }

        public Builder fetchedAt(Instant fetchedAt) {
            target.fetchedAt = fetchedAt;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            target.sourceUrl = sourceUrl;
            return this;
        }

        public ExchangeRateSnapshot build() {
            return target;
        }

    }
}
