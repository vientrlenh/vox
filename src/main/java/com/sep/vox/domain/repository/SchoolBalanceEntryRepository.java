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

    /** Sao kê của trường, mới nhất trước (khớp idx_school_balance_entries_school_occurred). */
    PageResult<SchoolBalanceEntry> findBySchoolId(UUID schoolId, int page, int size);

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
