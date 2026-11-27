package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;

/**
 * Payment attempts of an Order. Settling the result is serialized with a lock on the ORDER
 * ({@link OrderRepository#findByIdForUpdate}) and not a lock here -- Order is the aggregate root
 * of a purchase, and both the webhook and the reconciliation job go through it.
 */
public interface PaymentRecordRepository {
    Optional<PaymentRecord> findById(UUID id);
    PaymentRecord save(PaymentRecord paymentRecord);

    List<PaymentRecord> findByOrderId(UUID orderId);

    /**
     * Attempts of MULTIPLE orders at once, newest first. Made for the GraphQL batch loader: a
     * page of 20 orders queried one by one is 20 queries for one history screen.
     */
    List<PaymentRecord> findByOrderIdIn(Collection<UUID> orderIds);

    /**
     * The WEBHOOK lookup path: both gateways only send back their own order code
     * (PayOS {@code orderCode}, SePay {@code order_invoice_number}). Must look up by the PAIR
     * (provider, ref) and not ref alone -- a code is only unique within one gateway, two different gateways
     * may well generate the same string.
     */
    Optional<PaymentRecord> findByProviderAndProviderOrderRef(PaymentProvider provider, String providerOrderRef);

    /**
     * The pending attempt of an order, if any. Returning {@code Optional} is fine because
     * uq_payment_records_one_pending_per_order guarantees at most one row.
     *
     * <p>Must be called BEFORE issuing a new link: if a PENDING attempt still exists, the gateway must be asked whether
     * it already produced money before deciding to resume or open a new attempt. Skipping that step opens the way
     * for a school to pay twice for the same order.
     */
    Optional<PaymentRecord> findPendingByOrderId(UUID orderId);

    /** Pending attempts across the whole system -- the input of the reconciliation job. */
    List<PaymentRecord> findByStatus(PaymentStatus status);

    /**
     * Number of attempts made for the order. Used by the adapter to generate a new order code that does not duplicate an old one -- see
     * {@code PaymentProcessPort.newOrderRef}.
     */
    long countByOrderId(UUID orderId);
}
