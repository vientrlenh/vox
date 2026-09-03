package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionQuotaRecordJpaEntity;

public interface SpringDataSchoolSubscriptionQuotaRecordRepository extends JpaRepository<SchoolSubscriptionQuotaRecordJpaEntity, UUID> {
    List<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionId(UUID schoolSubscriptionId);
    Optional<SchoolSubscriptionQuotaRecordJpaEntity> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, String quotaType);

    // clearAutomatically=true vì lý do y hệt addUsage bên dưới, và nó áp cho CẢ HAI kết quả của câu
    // này: ConsumeQuotaService đọc lại chính dòng vừa đụng tới trong cùng transaction -- lọt qua thì
    // buildResponse đọc để trả về ví SAU khi trừ, hỏng thì chargeOverage đọc lại để tính phần hạn mức
    // còn lại. Thiếu cờ, cả hai lần đọc đó trúng entity CACHE từ lần load đầu method: nhánh lọt qua
    // trả về used CŨ (fundsExhausted vì thế trượt đúng lượt tiêu vừa vặn hết hạn mức), còn nhánh hỏng
    // thì lần đọc lại thành vô nghĩa vì không bao giờ thấy được gì mới.
    //
    // flushAutomatically=true đi kèm theo đúng quy tắc chung của repo này: hễ có clearAutomatically
    // thì phải có flush, vì em.clear() vứt mọi thay đổi CHƯA đẩy xuống DB chứ không riêng của bảng
    // này. Xem SpringDataSchoolSubscriptionQuotaUserAllocationRepository.addUsage để biết ca đã suýt
    // mất tiền vì thiếu đúng cặp cờ này.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.usedAmountVnd = q.usedAmountVnd + :amount
        WHERE q.id = :id AND q.usedAmountVnd + :amount <= q.totalAllocatedAmountVnd
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    // clearAutomatically=true BẮT BUỘC: nơi gọi luôn đọc lại entity này (before/after) trong CÙNG
    // transaction để so sánh trạng thái khóa/nợ (SchoolLockedDueToDebt/SchoolDebtCapExceeded/
    // SchoolDebtCleared) -- thiếu cờ này, Hibernate trả về entity CACHE cũ từ lần load trước thay vì
    // đọc lại DB sau bulk update, khiến so sánh trước/sau luôn sai (dù DB đã đúng).
    //
    // flushAutomatically=true: cùng quy tắc cặp cờ như tryConsume ở trên.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.totalAllocatedAmountVnd = q.totalAllocatedAmountVnd + :amount WHERE q.id = :id")
    void addAllocation(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchoolSubscriptionQuotaRecordJpaEntity q SET q.usedAmountVnd = q.usedAmountVnd + :amount WHERE q.id = :id")
    void addUsage(@Param("id") UUID id, @Param("amount") BigDecimal amount);

    /**
     * Nạp tiền tự nạp vào ví hạn mức: cộng vào total VÀ ghi nhận phần đến từ ví, trong CÙNG MỘT câu
     * lệnh.
     *
     * <p>KHÔNG tách thành addAllocation() rồi cộng funded riêng: hai câu lệnh là hai cơ hội để một cái
     * chạy còn cái kia không, và hậu quả không đối xứng. Cộng total mà quên funded thì tiền trường bỏ
     * ra biến mất vào ngày gia hạn (đúng lỗi mà V12 sinh ra để chặn); cộng funded mà quên total thì vi
     * phạm thẳng chk_..._funded_within_total. Một câu lệnh thì không có trạng thái ở giữa.
     *
     * <p>Cặp cờ clearAutomatically/flushAutomatically theo đúng quy tắc chung của repo này: use case
     * đọc lại chính dòng vừa cộng trong cùng transaction để trả về ví SAU khi nạp, và bút toán trừ ví
     * đang nằm chờ trong persistence context lúc câu này chạy.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SchoolSubscriptionQuotaRecordJpaEntity q
        SET q.totalAllocatedAmountVnd = q.totalAllocatedAmountVnd + :amount,
            q.fundedFromBalanceVnd = q.fundedFromBalanceVnd + :amount
        WHERE q.id = :id
        """)
    void addFundingFromBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
