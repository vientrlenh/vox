package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.school.SchoolBalanceEntryType;

/**
 * Sổ cái append-only của SchoolBalance -- CỐ Ý không có update/delete: sửa một bút toán đã ghi là sai
 * nghiệp vụ, muốn điều chỉnh thì ghi thêm dòng ADJUSTMENT.
 *
 * <p>Mọi khoảng thời gian ở đây là NỬA MỞ {@code [from, to)} -- xem javadoc từng method.
 */
public interface SchoolBalanceEntryRepository {
    Optional<SchoolBalanceEntry> findById(UUID id);
    SchoolBalanceEntry save(SchoolBalanceEntry entry);

    /**
     * Sao kê của trường, mới nhất trước (khớp idx_school_balance_entries_school_occurred).
     * {@code page} đếm TỪ 1 theo quy ước chung của dự án.
     *
     * @param entryType null = không lọc theo loại bút toán
     * @param from bao gồm, {@code to} KHÔNG bao gồm. Cả hai BẮT BUỘC khác null -- người gọi muốn
     *     "toàn bộ lịch sử" thì truyền {@link java.time.Instant#EPOCH} và thời điểm hiện tại. Cố ý
     *     không nhận null: một tham số Instant null trong JPQL buộc phải bọc CAST để Postgres suy
     *     được kiểu, và cái bẫy đó không đáng đánh đổi lấy một tham số tiện tay.
     */
    PageResult<SchoolBalanceEntry> findBySchoolId(
        UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to, int page, int size);

    /** @param from bao gồm, {@code to} KHÔNG bao gồm -- hai kỳ liền nhau không đếm trùng mốc giao. */
    List<SchoolBalanceEntry> findBySchoolIdInRange(UUID schoolId, Instant from, Instant to);

    /**
     * Chốt chặn idempotent cho đường cộng tiền, soi đúng cặp cột của
     * uq_school_balance_entries_order (order_id, entry_type): webhook cổng và PendingOrderReconciler
     * có thể cùng chốt một đơn, không có guard này thì tiền vào số dư hai lần.
     */
    boolean existsByOrderIdAndEntryType(UUID orderId, SchoolBalanceEntryType entryType);

    /** Trả 0 (không phải null) khi không có bút toán nào. {@code from} bao gồm, {@code to} không. */
    BigDecimal sumAmountBySchoolIdAndEntryTypeInRange(
        UUID schoolId, SchoolBalanceEntryType entryType, Instant from, Instant to);
}
