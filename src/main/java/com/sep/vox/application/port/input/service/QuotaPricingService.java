package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaPricingCalibration;
import com.sep.vox.domain.model.subscription.QuotaPricingSource;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;
import com.sep.vox.infrastructure.properties.QuotaSellingPriceProperties;

/**
 * Nguồn ĐỌC duy nhất cho pricing-related config -- estimatedCostPerExamSecondUsd (ưu tiên giá đã tự
 * calibrate từ dữ liệu thật qua QuotaPricingCalibrationService/Job, fallback về hằng số tĩnh trong
 * .env/QuotaPricingProperties khi chưa có lần calibrate nào thành công) và usdToVndRate (tỷ giá thị
 * trường, dùng chung mọi gói -- xem QuotaSellingPriceProperties). Dùng ở cả
 * ClassTestTokenQuotaGuardService (chặn trước) lẫn SubscriptionController (query quotaPricing cho FE)
 * để mọi nơi luôn thấy CÙNG 1 giá trị.
 */
@Service
public class QuotaPricingService {

    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final QuotaPricingProperties quotaPricingProperties;
    private final QuotaSellingPriceProperties quotaSellingPriceProperties;

    public QuotaPricingService(
            QuotaPricingCalibrationRepository quotaPricingCalibrationRepository,
            QuotaPricingProperties quotaPricingProperties,
            QuotaSellingPriceProperties quotaSellingPriceProperties) {
        this.quotaPricingCalibrationRepository = quotaPricingCalibrationRepository;
        this.quotaPricingProperties = quotaPricingProperties;
        this.quotaSellingPriceProperties = quotaSellingPriceProperties;
    }

    public BigDecimal currentEstimatedCostPerExamSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.EXAM)
            .map(QuotaPricingCalibration::getAppliedRateUsdPerSecond)
            .orElseGet(quotaPricingProperties::estimatedCostPerExamSecondUsd);
    }

    public BigDecimal currentEstimatedCostPerPracticeSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.PRACTICE)
            .map(QuotaPricingCalibration::getAppliedRateUsdPerSecond)
            .orElseGet(quotaPricingProperties::estimatedCostPerPracticeSecondUsd);
    }

    public BigDecimal usdToVndRate() {
        return quotaSellingPriceProperties.usdToVndRate();
    }
}
