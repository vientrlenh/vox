package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;
import com.sep.vox.infrastructure.properties.QuotaSellingPriceProperties;
import com.sep.vox.infrastructure.service.QuotaPricingService;

class QuotaPricingServiceTests {

    @Test
    void usdToVndRateUsesLatestSnapshotWhenPresent() {
        QuotaPricingCalibrationRepository quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        ExchangeRateSnapshotRepository exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        QuotaPricingProperties quotaPricingProperties = new QuotaPricingProperties(null, null);
        QuotaSellingPriceProperties quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("25500"), new BigDecimal("0.05"));
        when(exchangeRateSnapshotRepository.findLatest(CurrencyCode.USD)).thenReturn(
            Optional.of(new ExchangeRateSnapshot(CurrencyCode.USD, new BigDecimal("26777"), Instant.now(), "https://example.test")));

        QuotaPricingService service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("26777"));
    }

    @Test
    void usdToVndRateFallsBackToConfiguredMarketRateWhenNoSnapshot() {
        QuotaPricingCalibrationRepository quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        ExchangeRateSnapshotRepository exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        QuotaPricingProperties quotaPricingProperties = new QuotaPricingProperties(null, null);
        QuotaSellingPriceProperties quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("25500"), new BigDecimal("0.05"));
        when(exchangeRateSnapshotRepository.findLatest(CurrencyCode.USD)).thenReturn(Optional.empty());

        QuotaPricingService service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("25500"));
    }

    @Test
    void usdToVndRateFallsBackToPlaceholderDefaultWhenUnset() {
        QuotaPricingCalibrationRepository quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        ExchangeRateSnapshotRepository exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        QuotaPricingProperties quotaPricingProperties = new QuotaPricingProperties(null, null);
        QuotaSellingPriceProperties quotaSellingPriceProperties = new QuotaSellingPriceProperties(null, null);
        when(exchangeRateSnapshotRepository.findLatest(CurrencyCode.USD)).thenReturn(Optional.empty());

        QuotaPricingService service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("26000"));
    }

}
