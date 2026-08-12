package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaPricingCalibration;
import org.springframework.stereotype.Service;

import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.properties.QuotaPricingProperties;

/**
 * Nguồn ĐỌC duy nhất cho estimatedCostPerExamSecondUsd -- ưu tiên giá đã tự calibrate từ dữ liệu
 * thật (QuotaPricingCalibrationService/Job), fallback về hằng số tĩnh trong .env
 * (QuotaPricingProperties) khi chưa có lần calibrate nào thành công (mới deploy, hoặc chưa đủ
 * dữ liệu). Dùng ở cả ClassTestTokenQuotaGuardService (chặn trước) lẫn SubscriptionController
 * (query quotaPricing cho FE) để 2 nơi luôn thấy CÙNG 1 giá trị.
 */
@Service
public class QuotaPricingService {

    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final QuotaPricingProperties quotaPricingProperties;

    public QuotaPricingService(
            QuotaPricingCalibrationRepository quotaPricingCalibrationRepository,
            QuotaPricingProperties quotaPricingProperties) {
        this.quotaPricingCalibrationRepository = quotaPricingCalibrationRepository;
        this.quotaPricingProperties = quotaPricingProperties;
    }

    public BigDecimal currentEstimatedCostPerExamSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest()
            .map(QuotaPricingCalibration::getAppliedRateUsdPerSecond)
            .orElseGet(quotaPricingProperties::estimatedCostPerExamSecondUsd);
    }
}
