package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.ServiceFeePort;
import com.sep.vox.domain.model.metering.QuotaPricingSource;
import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;
import com.sep.vox.infrastructure.properties.QuotaSellingPriceProperties;

/**
 * The only READ source for pricing-related config -- estimatedCostPerAssessmentSecondUsd (preferring the price automatically
 * calibrated from real data through QuotaPricingCalibrationService/Job, falling back to the static constant in
 * .env/QuotaPricingProperties when no calibration has succeeded yet) and usdToVndRate (the market
 * exchange rate, shared by every plan -- see QuotaSellingPriceProperties). Used by both
 * ClassTestTokenQuotaGuardService (the pre-check) and GraphQLSubscriptionController (the quotaPricing query for the FE)
 * so that every place always sees the SAME value.
 *
 * <p>It sits in infrastructure because it reads @ConfigurationProperties directly; the application/interfaces side calls
 * through QuotaPricingPort so it does not depend back up on this layer.
 */
@Service
@RequiredArgsConstructor
public class QuotaPricingService implements QuotaPricingPort, ServiceFeePort {

    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;
    private final QuotaPricingProperties quotaPricingProperties;
    private final QuotaSellingPriceProperties quotaSellingPriceProperties;

    @Override
    public BigDecimal currentEstimatedCostPerAssessmentSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.ASSESSMENT)
            .map(calibration -> calibration.getAppliedRateUsdPerSecond())
            .orElseGet(quotaPricingProperties::estimatedCostPerAssessmentSecondUsd);
    }

    @Override
    public BigDecimal currentEstimatedCostPerPracticeSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.PRACTICE)
            .map(calibration -> calibration.getAppliedRateUsdPerSecond())
            .orElseGet(quotaPricingProperties::estimatedCostPerPracticeSecondUsd);
    }

    @Override
    public BigDecimal usdToVndRate() {
        return exchangeRateSnapshotRepository.findLatest(CurrencyCode.USD)
            .map(snapshot -> snapshot.getExchangeRateToVnd())
            .orElseGet(quotaSellingPriceProperties::usdToVndRate);
    }

    /**
     * Phí dịch vụ đọc thẳng từ config, KHÔNG nhân sẵn vào tỷ giá -- đơn hàng tự cộng thành một dòng
     * riêng. Xem ServiceFeePort#serviceFeeRatio.
     */
    @Override
    public BigDecimal serviceFeeRatio() {
        return quotaSellingPriceProperties.serviceFeeRatio();
    }
}
