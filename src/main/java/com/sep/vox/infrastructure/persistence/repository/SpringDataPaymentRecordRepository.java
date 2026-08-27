package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.PaymentRecordJpaEntity;

public interface SpringDataPaymentRecordRepository extends JpaRepository<PaymentRecordJpaEntity, UUID> {
    List<PaymentRecordJpaEntity> findByOrderId(UUID orderId);

    /**
     * Sắp xếp ngay ở đây chứ không để tầng trên sort lại: batch loader chỉ gom nhóm theo order_id,
     * mà groupingBy giữ nguyên thứ tự của stream nên thứ tự đúng phải có sẵn từ câu truy vấn.
     *
     * <p>Xếp theo {@code id} chứ không theo {@code created_at}, và điều đó CHỈ ĐÚNG vì id sinh bằng
     * {@code uuidv7()} -- 48 bit đầu là mốc mili giây nên so sánh uuid ở Postgres cũng chính là so
     * sánh thời gian. Đổi lại được một khóa sắp xếp DUY NHẤT (khóa chính), tức thứ tự đã toàn phần
     * mà không cần thêm chốt phá hòa như {@code createdAt DESC, id DESC} ở
     * {@code SpringDataOrderRepository.findForAdmin}.
     *
     * <p>Ràng buộc giữ cho điều đó đúng nằm ở {@code PaymentRecordJpaEntity.id}:
     * {@code insertable = false} cộng {@code DEFAULT uuidv7()} nghĩa là ứng dụng KHÔNG BAO GIỜ tự
     * đặt id được. Ai đổi default đó, hoặc chèn thẳng bằng SQL với id tự chọn, thì thứ tự ở đây hỏng
     * âm thầm chứ không báo lỗi -- nên đổi thì phải quay lại {@code createdAt}.
     */
    List<PaymentRecordJpaEntity> findByOrderIdInOrderByIdDesc(Collection<UUID> orderIds);
    Optional<PaymentRecordJpaEntity> findByProviderAndProviderOrderRef(String provider, String providerOrderRef);
    Optional<PaymentRecordJpaEntity> findByOrderIdAndStatus(UUID orderId, String status);
    List<PaymentRecordJpaEntity> findByStatus(String status);
    long countByOrderId(UUID orderId);
}
