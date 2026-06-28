package com.sep.vox.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.FrameworkJpaEntity;

public interface SpringDataFrameworkRepository extends JpaRepository<FrameworkJpaEntity, UUID> {
    Optional<FrameworkJpaEntity> findByCode(String code);

}
