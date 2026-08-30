package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SchoolBalanceEntryJpaEntity;

public interface SpringDataSchoolBalanceEntryRepository extends JpaRepository<SchoolBalanceEntryJpaEntity, UUID> {

    // Hai bản gần như trùng nhau, khác đúng một dòng entryType. Cố ý KHÔNG gộp thành một câu với
    // ":entryType IS NULL OR ..." -- tham số null trong JPQL phải bọc CAST để Postgres suy được kiểu,
    // và quên CAST thì lỗi chỉ nổ lúc chạy, đúng ở nhánh "không lọc" là nhánh thường dùng nhất.
    //
    // Khoá sắp xếp phụ theo id là BẮT BUỘC, không phải cho đẹp: một ca thi sinh nhiều bút toán trong
    // cùng một Instant, và chỉ ORDER BY occurred_at thì thứ tự giữa chúng không xác định -- với phân
    // trang, hậu quả là một dòng hiện hai lần ở hai trang còn một dòng khác biến mất. Id là uuidv7
    // nên tăng theo thời gian: vừa ổn định, vừa không phá thứ tự thời gian.
    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC, e.id DESC
        """)
    Page<SchoolBalanceEntryJpaEntity> findPageBySchoolIdInRange(
        @Param("schoolId") UUID schoolId,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable);

    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.entryType = :entryType
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC, e.id DESC
        """)
    Page<SchoolBalanceEntryJpaEntity> findPageBySchoolIdAndEntryTypeInRange(
        @Param("schoolId") UUID schoolId,
        @Param("entryType") String entryType,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable);

    // Khoảng NỬA MỞ [from, to) chứ không phải BETWEEN: BETWEEN inclusive hai đầu nên hai kỳ liền
    // nhau (tháng 1 -> 31/01, tháng 2 -> 01/02) vẫn đếm trùng bút toán rơi đúng mốc giao.
    @Query("""
        SELECT e FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.occurredAt >= :from AND e.occurredAt < :to
        ORDER BY e.occurredAt DESC
        """)
    List<SchoolBalanceEntryJpaEntity> findBySchoolIdInRange(
        @Param("schoolId") UUID schoolId,
        @Param("from") Instant from,
        @Param("to") Instant to);

    boolean existsByOrderIdAndEntryType(UUID orderId, String entryType);

    // COALESCE về 0: SUM trên tập rỗng trả NULL, để lọt ra ngoài thì mọi nơi gọi phải tự phòng null.
    @Query("""
        SELECT COALESCE(SUM(e.amountVnd), 0) FROM SchoolBalanceEntryJpaEntity e
        WHERE e.schoolId = :schoolId
          AND e.entryType = :entryType
          AND e.occurredAt >= :from AND e.occurredAt < :to
        """)
    BigDecimal sumAmountBySchoolIdAndEntryTypeInRange(
        @Param("schoolId") UUID schoolId,
        @Param("entryType") String entryType,
        @Param("from") Instant from,
        @Param("to") Instant to);
}
