package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Chuyển {@code amountVnd} từ ví tự nạp của trường sang ví hạn mức của {@code quotaType}.
 *
 * <p>{@code quotaType} là String chứ không phải enum, cùng khuôn với
 * {@link SetQuotaDistributionPolicyCommand}: giá trị đến từ đường dẫn REST, và phân giải nó ở use case
 * cho ra câu báo lỗi tiếng Việt thay vì một 500 từ tầng binding.
 *
 * <p>{@code reason} tuỳ chọn -- ghi chú của quản trị viên (vd "theo đề nghị của lớp 12A"), lưu thẳng
 * vào bút toán để tra ngược.
 */
public record FundQuotaFromBalanceCommand(
    UUID schoolId,
    String quotaType,
    BigDecimal amountVnd,
    String reason
) {
}
