package com.sep.vox.application.query.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Chỉ giữ những câu hỏi mà {@code SubscriptionPlanRepository} không trả lời được vì phải đọc CHÉO
 * tổng hợp khác (school_subscriptions). Việc liệt kê/phân trang gói là đọc thuần một tổng hợp nên
 * thuộc về repository của domain, không nhân đôi ở đây.
 */
public interface SubscriptionPlanQueryRepository {

    /**
     * Gói ACTIVE đang có nhiều trường dùng nhất -- nguồn duy nhất của cờ "phổ biến nhất".
     *
     * <p>Tách khỏi truy vấn phân trang một cách CÓ CHỦ Ý: nếu tính max trong phạm vi trang thì trang 2
     * sẽ gắn nhãn cho gói cao nhất CỦA TRANG ĐÓ, không phải cao nhất toàn hệ thống -- mỗi trang lại
     * hiện một "gói phổ biến nhất" khác nhau. Tính toàn cục rồi so id là cách duy nhất khiến nhãn
     * không phụ thuộc vào việc người dùng đang đứng ở trang nào.
     *
     * <p>Trả {@code Optional.empty()} khi chưa trường nào đăng ký -- lúc đó không gắn nhãn cho gói
     * nào cả, thay vì trao nhãn cho một gói có 0 khách.
     */
    Optional<UUID> findMostPopularPlanId();
}
