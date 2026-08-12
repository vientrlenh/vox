package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.subscription.QuotaPricingCalibration;
import com.sep.vox.domain.repository.QuotaPricingCalibrationRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuotaPricingCalibrationMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuotaPricingCalibrationRepository;

@Repository
public class QuotaPricingCalibrationRepositoryImpl implements QuotaPricingCalibrationRepository {

    private final SpringDataQuotaPricingCalibrationRepository springDataQuotaPricingCalibrationRepository;

    public QuotaPricingCalibrationRepositoryImpl(
            SpringDataQuotaPricingCalibrationRepository springDataQuotaPricingCalibrationRepository) {
        this.springDataQuotaPricingCalibrationRepository = springDataQuotaPricingCalibrationRepository;
    }

    @Override
    public QuotaPricingCalibration save(QuotaPricingCalibration calibration) {
        var saved = springDataQuotaPricingCalibrationRepository.save(QuotaPricingCalibrationMapper.toJpa(calibration));
        return QuotaPricingCalibrationMapper.toDomain(saved);
    }

    @Override
    public Optional<QuotaPricingCalibration> findLatest() {
        return springDataQuotaPricingCalibrationRepository.findFirstByOrderByComputedAtDesc()
            .map(QuotaPricingCalibrationMapper::toDomain);
    }
}
