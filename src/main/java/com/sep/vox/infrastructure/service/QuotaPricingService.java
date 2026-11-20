package com.sep.vox.infrastructure.service;

import java.math.BigDecimal;

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
 * Nguồn ĐỌC duy nhất cho pricing-related config -- estimatedCostPerExamSecondUsd (ưu tiên giá đã tự
 * calibrate từ dữ liệu thật qua QuotaPricingCalibrationService/Job, fallback về hằng số tĩnh trong
 * .env/QuotaPricingProperties khi chưa có lần calibrate nào thành công) và usdToVndRate (tỷ giá thị
 * trường, dùng chung mọi gói -- xem QuotaSellingPriceProperties). Dùng ở cả
 * ClassTestTokenQuotaGuardService (chặn trước) lẫn SubscriptionController (query quotaPricing cho FE)
 * để mọi nơi luôn thấy CÙNG 1 giá trị.
 *
 * <p>Nằm ở infrastructure vì đọc trực tiếp @ConfigurationProperties; phía application/interfaces gọi
 * qua QuotaPricingPort để không phụ thuộc ngược lên tầng này.
 */
@Service
public class QuotaPricingService implements QuotaPricingPort, ServiceFeePort {

    private final QuotaPricingCalibrationRepository quotaPricingCalibrationRepository;
    private final ExchangeRateSnapshotRepository exchangeRateSnapshotRepository;
    private final QuotaPricingProperties quotaPricingProperties;
    private final QuotaSellingPriceProperties quotaSellingPriceProperties;

    public QuotaPricingService(
            QuotaPricingCalibrationRepository quotaPricingCalibrationRepository,
            ExchangeRateSnapshotRepository exchangeRateSnapshotRepository,
            QuotaPricingProperties quotaPricingProperties,
            QuotaSellingPriceProperties quotaSellingPriceProperties) {
        this.quotaPricingCalibrationRepository = quotaPricingCalibrationRepository;
        this.exchangeRateSnapshotRepository = exchangeRateSnapshotRepository;
        this.quotaPricingProperties = quotaPricingProperties;
        this.quotaSellingPriceProperties = quotaSellingPriceProperties;
    }

    @Override
    public BigDecimal currentEstimatedCostPerExamSecondUsd() {
        return quotaPricingCalibrationRepository.findLatest(QuotaPricingSource.EXAM)
            .map(calibration -> calibration.getAppliedRateUsdPerSecond())
            .orElseGet(quotaPricingProperties::estimatedCostPerExamSecondUsd);
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
