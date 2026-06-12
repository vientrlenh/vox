package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataFrameworkResultBandRepository extends JpaRepository<FrameworkResultBandJpaEntity, UUID> {
}
