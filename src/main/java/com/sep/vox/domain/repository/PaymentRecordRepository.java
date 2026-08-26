package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.domain.model.payment.PaymentRecord;
import com.sep.vox.domain.model.payment.PaymentStatus;

/**
 * Các lần thử thanh toán của Order. Việc chốt kết quả được tuần tự hóa bằng khóa trên ĐƠN
 * ({@link OrderRepository#findByIdForUpdate}) chứ không phải khóa ở đây -- Order mới là gốc tổng
 * hợp của một lượt mua, và cả webhook lẫn job đối soát đều đi qua nó.
 */
public interface PaymentRecordRepository {
    Optional<PaymentRecord> findById(UUID id);
    PaymentRecord save(PaymentRecord paymentRecord);

    List<PaymentRecord> findByOrderId(UUID orderId);

    /**
     * Các lần thử của NHIỀU đơn cùng lúc, mới nhất trước. Sinh ra cho batch loader của GraphQL: một
     * trang 20 đơn mà hỏi lần lượt từng đơn là 20 lượt truy vấn cho một màn hình lịch sử.
     */
    List<PaymentRecord> findByOrderIdIn(Collection<UUID> orderIds);

    /**
     * Đường tra của WEBHOOK: cả hai cổng đều chỉ gửi về mã đơn của chính họ
     * (PayOS {@code orderCode}, SePay {@code order_invoice_number}). Phải tra theo CẶP
     * (provider, ref) chứ không riêng ref -- mã chỉ duy nhất trong phạm vi một cổng, hai cổng khác
     * nhau hoàn toàn có thể sinh ra cùng một chuỗi.
     */
    Optional<PaymentRecord> findByProviderAndProviderOrderRef(PaymentProvider provider, String providerOrderRef);

    /**
     * Lần thử đang treo của đơn, nếu có. Trả {@code Optional} được vì
     * uq_payment_records_one_pending_per_order đảm bảo tối đa một dòng.
     *
     * <p>Phải gọi cái này TRƯỚC khi phát link mới: nếu còn một lần thử PENDING thì phải hỏi cổng xem
     * nó đã ra tiền chưa rồi mới quyết định resume hay mở lần thử mới. Bỏ qua bước đó là mở đường
     * cho trường trả tiền hai lần cho cùng một đơn.
     */
    Optional<PaymentRecord> findPendingByOrderId(UUID orderId);

    /** Các lần thử đang treo trên toàn hệ thống -- đầu vào của job đối soát. */
    List<PaymentRecord> findByStatus(PaymentStatus status);

    /**
     * Số lần đã thử của đơn. Adapter dùng để sinh mã đơn mới không trùng mã cũ -- xem
     * {@code PaymentProcessPort.newOrderRef}.
     */
    long countByOrderId(UUID orderId);
}
