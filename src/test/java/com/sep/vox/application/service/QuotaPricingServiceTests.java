package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.QuotaPricingService;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;
import com.sep.vox.infrastructure.properties.QuotaSellingPriceProperties;

class QuotaPricingServiceTests {

    @Test
    void usdToVndRateReturnsConfiguredMarketRate() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(new BigDecimal("25500"));

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, quotaPricingProperties, quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("25500"));
    }

    @Test
    void usdToVndRateFallsBackToPlaceholderDefaultWhenUnset() {
        var quotaPricingCalibrationRepository = mock(QuotaPricingCalibrationRepository.class);
        var quotaPricingProperties = new QuotaPricingProperties(null, null);
        var quotaSellingPriceProperties = new QuotaSellingPriceProperties(null);

        var service = new QuotaPricingService(
            quotaPricingCalibrationRepository, quotaPricingProperties, quotaSellingPriceProperties);

        assertThat(service.usdToVndRate()).isEqualByComparingTo(new BigDecimal("26000"));
    }
}
