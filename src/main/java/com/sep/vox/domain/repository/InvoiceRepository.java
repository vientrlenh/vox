package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.Invoice;
import com.sep.vox.domain.model.subscription.InvoiceStatus;

public interface InvoiceRepository {
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByIdForUpdate(UUID id);
    Invoice save(Invoice invoice);
    List<Invoice> findAllBySubscriptionId(UUID subscriptionId);
    List<Invoice> findAllBySubscriptionIdIn(Collection<UUID> subscriptionIds);
    Optional<Invoice> findByPayosOrderCode(Long payosOrderCode);
    List<Invoice> findAllByStatus(InvoiceStatus status);
}
