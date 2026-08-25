package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;

public interface QuotaPricingCalibrationRepository {
    QuotaPricingCalibration save(QuotaPricingCalibration calibration);

    /** Lần calibrate gần nhất đã tính thành công (đủ mẫu) cho nguồn này -- rỗng nếu chưa có lần nào. */
    Optional<QuotaPricingCalibration> findLatest(QuotaPricingSource source);
}
