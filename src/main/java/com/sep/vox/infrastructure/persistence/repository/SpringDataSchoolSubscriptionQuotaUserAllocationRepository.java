package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaUserAllocationJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaUserAllocationRepository extends JpaRepository<SchoolSubscriptionQuotaUserAllocationJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocationJpaEntity> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, String quotaType, UUID userId);

    @Modifying
    @Query("""
        UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a
        SET a.usedAmountVnd = a.usedAmountVnd + :amount
        WHERE a.id = :id AND a.usedAmountVnd + :amount <= a.allocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    /**
     * Tổng phần hạn mức đã hứa cho từng người mà họ CHƯA tiêu.
     *
     * <p>Kẹp âm bằng WHERE chứ không bằng CASE: dòng đã tiêu quá trần (addUsage là UPDATE vô điều
     * kiện nên chuyện đó xảy ra được) bị loại khỏi tập cộng thay vì đóng góp một số âm. Một giáo viên
     * lỡ vượt trần KHÔNG phải là khoản tín dụng trả ngược lại cho trường -- tiền đó đã tiêu thật rồi,
     * và cộng phần âm vào sẽ làm phần đã hứa cho những người khác trông nhỏ đi.
     *
     * <p>Cộng phần CHƯA TIÊU chứ không phải toàn bộ allocated, vì đây là con số sẽ đem trừ khỏi ví
     * trường: phần giáo viên đã tiêu thì ConsumeQuotaService đã trừ vào used_amount_vnd của ví rồi
     * (consumeUserAllocation cộng dồn CÙNG khoản tiền vào cả hai bộ đếm), nên trừ cả allocated là trừ
     * hai lần đúng những đồng đã tiêu.
     */
    // COALESCE về 0: SUM trên tập rỗng trả NULL -- trường chưa chia cho ai là ca THƯỜNG GẶP nhất.
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd - a.usedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
          AND a.allocatedAmountVnd > a.usedAmountVnd
        """)
    BigDecimal sumUnusedAllocation(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType);

    /**
     * Tổng đã chia cho những người CÒN đủ điều kiện nhận -- cùng phép lọc mà màn chia hạn mức dùng để
     * dựng danh sách ({@code SchoolUserRepository.findBySchoolId} với status ACTIVE + roleId).
     *
     * <p>Phải lọc chứ không cộng tất: dòng phân bổ KHÔNG bị xoá khi giáo viên nghỉ việc hay học sinh
     * ra trường, nên cộng tất là đếm cả phần đứng tên những người không còn xuất hiện ở bất kỳ trang
     * nào của bảng. Trường nhìn thấy "đã chia" lớn hơn tổng các dòng đang hiện, và tới lúc phần vô
     * hình đó chạm trần thì mọi lần chia tiếp đều bị từ chối mà không màn hình nào giải thích nổi.
     *
     * <p>Bỏ họ khỏi tổng KHÔNG tạo ra tiền từ hư không: phần họ đã TIÊU vẫn nằm nguyên ở
     * {@code used_amount_vnd} của ví trường (ConsumeQuotaService.consumeUserAllocation cộng dồn cùng
     * khoản vào cả hai bộ đếm). Thứ thôi chiếm chỗ trong trần chỉ là phần CHƯA ai tiêu được nữa.
     *
     * <p>Dòng của họ cũng KHÔNG bị xoá: nếu tài khoản được kích hoạt lại thì trần cũ trở lại nguyên
     * vẹn, và giữ lại thì sổ sách vẫn tra ngược được ai từng được cấp bao nhiêu.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        JOIN SchoolUserJpaEntity su ON su.userId = a.userId
        JOIN UserJpaEntity u ON u.id = a.userId
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
          AND su.schoolId = :schoolId
          AND u.status = :status
          AND EXISTS (
              SELECT 1 FROM UserRoleJpaEntity ur
              WHERE ur.userId = a.userId AND ur.roleId = :roleId)
        """)
    BigDecimal sumAllocatedForEligibleUsers(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType,
        @Param("schoolId") UUID schoolId,
        @Param("roleId") UUID roleId,
        @Param("status") String status);

    /**
     * Tổng đã chia trên MỌI dòng, kể cả người không còn đủ điều kiện.
     *
     * <p>Chỉ dùng để hiện phần chênh so với {@link #sumAllocatedForEligibleUsers} -- con số đó mới là
     * con số đem so với trần. Có mặt ở đây để phần "vô hình" được nói thành lời trên giao diện thay vì
     * bị lặng lẽ loại khỏi phép tính.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmountVnd), 0)
        FROM SchoolSubscriptionQuotaUserAllocationJpaEntity a
        WHERE a.schoolSubscriptionId = :schoolSubscriptionId
          AND a.quotaType = :quotaType
        """)
    BigDecimal sumAllocated(
        @Param("schoolSubscriptionId") UUID schoolSubscriptionId,
        @Param("quotaType") String quotaType);

    // clearAutomatically=true -- cùng lý do với SpringDataSchoolSubscriptionQuotaRecordRepository.addUsage:
    // tránh Hibernate trả về entity cache cũ nếu có chỗ nào sau này đọc lại trong cùng transaction.
    //
    // flushAutomatically=true BẮT BUỘC đi kèm, không phải cho gọn đôi: ConsumeQuotaService gọi
    // consumeUserAllocation SAU chargeOverage, mà chargeOverage vừa merge SchoolBalance đã bị trừ và
    // persist bút toán OVERAGE_CHARGE -- cả hai còn nằm chờ trong persistence context. Hibernate chỉ
    // tự flush cho những query space mà câu UPDATE này chạm tới (bảng allocation), KHÔNG gồm
    // school_balances / school_balance_entries, nên em.clear() sau đó vứt thẳng khoản ghi nợ: chi phí
    // AI biến mất khỏi ví trường mà không lỗi nào nổi lên, và bất biến SUM(entries) = balance_vnd vỡ.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaUserAllocationJpaEntity a SET a.usedAmountVnd = a.usedAmountVnd + :amount WHERE a.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
