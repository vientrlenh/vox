package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.metering.QuotaPricingCalibration;
import com.sep.vox.domain.model.metering.QuotaPricingSource;

public interface QuotaPricingCalibrationRepository {
    QuotaPricingCalibration save(QuotaPricingCalibration calibration);

    /** The latest calibration computed successfully (enough samples) for this source -- empty if there has never been one. */
    Optional<QuotaPricingCalibration> findLatest(QuotaPricingSource source);
}
