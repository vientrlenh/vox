package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.infrastructure.client.ExchangeRateApiClient;
import com.sep.vox.infrastructure.properties.ExchangeRateApiProperties;

/**
 * Orchestrate 1 lần fetch tỷ giá USD->VND thật cho ExchangeRateRefreshJob. Không làm mượt/kẹp biên
 * theo lịch sử như QuotaPricingCalibrationService (đó là số tính từ dữ liệu nội bộ, cần smoothing
 * để tránh nhảy vọt theo khối lượng dữ liệu) -- ở đây chỉ là 1 con số bên ngoài, ExchangeRateApiClient
 * đã tự validate biên an toàn trước khi trả về, nên fetch được là lưu thẳng.
 */
@Service
public class ExchangeRateRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateRefreshService.class);

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;
    private final ExchangeRateApiProperties exchangeRateApiProperties;

    public ExchangeRateRefreshService(
            ExchangeRateApiClient exchangeRateApiClient,
            ExchangeRateSnapshotRepository exchangeRateSnapshotRepository,
            ExchangeRateApiProperties exchangeRateApiProperties) {
        this.exchangeRateApiClient = exchangeRateApiClient;
        this.exchangeRateSnapshotRepository = exchangeRateSnapshotRepository;
        this.exchangeRateApiProperties = exchangeRateApiProperties;
    }

    public void refresh() {
        Optional<BigDecimal> rate = exchangeRateApiClient.fetchUsdToVndRate();
        if (rate.isEmpty()) {
            LOGGER.info("[exchange-rate] Bỏ qua lần refresh này -- giữ nguyên snapshot mới nhất hiện có");
            return;
        }

        // This job only fetches the USD rate (ExchangeRateApiProperties.baseUrl points to .../latest/USD), so
        // currency_code is hard-coded to USD -- adding a second currency means fetching separately, not attaching
        // another label to the same number.
        ExchangeRateSnapshot snapshot = new ExchangeRateSnapshot(
            CurrencyCode.USD, rate.get(), Instant.now(), exchangeRateApiProperties.baseUrl());
        exchangeRateSnapshotRepository.save(snapshot);
        LOGGER.info("[exchange-rate] Đã lưu snapshot tỷ giá mới: {}", rate.get());
    }
}
