package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.InvoiceJpaEntity;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    List<InvoiceJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
    List<InvoiceJpaEntity> findAllBySubscriptionIdIn(Collection<UUID> subscriptionIds);
    Optional<InvoiceJpaEntity> findByPayosOrderCode(Long payosOrderCode);
    List<InvoiceJpaEntity> findAllByStatus(String status);
}
