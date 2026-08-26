package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanJpaEntity;

public interface SpringDataSubscriptionPlanRepository extends JpaRepository<SubscriptionPlanJpaEntity, UUID> {
    List<SubscriptionPlanJpaEntity> findByStatus(String status);
    Page<SubscriptionPlanJpaEntity> findByStatus(String status, Pageable pageable);
}
