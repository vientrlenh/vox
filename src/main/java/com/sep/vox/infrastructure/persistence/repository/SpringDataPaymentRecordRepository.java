package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.PaymentRecordJpaEntity;

public interface SpringDataPaymentRecordRepository extends JpaRepository<PaymentRecordJpaEntity, UUID> {
    List<PaymentRecordJpaEntity> findByOrderId(UUID orderId);
    Optional<PaymentRecordJpaEntity> findByProviderAndProviderOrderRef(String provider, String providerOrderRef);
    Optional<PaymentRecordJpaEntity> findByOrderIdAndStatus(UUID orderId, String status);
    List<PaymentRecordJpaEntity> findByStatus(String status);
    long countByOrderId(UUID orderId);
}
