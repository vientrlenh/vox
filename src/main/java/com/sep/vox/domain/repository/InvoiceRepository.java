package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.invoice.Invoice;

/**
 * Hóa đơn giờ là CHỨNG TỪ phát hành sau khi tiền đã về, không còn mang vòng đời thanh toán (đã sang
 * Order) lẫn phiên cổng (đã sang PaymentRecord).
 *
 * <p>Vì vậy các method cũ đã BỎ, không phải quên:
 * <ul>
 *   <li>{@code findAllByStatus} / {@code sumAmountByStatus} -- invoice không còn cột status. Đơn
 *       PENDING cần đối soát hỏi {@code OrderRepository.findAllByStatus}, doanh thu hỏi
 *       {@code OrderRepository.sumTotalAmountByStatusInRange}.</li>
 *   <li>{@code findByPaymentProviderAndProviderOrderRef} -- mã đơn phía cổng đã chuyển sang
 *       PaymentRecord.</li>
 *   <li>{@code findByIdForUpdate} -- không còn gì để "chốt" trên hóa đơn; khóa nay đặt ở Order
 *       ({@code OrderRepository.findByIdForUpdate}).</li>
 * </ul>
 */
public interface InvoiceRepository {
    Optional<Invoice> findById(UUID id);
    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrderId(UUID orderId);
    List<Invoice> findByOrderIdIn(Collection<UUID> orderIds);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /** Guard idempotent: một đơn chỉ phát hành đúng một hóa đơn. */
    boolean existsByOrderId(UUID orderId);
}
