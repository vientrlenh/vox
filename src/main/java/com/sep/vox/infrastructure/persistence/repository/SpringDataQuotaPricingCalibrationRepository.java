package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.QuotaPricingCalibrationJpaEntity;

public interface SpringDataQuotaPricingCalibrationRepository
        extends JpaRepository<QuotaPricingCalibrationJpaEntity, UUID> {
    Optional<QuotaPricingCalibrationJpaEntity> findFirstByOrderByComputedAtDesc();
}
