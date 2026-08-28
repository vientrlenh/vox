package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.InvoiceJpaEntity;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {
    Optional<InvoiceJpaEntity> findByOrderId(UUID orderId);
    List<InvoiceJpaEntity> findByOrderIdIn(Collection<UUID> orderIds);
    Optional<InvoiceJpaEntity> findByInvoiceNumber(String invoiceNumber);
    boolean existsByOrderId(UUID orderId);
}
