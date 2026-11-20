package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Chuyển {@code amountVnd} từ ví tự nạp của trường sang ví hạn mức của {@code quotaType}.
 *
 * <p>{@code reason} tuỳ chọn -- ghi chú của quản trị viên (vd "theo đề nghị của lớp 12A"), lưu thẳng
 * vào bút toán để tra ngược.
 */
public record FundQuotaFromBalanceCommand(
    UUID schoolId,
    QuotaType quotaType,
    BigDecimal amountVnd,
    String reason
) {
}
