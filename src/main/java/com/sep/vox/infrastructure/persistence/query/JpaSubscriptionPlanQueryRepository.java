package com.sep.vox.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.repository.SubscriptionPlanQueryRepository;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSubscriptionPlanQueryRepository implements SubscriptionPlanQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<UUID> findMostPopularPlanId() {
        // Đếm SỐ TRƯỜNG đang dùng, không đếm số lần thanh toán: gói được thay thế (replacedByPlanId)
        // sinh ra một id mới, nên lịch sử thanh toán vĩnh viễn nằm ở gói cũ đã ARCHIVED -- gói mới dù
        // ai cũng mua vẫn đếm ra 0. Còn đếm trường đang dùng thì khi gia hạn sang gói mới, con số tự
        // chuyển theo. Gia hạn cũng không bị đếm hai lần như khi đếm giao dịch.
        //
        // DISTINCT phòng trường hợp một trường lỡ có hai dòng ACTIVE trên cùng gói -- vẫn tính là một
        // khách. Chỉ tính s.status = ACTIVE: trường bị SUSPENDED đã mất quyền dùng nên không còn là
        // bằng chứng cho việc gói đó đang được ưa chuộng.
        //
        // ORDER BY phải VÉT CẠN tới mức duy nhất (số trường -> giá -> id): nếu chỉ sắp theo số trường
        // thì lúc mới chạy, các gói cùng có 1 trường sẽ hòa nhau và Postgres trả về thứ tự tùy ý --
        // nhãn "phổ biến nhất" nhảy sang gói khác sau mỗi lần tải trang. Hòa số trường thì ưu tiên gói
        // RẺ HƠN, rồi tới gói ra đời trước; id là uuidv7 nên "id ASC" chính là "cũ nhất trước" và vì
        // nó duy nhất nên thứ tự chốt hoàn toàn ở đây.
        //
        // JOIN (không phải LEFT JOIN) là cố ý: gói chưa có trường nào dùng bị loại thẳng, và khi cả
        // hệ thống chưa ai đăng ký thì không gói nào được gắn nhãn.
        return em.createQuery("""
            SELECT p.id
            FROM SubscriptionPlanJpaEntity p
                JOIN SchoolSubscriptionJpaEntity s ON s.subscriptionPlanId = p.id
            WHERE p.status = :planStatus AND s.status = :subscriptionStatus
            GROUP BY p.id, p.priceVnd
            ORDER BY COUNT(DISTINCT s.schoolId) DESC, p.priceVnd ASC, p.id ASC
        """, UUID.class)
            .setParameter("planStatus", SubscriptionPlanStatus.ACTIVE.name())
            .setParameter("subscriptionStatus", SchoolSubscriptionStatus.ACTIVE.name())
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst();
    }
}
