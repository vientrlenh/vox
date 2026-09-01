package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Một người và số tiền AI họ đã tiêu của trường trong cửa sổ.
 *
 * <p>{@code allocatedAmountVnd} là TRẦN CHI cá nhân nhà trường tự chia, không phải một ví: nó không
 * giữ tiền và vượt trần không sinh bút toán nào. null nghĩa là người này không bị chia trần — vẫn
 * tiêu được, chỉ là không có mức nào để so.
 */
public record UserAiSpendDto(
    UUID userId,
    String fullName,
    String quotaType,
    BigDecimal spentVnd,
    BigDecimal allocatedAmountVnd
) {
}
