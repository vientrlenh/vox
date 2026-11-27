package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;

public interface ExchangeRateSnapshotRepository {
    ExchangeRateSnapshot save(ExchangeRateSnapshot snapshot);

    /**
     * The latest successful fetch FOR EXACTLY THIS CURRENCY -- empty if there has never been one.
     *
     * <p>Filtering by currencyCode is mandatory instead of taking the newest record blindly: the table stores rates converting to VND
     * for MULTIPLE currencies (column currency_code), so "latest" without a condition returns whichever currency
     * the job ran last. Today there is only USD so nothing is wrong yet, but once a second currency is added the quota
     * selling price silently jumps to the rate of that currency -- no error is reported.
     */
    Optional<ExchangeRateSnapshot> findLatest(CurrencyCode currencyCode);
}
