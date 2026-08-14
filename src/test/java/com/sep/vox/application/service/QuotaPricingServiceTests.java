package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.QuotaPricingService;
import com.sep.vox.domain.model.subscription.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;
import com.sep.vox.infrastructure.properties.QuotaSellingPriceProperties;

class QuotaPricingServiceTests {

    @Test
    void usdToVndRateUsesLatestSnapshotWhenPresent() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("25500"));
        when(exchangeRateSnapshotRepository.findLatest()).thenReturn(
            Optional.of(new ExchangeRateSnapshot(Instant.now(), new BigDecimal("26777"), "https://example.test")));

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("26777"));
    }

    @Test
    void usdToVndRateFallsBackToConfiguredMarketRateWhenNoSnapshot() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("25500"));
        when(exchangeRateSnapshotRepository.findLatest()).thenReturn(Optional.empty());

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("25500"));
    }

    @Test
    void usdToVndRateFallsBackToPlaceholderDefaultWhenUnset() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(null);
        when(exchangeRateSnapshotRepository.findLatest()).thenReturn(Optional.empty());

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("26000"));
    }

    @Test
    void tokenUnitPriceForAppliesServiceFeeRatioAndRoundsToWholeNumber() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var exchangeRateSnapshotRepository = mock(ExchangeRateSnapshotRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("26000"));
        when(exchangeRateSnapshotRepository.findLatest()).thenReturn(Optional.empty());

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, exchangeRateSnapshotRepository, quotaPricingProperties,
            quotaSellingPriceProperties);

        // 26000 * (1 + 0.20) = 31200
        assertThat(service.tokenUnitPriceFor(new BigDecimal("0.20"))).isEqualByComparingTo(new BigDecimal("31200"));
    }
}
