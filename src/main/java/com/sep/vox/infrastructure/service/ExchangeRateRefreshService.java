package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.infrastructure.client.ExchangeRateApiClient;
import com.sep.vox.infrastructure.properties.ExchangeRateApiProperties;

/**
 * Orchestrates ONE fetch of the real USD->VND exchange rate for ExchangeRateRefreshJob. It does not smooth/clamp against
 * history like QuotaPricingCalibrationService (that number is computed from internal data and needs smoothing to avoid
 * jumping with the data volume) -- here it is just one external number, and ExchangeRateApiClient already validates the
 * safe bounds before returning, so whatever is fetched is stored directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateRefreshService {

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;
    private final ExchangeRateApiProperties exchangeRateApiProperties;

    public void refresh() {
        Optional<BigDecimal> rate = exchangeRateApiClient.fetchUsdToVndRate();
        if (rate.isEmpty()) {
            log.info("[exchange-rate] Bỏ qua lần refresh này -- giữ nguyên snapshot mới nhất hiện có");
            return;
        }

        // This job only fetches the USD rate (ExchangeRateApiProperties.baseUrl points to .../latest/USD), so
        // currency_code is hard-coded to USD -- adding a second currency means fetching separately, not attaching
        // another label to the same number.
        ExchangeRateSnapshot snapshot = ExchangeRateSnapshot.builder()
            .currencyCode(CurrencyCode.USD)
            .exchangeRateToVnd(rate.get())
            .fetchedAt(Instant.now())
            .sourceUrl(exchangeRateApiProperties.baseUrl())
            .build();
        exchangeRateSnapshotRepository.save(snapshot);
        log.info("[exchange-rate] Đã lưu snapshot tỷ giá mới: {}", rate.get());
    }
}
