package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.common.DecimalText;
import com.sep.vox.domain.model.school.SchoolBalance;

/**
 * Ví tiền tự nạp của trường, dạng đọc.
 *
 * <p>Không phơi {@code id} và {@code version}: không màn hình nào cần trỏ tới dòng số dư -- nó là
 * bản tổng hợp, mỗi trường đúng một dòng, không có gì để liên kết tới. {@code version} càng không,
 * nó là cơ chế khoá lạc quan của Hibernate; phơi ra là mời client tự nghĩ ra một luồng ghi mà
 * backend không hề có (đường ghi duy nhất đi qua findBySchoolIdForUpdateOrCreate).
 */
public record SchoolBalanceDto(
    UUID schoolId,
    String balanceVnd,
    boolean locked,
    String updatedAt
) {

    public static SchoolBalanceDto toDto(SchoolBalance balance) {
        return new SchoolBalanceDto(
            balance.getSchoolId(),
            DecimalText.of(balance.getBalanceVnd()),
            // Hỏi thẳng domain model chứ KHÔNG viết lại phép so với 0: isInDebt() đúng là hàm mà
            // SchoolSubscriptionDebtGuardService.isSchoolLocked gọi để chặn mở ca thi. Gõ lại luật ở
            // đây là dựng bản sao thứ hai, và bản sao đó sẽ không đổi theo nếu luật kia đổi.
            balance.isInDebt(),
            balance.getUpdatedAt() == null ? null : balance.getUpdatedAt().toString()
        );
    }

    /**
     * Trường chưa từng chạm vào ví. Dòng số dư được tạo lười (chỉ sinh lúc ghi lần đầu), và ví rỗng
     * với ví 0 đồng là CÙNG một nghĩa -- xem {@link SchoolBalance#emptyFor}. Trả null ra GraphQL chỉ
     * bắt client thêm một nhánh cho cùng một trạng thái, nên dựng sẵn bản 0 đồng ở đây.
     *
     * <p>KHÔNG dùng {@code SchoolBalance.emptyFor} rồi map: hàm đó dựng một aggregate để GHI, và
     * javadoc của nó cấm dùng cho đường đọc.
     */
    public static SchoolBalanceDto emptyFor(UUID schoolId) {
        return new SchoolBalanceDto(schoolId, DecimalText.of(BigDecimal.ZERO), false, null);
    }
}
