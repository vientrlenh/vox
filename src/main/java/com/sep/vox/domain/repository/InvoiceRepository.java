package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.invoice.Invoice;

/**
 * An invoice is now a DOCUMENT issued after the money has arrived, and no longer carries the payment lifecycle (moved to
 * Order) or the gateway session (moved to PaymentRecord).
 *
 * <p>That is why the old methods were REMOVED, not forgotten:
 * <ul>
 *   <li>{@code findAllByStatus} / {@code sumAmountByStatus} -- an invoice no longer has a status column. PENDING
 *       orders needing reconciliation are asked of {@code OrderRepository.findAllByStatus}, revenue of
 *       {@code OrderRepository.sumTotalAmountByStatusInRange}.</li>
 *   <li>{@code findByPaymentProviderAndProviderOrderRef} -- the gateway-side order code moved to
 *       PaymentRecord.</li>
 *   <li>{@code findByIdForUpdate} -- there is nothing left to "settle" on an invoice; the lock is now placed on Order
 *       ({@code OrderRepository.findByIdForUpdate}).</li>
 * </ul>
 */
public interface InvoiceRepository {
    Optional<Invoice> findById(UUID id);
    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrderId(UUID orderId);
    List<Invoice> findByOrderIdIn(Collection<UUID> orderIds);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /** Idempotency guard: one order issues exactly one invoice. */
    boolean existsByOrderId(UUID orderId);
}
