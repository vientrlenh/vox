package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * Mỗi trường đúng MỘT dòng số dư (uq_school_balances_school), nên mọi lần cộng/trừ đều tranh chấp
 * trên cùng một dòng.
 *
 * <p>CỐ Ý không có {@code tryDebit}/{@code credit} dạng "UPDATE ... WHERE" không khóa: mọi biến động
 * số dư đều phải kèm một SchoolBalanceEntry, mà entry có cột {@code balance_after_vnd NOT NULL}.
 * Một câu UPDATE cộng trừ tại chỗ không trả về số dư SAU khi trừ, còn SELECT lại sau đó thì có thể
 * đã thấy kết quả của một giao dịch khác chen vào -- hai bút toán sẽ ghi cùng một balanceAfter và
 * phá bất biến SUM(entries.amountVnd) = balanceVnd.
 *
 * <p>Vì vậy đường ghi DUY NHẤT là: {@link #findBySchoolIdForUpdate} (khóa dòng) -> tính số dư mới
 * trong Java -> {@link #save} + ghi entry, tất cả trong cùng transaction. Khóa chỉ giữ trên một dòng
 * của một trường trong vài câu lệnh, không phải điểm nghẽn ở quy mô này.
 */
public interface SchoolBalanceRepository {
    Optional<SchoolBalance> findById(UUID id);

    /** Chỉ đọc (vd guard kiểm tra đủ tiền). Muốn GHI thì phải qua {@link #findBySchoolIdForUpdate}. */
    Optional<SchoolBalance> findBySchoolId(UUID schoolId);

    /**
     * Khóa dòng số dư (SELECT ... FOR UPDATE) tới hết transaction. Bắt buộc dùng trước mọi thay đổi
     * số dư -- xem javadoc của interface.
     */
    Optional<SchoolBalance> findBySchoolIdForUpdate(UUID schoolId);

    SchoolBalance save(SchoolBalance balance);
}
